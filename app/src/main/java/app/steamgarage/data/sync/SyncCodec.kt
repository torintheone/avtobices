package app.steamgarage.data.sync

import app.steamgarage.data.db.CarEntity
import app.steamgarage.data.db.ItemEntity
import app.steamgarage.data.db.MileageEntity
import app.steamgarage.data.db.ServiceEntity

/**
 * Записи гаража ↔ словари Firebase Realtime Database: /garages/{код}/{cars|items|services|mileage}/{id}.
 * Явное перечисление полей вместо рефлексии: R8 не нужно ничего сохранять, а лишние поля из будущих версий
 * просто игнорируются. id и garageId в словарь не пишутся — их задаёт путь. dirty — только локальное.
 */
object SyncCodec {
    const val CARS = "cars"
    const val ITEMS = "items"
    const val SERVICES = "services"
    const val MILEAGE = "mileage"

    fun encode(e: CarEntity): Map<String, Any?> = base(e.updatedAt, e.updatedBy, e.deleted) + mapOf(
        "name" to e.name,
        "make" to e.make,
        "model" to e.model,
        "year" to e.year,
        "plate" to e.plate,
        "vin" to e.vin,
        "note" to e.note,
        "color" to e.color,
    )

    fun encode(e: ItemEntity): Map<String, Any?> = base(e.updatedAt, e.updatedBy, e.deleted) + mapOf(
        "carId" to e.carId,
        "name" to e.name,
        "category" to e.category,
        "intervalKm" to e.intervalKm,
        "intervalMonths" to e.intervalMonths,
        "warnKm" to e.warnKm,
        "warnDays" to e.warnDays,
        "defaultPart" to e.defaultPart,
        "note" to e.note,
        "active" to e.active,
    )

    fun encode(e: ServiceEntity): Map<String, Any?> = base(e.updatedAt, e.updatedBy, e.deleted) + mapOf(
        "carId" to e.carId,
        "itemId" to e.itemId,
        "date" to e.date,
        "mileage" to e.mileage,
        "costKopecks" to e.costKopecks,
        "part" to e.part,
        "place" to e.place,
        "note" to e.note,
        "batchId" to e.batchId,
        "authorName" to e.authorName,
    )

    fun encode(e: MileageEntity): Map<String, Any?> = base(e.updatedAt, e.updatedBy, e.deleted) + mapOf(
        "carId" to e.carId,
        "date" to e.date,
        "km" to e.km,
        "source" to e.source,
        "serviceId" to e.serviceId,
        "authorName" to e.authorName,
    )

    /** null — запись повреждена (нет обязательных полей): её пропускаем, а не роняем синхронизацию. */
    fun decodeCar(id: String, garageId: String, m: Map<*, *>): CarEntity? {
        val (at, by) = stamp(m) ?: return null
        return CarEntity(
            id = id, garageId = garageId, updatedAt = at, updatedBy = by, deleted = m.bool("deleted"),
            name = m.str("name"), make = m.str("make"), model = m.str("model"), year = m.int("year"),
            plate = m.str("plate"), vin = m.str("vin"), note = m.str("note"), color = m.int("color") ?: 0,
        )
    }

    fun decodeItem(id: String, garageId: String, m: Map<*, *>): ItemEntity? {
        val (at, by) = stamp(m) ?: return null
        val carId = m.str("carId").ifEmpty { return null }
        return ItemEntity(
            id = id, garageId = garageId, updatedAt = at, updatedBy = by, deleted = m.bool("deleted"),
            carId = carId, name = m.str("name"), category = m.str("category"),
            intervalKm = m.int("intervalKm"), intervalMonths = m.int("intervalMonths"),
            warnKm = m.int("warnKm"), warnDays = m.int("warnDays"),
            defaultPart = m.str("defaultPart"), note = m.str("note"), active = m.bool("active", default = true),
        )
    }

    fun decodeService(id: String, garageId: String, m: Map<*, *>): ServiceEntity? {
        val (at, by) = stamp(m) ?: return null
        val carId = m.str("carId").ifEmpty { return null }
        val itemId = m.str("itemId").ifEmpty { return null }
        return ServiceEntity(
            id = id, garageId = garageId, updatedAt = at, updatedBy = by, deleted = m.bool("deleted"),
            carId = carId, itemId = itemId, date = m.long("date") ?: return null, mileage = m.int("mileage") ?: 0,
            costKopecks = m.long("costKopecks"), part = m.str("part"), place = m.str("place"), note = m.str("note"),
            batchId = (m["batchId"] as? String)?.takeIf { it.isNotEmpty() }, authorName = m.str("authorName"),
        )
    }

    fun decodeMileage(id: String, garageId: String, m: Map<*, *>): MileageEntity? {
        val (at, by) = stamp(m) ?: return null
        val carId = m.str("carId").ifEmpty { return null }
        return MileageEntity(
            id = id, garageId = garageId, updatedAt = at, updatedBy = by, deleted = m.bool("deleted"),
            carId = carId, date = m.long("date") ?: return null, km = m.int("km") ?: return null,
            source = m.str("source"), serviceId = (m["serviceId"] as? String)?.takeIf { it.isNotEmpty() },
            authorName = m.str("authorName"),
        )
    }

    private fun base(updatedAt: Long, updatedBy: String, deleted: Boolean): Map<String, Any?> =
        mapOf("updatedAt" to updatedAt, "updatedBy" to updatedBy, "deleted" to deleted)

    private fun stamp(m: Map<*, *>): Pair<Long, String>? {
        val at = m.long("updatedAt") ?: return null
        val by = (m["updatedBy"] as? String) ?: return null
        return at to by
    }

    // Firebase отдаёт целые как Long, дробные как Double; null-поля в базе просто отсутствуют.
    private fun Map<*, *>.long(key: String): Long? = (this[key] as? Number)?.toLong()
    private fun Map<*, *>.int(key: String): Int? = (this[key] as? Number)?.toInt()
    private fun Map<*, *>.str(key: String): String = this[key] as? String ?: ""
    private fun Map<*, *>.bool(key: String, default: Boolean = false): Boolean = this[key] as? Boolean ?: default
}
