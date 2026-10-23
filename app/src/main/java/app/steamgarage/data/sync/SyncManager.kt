package app.steamgarage.data.sync

import android.content.Context
import androidx.room.withTransaction
import app.steamgarage.data.SettingsRepository
import app.steamgarage.data.db.AppDatabase
import app.steamgarage.data.db.GarageEntity
import app.steamgarage.domain.GarageCode
import app.steamgarage.domain.Merge
import app.steamgarage.domain.Syncable
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseException
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/** Состояние синхронизации для верхней панели и настроек. */
sealed interface SyncStatus {
    /** В сборке нет конфига Firebase — синхронизация недоступна. */
    data object NotConfigured : SyncStatus

    data object NoGarage : SyncStatus
    data object Idle : SyncStatus
    data object Syncing : SyncStatus
    data class Failed(val error: SyncError) : SyncStatus
}

enum class SyncError {
    /** Нет сети или сервер не ответил вовремя — изменения ждут в очереди. */
    OFFLINE,

    /** Отказ в доступе: устройство исключили из гаража или правила базы не те. */
    DENIED,

    /** В google-services.json нет адреса базы: конфиг скачан до создания Realtime Database. */
    NO_DATABASE,

    /** Анонимный вход не удался (выключен в консоли Firebase). */
    AUTH,

    /** Код гаража с опечаткой: не сошёлся проверочный символ. */
    BAD_CODE,

    OTHER,
}

class SyncException(val error: SyncError, cause: Throwable? = null) : Exception(error.name, cause)

data class GarageMember(val uid: String, val name: String, val joinedAt: Long)

/**
 * Синхронизация общих машин через Firebase Realtime Database (раздел «Синхронизация» SPEC.md).
 * Источник правды — Room: из облака записи сливаются по общему правилу, в облако уходят записи с dirty.
 * Встроенный офлайн-кэш Firebase выключен, чтобы не было двух конкурирующих копий.
 */
