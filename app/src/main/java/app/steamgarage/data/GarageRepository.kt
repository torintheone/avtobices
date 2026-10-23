package app.steamgarage.data

import androidx.room.withTransaction
import app.steamgarage.data.db.AppDatabase
import app.steamgarage.data.db.CarEntity
import app.steamgarage.data.db.ItemEntity
import app.steamgarage.data.db.MileageEntity
import app.steamgarage.data.db.MileageSource
import app.steamgarage.data.db.Place
import app.steamgarage.data.db.ServiceEntity
import app.steamgarage.data.db.TemplateEntity
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Поля машины, которые заполняет пользователь. */
data class CarDraft(
    val name: String,
    val make: String = "",
    val model: String = "",
    val year: Int? = null,
    val plate: String = "",
    val vin: String = "",
    val note: String = "",
    val color: Int = 0,
)

/** Поля расходника, которые заполняет пользователь. */
data class ItemDraft(
    val name: String,
    val category: String,
    val intervalKm: Int?,
    val intervalMonths: Int?,
    val warnKm: Int? = null,
    val warnDays: Int? = null,
    val defaultPart: String = "",
    val note: String = "",
    val active: Boolean = true,
)

/** Ответ на «Когда меняли в последний раз?». */
data class LastReplacement(val date: LocalDate, val km: Int)

/** Строка формы замены: расходник и его стоимость/артикул. */
data class ServiceLine(
    val itemId: String,
    val costKopecks: Long? = null,
    val part: String = "",
)

/** Строка пользовательского шаблона. */
@Serializable
data class TemplateItem(
    val name: String,
    val category: String,
    val intervalKm: Int? = null,
    val intervalMonths: Int? = null,
    val warnKm: Int? = null,
    val warnDays: Int? = null,
    val defaultPart: String = "",
)

/** Шаблон, сохранённый с другой машины. */
data class UserTemplate(val id: String, val name: String, val items: List<TemplateItem>)

/** Локальное изменение данных: на него реагируют синхронизация и проверка сроков. */
data class LocalChange(val garageId: String?)

/**
 * Все изменения данных идут через этот класс. Каждая запись получает отметку времени и устройства;
 * удаление — это «надгробие» (deleted = true), а не стирание строки; записи общих машин
 * помечаются dirty, чтобы синхронизация отправила их в облако.
 */
