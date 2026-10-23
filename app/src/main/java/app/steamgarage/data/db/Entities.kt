package app.steamgarage.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import app.steamgarage.domain.Syncable

/*
 * Таблицы базы. У каждой синхронизируемой записи — поля из раздела «Данные» SPEC.md:
 * id (UUID, не меняется), garageId (null — личная запись), updatedAt, updatedBy (устройство),
 * deleted («надгробие» вместо стирания) и dirty (изменение ещё не отправлено, только локально).
 * Производные величины (текущий пробег, последняя замена, статус) не хранятся — вычисляются.
 */

/** Машина. Обязательно только название. */
@Entity(tableName = "cars")
data class CarEntity(
    @PrimaryKey override val id: String,
    val garageId: String?,
    override val updatedAt: Long,
    override val updatedBy: String,
    override val deleted: Boolean = false,
    val dirty: Boolean = false,
    val name: String,
    val make: String = "",
    val model: String = "",
    val year: Int? = null,
    val plate: String = "",
    val vin: String = "",
    val note: String = "",
    /** Номер цвета значка из палитры приложения. */
    val color: Int = 0,
) : Syncable

/** Расходник машины. Нужен хотя бы один интервал; пороги null — по глобальному правилу. */
@Entity(tableName = "items", indices = [Index("carId")])
data class ItemEntity(
    @PrimaryKey override val id: String,
    val garageId: String?,
    override val updatedAt: Long,
    override val updatedBy: String,
    override val deleted: Boolean = false,
    val dirty: Boolean = false,
    val carId: String,
    val name: String,
    /** Имя значения [app.steamgarage.domain.Category]. */
    val category: String,
    val intervalKm: Int? = null,
    val intervalMonths: Int? = null,
    val warnKm: Int? = null,
    val warnDays: Int? = null,
    val defaultPart: String = "",
    val note: String = "",
    val active: Boolean = true,
) : Syncable

/** Запись замены. Одно ТО на несколько расходников — несколько записей с общим batchId. */
@Entity(tableName = "services", indices = [Index("carId"), Index("itemId"), Index("batchId")])
data class ServiceEntity(
    @PrimaryKey override val id: String,
    val garageId: String?,
    override val updatedAt: Long,
    override val updatedBy: String,
    override val deleted: Boolean = false,
    val dirty: Boolean = false,
    val carId: String,
    val itemId: String,
    /** Дата замены: номер дня от эпохи (LocalDate.toEpochDay). */
    val date: Long,
    val mileage: Int,
    /** Стоимость в копейках; null — не указана. */
    val costKopecks: Long? = null,
    val part: String = "",
    /** [Place.name] или пусто — место не указано. */
    val place: String = "",
    val note: String = "",
    val batchId: String? = null,
    /** «Имя на этом устройстве» того, кто записал: «Заменил: Папа». */
    val authorName: String = "",
) : Syncable

/** Показание одометра. Текущий пробег — максимум по показаниям. */
@Entity(tableName = "mileage", indices = [Index("carId"), Index("serviceId")])
data class MileageEntity(
    @PrimaryKey override val id: String,
    val garageId: String?,
    override val updatedAt: Long,
    override val updatedBy: String,
    override val deleted: Boolean = false,
    val dirty: Boolean = false,
    val carId: String,
    /** Номер дня от эпохи. */
    val date: Long,
    val km: Int,
    /** [MileageSource.name]. */
    val source: String,
    /** Запись замены, из которой взято показание (её правка или удаление меняет и показание). */
    val serviceId: String? = null,
    val authorName: String = "",
) : Syncable

/** Гараж, в котором состоит устройство (в версии 1 — не больше одного). Только локально. */
@Entity(tableName = "garage")
data class GarageEntity(
    @PrimaryKey val code: String,
    val name: String,
    val joinedAt: Long,
)

/** Что уже сообщено уведомлением о расходнике. Только локально. */
@Entity(tableName = "notify_state")
data class NotifyStateEntity(
    @PrimaryKey val itemId: String,
    val lastStatus: String?,
    val lastNotifiedDay: Long?,
    val remindOnDay: Long?,
)

/** Когда напоминали обновить пробег машины. Только локально. */
@Entity(tableName = "car_notify_state")
data class CarNotifyStateEntity(
    @PrimaryKey val carId: String,
    val lastMileageReminderDay: Long?,
)

/** Набор расходников, сохранённый как шаблон («Сохранить набор как шаблон»). Только локально. */
@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    /** Строки шаблона в JSON (список [app.steamgarage.data.TemplateItem]). */
    val itemsJson: String,
)

/** Где делали замену. */
enum class Place { SERVICE, SELF }

/** Откуда взялось показание пробега. */
enum class MileageSource { MANUAL, SERVICE }