@OptIn(FlowPreview::class)
class SyncManager(
    private val context: Context,
    private val db: AppDatabase,
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
    /** Пришли чужие изменения — пора перепроверить сроки. */
    private val onRemoteChanges: () -> Unit,
) {
    private val cars = db.cars()
    private val items = db.items()
    private val services = db.services()
    private val mileage = db.mileage()
    private val garageDao = db.garage()

    /** Есть ли в сборке конфиг Firebase (google-services.json). */
    val configured: Boolean by lazy { runCatching { FirebaseApp.getApps(context).isNotEmpty() }.getOrDefault(false) }

    private val database: FirebaseDatabase? by lazy {
        if (!configured) {
            null
        } else {
            try {
                FirebaseDatabase.getInstance().also { it.setPersistenceEnabled(false) }
            } catch (_: Exception) {
                null
            }
        }
    }

    val garage: Flow<GarageEntity?> = garageDao.observe()

    /** Анонимный аккаунт этого телефона в Firebase — чтобы отметить себя в списке участников. */
    fun currentUid(): String? =
        if (configured) runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull() else null

    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status

    private val _members = MutableStateFlow<List<GarageMember>>(emptyList())
    val members: StateFlow<List<GarageMember>> = _members

    private val pushMutex = Mutex()
    private val remoteChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 64)
    private var listening: String? = null
    private val listeners = mutableListOf<Pair<DatabaseReference, Any>>()

    init {
        scope.launch {
            _status.value = initialStatus()
            remoteChanges.debounce(REMOTE_DEBOUNCE_MS).collect { onRemoteChanges() }
        }
    }

    private suspend fun initialStatus(): SyncStatus = when {
        !configured -> SyncStatus.NotConfigured
        garageDao.get() == null -> SyncStatus.NoGarage
        else -> SyncStatus.Idle
    }

    // ── Гараж ──────────────────────────────────────────────────────────────

    /** «Создать гараж»: новый код, устройство — первый участник. Возвращает код. */
    suspend fun createGarage(): String = guarded {
        val uid = signIn()
        val code = GarageCode.generate()
        writeMember(code, uid)
        garageDao.upsert(GarageEntity(code = code, name = "", joinedAt = System.currentTimeMillis()))
        _status.value = SyncStatus.Idle
        startRealtime(code)
        code
    }

    /** «Вступить в гараж»: записаться в участники и загрузить все общие машины. */
    suspend fun joinGarage(input: String): SyncResult = guarded {
        val code = GarageCode.normalize(input) ?: throw SyncException(SyncError.BAD_CODE)
        val uid = signIn()
        writeMember(code, uid)
        garageDao.upsert(GarageEntity(code = code, name = "", joinedAt = System.currentTimeMillis()))
        val received = pull(code)
        val sent = push(code)
        finish(received, sent)
        startRealtime(code)
        SyncResult(received, sent)
    }

    /** «Покинуть гараж»: [keepData] — общие машины остаются на телефоне личными, иначе удаляются. */
    suspend fun leaveGarage(keepData: Boolean) {
        val garage = garageDao.get() ?: return
        stopRealtime()
        // Без сети просто уходим локально: место в списке участников освободится при следующем входе.
        runCatching {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) withTimeout(TIMEOUT_MS) { garageRef(garage.code).child(MEMBERS).child(uid).removeValue().await() }
        }
        db.withTransaction {
            if (keepData) {
                cars.detachGarage(garage.code)
                items.detachGarage(garage.code)
                services.detachGarage(garage.code)
                mileage.detachGarage(garage.code)
            } else {
                cars.deleteForGarage(garage.code)
                items.deleteForGarage(garage.code)
                services.deleteForGarage(garage.code)
                mileage.deleteForGarage(garage.code)
            }
            garageDao.clear()
        }
        settings.clearHiddenSharedCars()
        _members.value = emptyList()
        _status.value = if (configured) SyncStatus.NoGarage else SyncStatus.NotConfigured
    }

    // ── Синхронизация ──────────────────────────────────────────────────────

    /** Полная сверка: прочитать весь гараж, слить, отправить своё. Рычаг в верхней панели и фоновая задача. */
    suspend fun syncNow(): SyncResult {
        val garage = garageDao.get() ?: throw SyncException(SyncError.OTHER)
        return guarded {
            signIn()
            val received = pull(garage.code)
            val sent = push(garage.code)
            finish(received, sent)
            SyncResult(received, sent)
        }
    }

    /** Отправить накопленные изменения (после правок и при появлении сети). Ошибки не шумят: повторим позже. */
    suspend fun pushPending() {
        val garage = garageDao.get() ?: return
        if (!configured) return
        try {
            signIn()
            push(garage.code)
            if (_status.value is SyncStatus.Failed) _status.value = SyncStatus.Idle
        } catch (e: Exception) {
            if (e.isRealCancellation()) throw e
            _status.value = SyncStatus.Failed(classify(e))
        }
    }

    /** Приложение на экране — слушаем гараж в реальном времени и отправляем накопленное. */
    fun onAppVisible() {
        scope.launch {
            val garage = garageDao.get() ?: return@launch
            if (!configured) return@launch
            try {
                signIn()
                startRealtime(garage.code)
            } catch (e: Exception) {
                if (e.isRealCancellation()) throw e
                _status.value = SyncStatus.Failed(classify(e))
                return@launch
            }
            pushPending()
        }
    }

    /** Приложение ушло в фон — слушатели снимаются; дальше раз в 6 часов работает фоновая задача. */
    fun onAppHidden() {
        scope.launch { stopRealtime() }
    }

    private suspend fun finish(received: Int, sent: Int) {
        settings.setLastSync(System.currentTimeMillis(), received, sent)
        _status.value = SyncStatus.Idle
        if (received > 0) remoteChanges.tryEmit(Unit)
    }

    private suspend fun <T> guarded(block: suspend () -> T): T {
        if (!configured) throw SyncException(SyncError.OTHER)
        if (database == null) {
            _status.value = SyncStatus.Failed(SyncError.NO_DATABASE)
            throw SyncException(SyncError.NO_DATABASE)
        }
        _status.value = SyncStatus.Syncing
        return try {
            block()
        } catch (e: Exception) {
            if (e.isRealCancellation()) {
                _status.value = SyncStatus.Idle
                throw e
            }
            val error = classify(e)
            _status.value = SyncStatus.Failed(error)
            throw if (e is SyncException) e else SyncException(error, e)
        }
    }

    private suspend fun signIn(): String {
        val auth = FirebaseAuth.getInstance()
        auth.currentUser?.let { return it.uid }
        val result = withTimeout(TIMEOUT_MS) { auth.signInAnonymously().await() }
        return result.user?.uid ?: throw SyncException(SyncError.AUTH)
    }

    private suspend fun writeMember(code: String, uid: String) {
        val member = mapOf("name" to settings.current().deviceName, "joinedAt" to System.currentTimeMillis())
        withTimeout(TIMEOUT_MS) { garageRef(code).child(MEMBERS).child(uid).setValue(member).await() }
    }

    private fun garageRef(code: String): DatabaseReference =
        (database ?: throw SyncException(SyncError.NO_DATABASE)).getReference(GARAGES).child(code)

    /** Отправка: все dirty-записи одним многопутевым updateChildren; флаг снимается, если запись не менялась. */
    private suspend fun push(code: String): Int = pushMutex.withLock {
        val dirtyCars = cars.dirty(code)
        val dirtyItems = items.dirty(code)
        val dirtyServices = services.dirty(code)
        val dirtyMileage = mileage.dirty(code)
        val total = dirtyCars.size + dirtyItems.size + dirtyServices.size + dirtyMileage.size
        if (total == 0) return@withLock 0
        val update = HashMap<String, Any>()
        dirtyCars.forEach { update["${SyncCodec.CARS}/${it.id}"] = SyncCodec.encode(it).withoutNulls() }
        dirtyItems.forEach { update["${SyncCodec.ITEMS}/${it.id}"] = SyncCodec.encode(it).withoutNulls() }
        dirtyServices.forEach { update["${SyncCodec.SERVICES}/${it.id}"] = SyncCodec.encode(it).withoutNulls() }
        dirtyMileage.forEach { update["${SyncCodec.MILEAGE}/${it.id}"] = SyncCodec.encode(it).withoutNulls() }
        withTimeout(TIMEOUT_MS) { garageRef(code).updateChildren(update).await() }
        dirtyCars.forEach { cars.markClean(it.id, it.updatedAt) }
        dirtyItems.forEach { items.markClean(it.id, it.updatedAt) }
        dirtyServices.forEach { services.markClean(it.id, it.updatedAt) }
        dirtyMileage.forEach { mileage.markClean(it.id, it.updatedAt) }
        total
    }

    /** Получение: разовое чтение всего гаража и слияние. Возвращает число записей, которые что-то изменили. */
    private suspend fun pull(code: String): Int {
        val snapshot = withTimeout(TIMEOUT_MS) { garageRef(code).get().await() }
        _members.value = parseMembers(snapshot.child(MEMBERS))
        var received = 0
        for (kind in KINDS) {
            for (child in snapshot.child(kind).children) {
                if (mergeRemote(kind, code, child)) received++
            }
        }
        return received
    }

    /** Слить одну пришедшую запись. Чтение и запись — в одной транзакции, чтобы не затереть свежую правку. */
    private suspend fun mergeRemote(kind: String, code: String, child: DataSnapshot): Boolean {
        val id = child.key ?: return false
        val map = child.value as? Map<*, *> ?: return false
        val hidden = settings.current().hiddenSharedCars
        return db.withTransaction {
            when (kind) {
                SyncCodec.CARS -> SyncCodec.decodeCar(id, code, map)
                    ?.takeIf { it.id !in hidden }
                    ?.let { merge(cars.get(it.id), it) { winner -> cars.upsert(winner.copy(dirty = false)) } }
                SyncCodec.ITEMS -> SyncCodec.decodeItem(id, code, map)
                    ?.takeIf { it.carId !in hidden }
                    ?.let { merge(items.get(it.id), it) { winner -> items.upsert(winner.copy(dirty = false)) } }
                SyncCodec.SERVICES -> SyncCodec.decodeService(id, code, map)
                    ?.takeIf { it.carId !in hidden }
                    ?.let { merge(services.get(it.id), it) { winner -> services.upsert(winner.copy(dirty = false)) } }
                SyncCodec.MILEAGE -> SyncCodec.decodeMileage(id, code, map)
                    ?.takeIf { it.carId !in hidden }
                    ?.let { merge(mileage.get(it.id), it) { winner -> mileage.upsert(winner.copy(dirty = false)) } }
                else -> null
            } ?: false
        }
    }

    private suspend fun <T : Syncable> merge(local: T?, incoming: T, save: suspend (T) -> Unit): Boolean {
        if (local != null && !Merge.incomingWins(local, incoming)) return false
        save(incoming)
        return true
    }

    // ── Реальное время ─────────────────────────────────────────────────────

    @Synchronized
    private fun startRealtime(code: String) {
        if (listening == code) return
        stopRealtime()
        val ref = garageRef(code)
        for (kind in KINDS) {
            val listener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) = onRemote(kind, code, snapshot)
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) = onRemote(kind, code, snapshot)
                override fun onChildRemoved(snapshot: DataSnapshot) = Unit
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) = Unit
                override fun onCancelled(error: DatabaseError) = onListenerCancelled(error)
            }
            ref.child(kind).addChildEventListener(listener)
            listeners += ref.child(kind) to listener
        }
        val membersListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _members.value = parseMembers(snapshot)
            }

            override fun onCancelled(error: DatabaseError) = onListenerCancelled(error)
        }
        ref.child(MEMBERS).addValueEventListener(membersListener)
        listeners += ref.child(MEMBERS) to membersListener
        // Сеть появилась — отправляем то, что накопилось без неё.
        val connectedRef = (database ?: return).getReference(".info/connected")
        val connectedListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.getValue(Boolean::class.java) == true) scope.launch { pushPending() }
            }

            override fun onCancelled(error: DatabaseError) = Unit
        }
        connectedRef.addValueEventListener(connectedListener)
        listeners += connectedRef to connectedListener
        listening = code
    }

    @Synchronized
    private fun stopRealtime() {
        for ((ref, listener) in listeners) {
            when (listener) {
                is ChildEventListener -> ref.removeEventListener(listener)
                is ValueEventListener -> ref.removeEventListener(listener)
            }
        }
        listeners.clear()
        listening = null
    }

    private fun onRemote(kind: String, code: String, snapshot: DataSnapshot) {
        scope.launch(Dispatchers.IO) {
            if (runCatching { mergeRemote(kind, code, snapshot) }.getOrDefault(false)) remoteChanges.tryEmit(Unit)
        }
    }

    private fun onListenerCancelled(error: DatabaseError) {
        _status.value = SyncStatus.Failed(
            if (error.code == DatabaseError.PERMISSION_DENIED) SyncError.DENIED else SyncError.OTHER,
        )
        stopRealtime()
    }

    private fun parseMembers(snapshot: DataSnapshot): List<GarageMember> = snapshot.children.mapNotNull { child ->
        val uid = child.key ?: return@mapNotNull null
        val map = child.value as? Map<*, *> ?: return@mapNotNull null
        GarageMember(uid, map["name"] as? String ?: "", (map["joinedAt"] as? Number)?.toLong() ?: 0)
    }.sortedBy { it.joinedAt }

    private fun classify(e: Throwable): SyncError = when {
        e is SyncException -> e.error
        e is TimeoutCancellationException || e is FirebaseNetworkException -> SyncError.OFFLINE
        e is FirebaseAuthException -> SyncError.AUTH
        e is DatabaseException && e.message.orEmpty().contains("permission", ignoreCase = true) -> SyncError.DENIED
        e.cause != null && e.cause !== e -> classify(e.cause!!)
        else -> SyncError.OTHER
    }

    /** Отмена корутины (ушли с экрана) — не ошибка синхронизации; истёкшее ожидание ответа — ошибка «нет сети». */
    private fun Exception.isRealCancellation(): Boolean = this is CancellationException && this !is TimeoutCancellationException

    private fun Map<String, Any?>.withoutNulls(): Map<String, Any> =
        entries.mapNotNull { (k, v) -> v?.let { k to it } }.toMap()

    private companion object {
        const val GARAGES = "garages"
        const val MEMBERS = "members"
        val KINDS = listOf(SyncCodec.CARS, SyncCodec.ITEMS, SyncCodec.SERVICES, SyncCodec.MILEAGE)

        /** Сколько ждать ответа сервера: без сети запросы Firebase висят бесконечно. */
        const val TIMEOUT_MS = 20_000L

        /** Пачка чужих правок — одна проверка сроков. */
        const val REMOTE_DEBOUNCE_MS = 2_000L
    }
}

/** Итог сверки: «Получено 3, отправлено 1». */
data class SyncResult(val received: Int, val sent: Int)