class GarageRepository(
    private val db: AppDatabase,
    private val settings: SettingsRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val cars = db.cars()
    private val items = db.items()
    private val services = db.services()
    private val mileage = db.mileage()
    private val templates = db.templates()
    private val json = Json { ignoreUnknownKeys = true }

    private val _changes = MutableSharedFlow<LocalChange>(extraBufferCapacity = 32)

    /** Поток локальных изменений. */
    val changes: SharedFlow<LocalChange> = _changes

    val snapshot: Flow<GarageSnapshot> = combine(
        cars.observeAlive(),
        items.observeAlive(),
        services.observeAlive(),
        mileage.observeAlive(),
    ) { c, i, s, m -> GarageSnapshot(c, i, s, m) }

    val userTemplates: Flow<List<UserTemplate>> = templates.observeAll().map { list ->
        list.map { t ->
            val parsed = runCatching { json.decodeFromString<List<TemplateItem>>(t.itemsJson) }.getOrDefault(emptyList())
            UserTemplate(t.id, t.name, parsed)
        }
    }

    private data class Stamp(val time: Long, val device: String, val author: String)

    private suspend fun stamp(): Stamp {
        val s = settings.current()
        return Stamp(clock(), settings.deviceId(), s.deviceName)
    }

    private fun notifyChange(garageId: String?) {
        _changes.tryEmit(LocalChange(garageId))
    }

    // ── Машины ─────────────────────────────────────────────────────────────

    suspend fun createCar(draft: CarDraft): String {
        val s = stamp()
        val car = CarEntity(
            id = newId(),
            garageId = null,
            updatedAt = s.time,
            updatedBy = s.device,
            name = draft.name.trim(),
            make = draft.make.trim(),
            model = draft.model.trim(),
            year = draft.year,
            plate = draft.plate.trim(),
            vin = draft.vin.trim(),
            note = draft.note.trim(),
            color = draft.color,
        )
        cars.upsert(car)
        notifyChange(null)
        return car.id
    }

    suspend fun updateCar(carId: String, draft: CarDraft) {
        val old = cars.get(carId) ?: return
        val s = stamp()
        cars.upsert(
            old.copy(
                name = draft.name.trim(),
                make = draft.make.trim(),
                model = draft.model.trim(),
                year = draft.year,
                plate = draft.plate.trim(),
                vin = draft.vin.trim(),
                note = draft.note.trim(),
                color = draft.color,
                updatedAt = s.time,
                updatedBy = s.device,
                dirty = old.garageId != null,
            ),
        )
        notifyChange(old.garageId)
    }

    /** Удаляет машину вместе со всеми её записями (надгробия). */
    suspend fun deleteCar(carId: String) {
        val car = cars.get(carId) ?: return
        val s = stamp()
        val shared = car.garageId != null
        db.withTransaction {
            cars.upsert(car.copy(deleted = true, updatedAt = s.time, updatedBy = s.device, dirty = shared))
            items.upsertAll(
                items.byCar(carId).filter { !it.deleted }
                    .map { it.copy(deleted = true, updatedAt = s.time, updatedBy = s.device, dirty = it.garageId != null) },
            )
            services.upsertAll(
                services.byCar(carId).filter { !it.deleted }
                    .map { it.copy(deleted = true, updatedAt = s.time, updatedBy = s.device, dirty = it.garageId != null) },
            )
            mileage.upsertAll(
                mileage.byCar(carId).filter { !it.deleted }
                    .map { it.copy(deleted = true, updatedAt = s.time, updatedBy = s.device, dirty = it.garageId != null) },
            )
        }
        notifyChange(car.garageId)
    }

    // ── Расходники ─────────────────────────────────────────────────────────

    /**
     * Добавляет расходники машине. Для тех, у кого известна последняя замена, сразу создаётся
     * первая запись замены; её пробег попадает в показания, если он больше текущего.
     */
    suspend fun addItems(carId: String, entries: List<Pair<ItemDraft, LastReplacement?>>): List<String> {
        val car = cars.get(carId) ?: return emptyList()
        val s = stamp()
        val shared = car.garageId != null
        val created = mutableListOf<String>()
        db.withTransaction {
            val newServices = mutableListOf<ServiceEntity>()
            entries.forEach { (draft, last) ->
                val item = draft.toEntity(newId(), car, s)
                items.upsert(item)
                created += item.id
                if (last != null) {
                    newServices += ServiceEntity(
                        id = newId(),
                        garageId = car.garageId,
                        updatedAt = s.time,
                        updatedBy = s.device,
                        dirty = shared,
                        carId = car.id,
                        itemId = item.id,
                        date = last.date.toEpochDay(),
                        mileage = last.km,
                        part = draft.defaultPart,
                        authorName = s.author,
                    )
                }
            }
            services.upsertAll(newServices)
            newServices.maxByOrNull { it.mileage }?.let { addReadingFromService(car, it, s) }
        }
        notifyChange(car.garageId)
        return created
    }

    suspend fun updateItem(itemId: String, draft: ItemDraft) {
        val old = items.get(itemId) ?: return
        val s = stamp()
        items.upsert(
            old.copy(
                name = draft.name.trim(),
                category = draft.category,
                intervalKm = draft.intervalKm,
                intervalMonths = draft.intervalMonths,
                warnKm = draft.warnKm,
                warnDays = draft.warnDays,
                defaultPart = draft.defaultPart.trim(),
                note = draft.note.trim(),
                active = draft.active,
                updatedAt = s.time,
                updatedBy = s.device,
                dirty = old.garageId != null,
            ),
        )
        notifyChange(old.garageId)
    }

    /** Удаляет расходник и его историю замен. Показания пробега остаются: это факты об одометре. */
    suspend fun deleteItem(itemId: String) {
        val item = items.get(itemId) ?: return
        val s = stamp()
        db.withTransaction {
            items.upsert(item.copy(deleted = true, updatedAt = s.time, updatedBy = s.device, dirty = item.garageId != null))
            services.upsertAll(
                services.byItem(itemId).filter { !it.deleted }
                    .map { it.copy(deleted = true, updatedAt = s.time, updatedBy = s.device, dirty = it.garageId != null) },
            )
        }
        notifyChange(item.garageId)
    }

    // ── Замены ─────────────────────────────────────────────────────────────

    /**
     * Записывает замену одного или нескольких расходников («Провести ТО»).
     * Несколько записей получают общий batchId. Пробег, если он больше текущего, попадает в показания.
     */
    suspend fun recordService(
        carId: String,
        lines: List<ServiceLine>,
        date: LocalDate,
        km: Int,
        place: Place?,
        note: String,
    ): List<String> {
        if (lines.isEmpty()) return emptyList()
        val car = cars.get(carId) ?: return emptyList()
        val s = stamp()
        val batch = if (lines.size > 1) newId() else null
        val created = lines.map { line ->
            ServiceEntity(
                id = newId(),
                garageId = car.garageId,
                updatedAt = s.time,
                updatedBy = s.device,
                dirty = car.garageId != null,
                carId = car.id,
                itemId = line.itemId,
                date = date.toEpochDay(),
                mileage = km,
                costKopecks = line.costKopecks,
                part = line.part.trim(),
                place = place?.name.orEmpty(),
                note = note.trim(),
                batchId = batch,
                authorName = s.author,
            )
        }
        db.withTransaction {
            services.upsertAll(created)
            addReadingFromService(car, created.first(), s)
        }
        notifyChange(car.garageId)
        return created.map { it.id }
    }

    suspend fun updateService(
        serviceId: String,
        date: LocalDate,
        km: Int,
        costKopecks: Long?,
        part: String,
        place: Place?,
        note: String,
    ) {
        val old = services.get(serviceId) ?: return
        val car = cars.get(old.carId) ?: return
        val s = stamp()
        val updated = old.copy(
            date = date.toEpochDay(),
            mileage = km,
            costKopecks = costKopecks,
            part = part.trim(),
            place = place?.name.orEmpty(),
            note = note.trim(),
            updatedAt = s.time,
            updatedBy = s.device,
            dirty = old.garageId != null,
        )
        db.withTransaction {
            services.upsert(updated)
            val linked = mileage.byService(serviceId).filter { !it.deleted }
            if (linked.isNotEmpty()) {
                mileage.upsertAll(
                    linked.map {
                        it.copy(km = km, date = updated.date, updatedAt = s.time, updatedBy = s.device, dirty = it.garageId != null)
                    },
                )
            } else {
                addReadingFromService(car, updated, s)
            }
        }
        notifyChange(old.garageId)
    }

    /** Удаляет запись замены и показание пробега, взятое из неё. */
    suspend fun deleteService(serviceId: String) {
        val old = services.get(serviceId) ?: return
        val s = stamp()
        db.withTransaction {
            services.upsert(old.copy(deleted = true, updatedAt = s.time, updatedBy = s.device, dirty = old.garageId != null))
            mileage.upsertAll(
                mileage.byService(serviceId).filter { !it.deleted }
                    .map { it.copy(deleted = true, updatedAt = s.time, updatedBy = s.device, dirty = it.garageId != null) },
            )
        }
        notifyChange(old.garageId)
    }

    // ── Пробег ─────────────────────────────────────────────────────────────

    /** Текущий пробег машины — максимум по живым показаниям. */
    suspend fun currentKm(carId: String): Int? =
        mileage.byCar(carId).filter { !it.deleted }.maxOfOrNull { it.km }

    /**
     * Записывает показание одометра. [correctPast] — пользователь подтвердил, что прошлые показания
     * больше нового ошибочны: они удаляются (надгробия).
     */
    suspend fun addMileage(carId: String, km: Int, date: LocalDate, correctPast: Boolean = false) {
        val car = cars.get(carId) ?: return
        val s = stamp()
        val shared = car.garageId != null
        db.withTransaction {
            if (correctPast) {
                mileage.upsertAll(
                    mileage.byCar(carId).filter { !it.deleted && it.km > km }
                        .map { it.copy(deleted = true, updatedAt = s.time, updatedBy = s.device, dirty = it.garageId != null) },
                )
            }
            mileage.upsert(
                MileageEntity(
                    id = newId(),
                    garageId = car.garageId,
                    updatedAt = s.time,
                    updatedBy = s.device,
                    dirty = shared,
                    carId = carId,
                    date = date.toEpochDay(),
                    km = km,
                    source = MileageSource.MANUAL.name,
                    authorName = s.author,
                ),
            )
        }
        notifyChange(car.garageId)
    }

    private suspend fun addReadingFromService(car: CarEntity, service: ServiceEntity, s: Stamp) {
        val current = mileage.byCar(car.id).filter { !it.deleted }.maxOfOrNull { it.km }
        if (current != null && service.mileage <= current) return
        mileage.upsert(
            MileageEntity(
                id = newId(),
                garageId = car.garageId,
                updatedAt = s.time,
                updatedBy = s.device,
                dirty = car.garageId != null,
                carId = car.id,
                date = service.date,
                km = service.mileage,
                source = MileageSource.SERVICE.name,
                serviceId = service.id,
                authorName = s.author,
            ),
        )
    }

    // ── Шаблоны ────────────────────────────────────────────────────────────

    /** «Сохранить набор как шаблон»: отслеживаемые расходники машины с интервалами. */
    suspend fun saveTemplate(name: String, carId: String) {
        val list = items.byCar(carId).filter { !it.deleted && it.active }.map {
            TemplateItem(it.name, it.category, it.intervalKm, it.intervalMonths, it.warnKm, it.warnDays, it.defaultPart)
        }
        if (list.isEmpty()) return
        templates.upsert(
            TemplateEntity(
                id = newId(),
                name = name.trim(),
                createdAt = clock(),
                itemsJson = json.encodeToString(list),
            ),
        )
    }

    suspend fun deleteTemplate(id: String) = templates.delete(id)

    private fun ItemDraft.toEntity(id: String, car: CarEntity, s: Stamp) = ItemEntity(
        id = id,
        garageId = car.garageId,
        updatedAt = s.time,
        updatedBy = s.device,
        dirty = car.garageId != null,
        carId = car.id,
        name = name.trim(),
        category = category,
        intervalKm = intervalKm,
        intervalMonths = intervalMonths,
        warnKm = warnKm,
        warnDays = warnDays,
        defaultPart = defaultPart.trim(),
        note = note.trim(),
        active = active,
    )

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}
