package app.steamgarage.data.backup

import app.steamgarage.data.db.CarEntity
import app.steamgarage.data.db.ItemEntity
import app.steamgarage.data.db.MileageEntity
import app.steamgarage.data.db.ServiceEntity
import app.steamgarage.data.db.TemplateEntity
import app.steamgarage.domain.Merge
import app.steamgarage.domain.Syncable
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Резервная копия: все записи телефона в одном JSON-файле, включая «надгробия» удалённых —
 * чтобы при восстановлении удаление не воскресло. Страховка, не зависящая от Firebase.
 */
@Serializable
data class BackupFile(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val exportedAt: Long = 0,
    val cars: List<CarEntity> = emptyList(),
    val items: List<ItemEntity> = emptyList(),
    val services: List<ServiceEntity> = emptyList(),
    val mileage: List<MileageEntity> = emptyList(),
    val templates: List<TemplateEntity> = emptyList(),
) {
    companion object {
        const val FORMAT = "steamgarage-backup"
        const val VERSION = 1
    }
}

/** Файл — не резервная копия приложения или копия из более новой версии. */
class BackupFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

object BackupCodec {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(file: BackupFile): String = json.encodeToString(BackupFile.serializer(), file)

    fun decode(text: String): BackupFile {
        val file = try {
            val element = json.parseToJsonElement(text)
            // Метку формата проверяем в самом файле: у поля в классе есть значение по умолчанию.
            val format = ((element as? JsonObject)?.get("format") as? JsonPrimitive)?.contentOrNull
            if (format != BackupFile.FORMAT) throw BackupFormatException("not a backup")
            json.decodeFromJsonElement(BackupFile.serializer(), element)
        } catch (e: SerializationException) {
            throw BackupFormatException("not a backup", e)
        } catch (e: IllegalArgumentException) {
            throw BackupFormatException("not a backup", e)
        }
        if (file.version > BackupFile.VERSION) throw BackupFormatException("newer version")
        return file
    }
}

/** Слияние копии с данными телефона — по тому же правилу, что и синхронизация. */
object BackupMerge {
    /** Записи из копии, которые нужно записать: новых на телефоне нет, или версия в копии свежее. */
    fun <T : Syncable> winners(local: Collection<T>, incoming: Collection<T>): List<T> {
        val byId = local.associateBy { it.id }
        return Merge.mergeAll(incoming).values.filter { candidate ->
            val existing = byId[candidate.id]
            existing == null || Merge.incomingWins(existing, candidate)
        }
    }

    /** Шаблоны не синхронизируются и не меняются после сохранения: добавляем только недостающие. */
    fun newTemplates(local: Collection<TemplateEntity>, incoming: Collection<TemplateEntity>): List<TemplateEntity> {
        val ids = local.mapTo(HashSet()) { it.id }
        return incoming.filter { it.id !in ids }.distinctBy { it.id }
    }
}
