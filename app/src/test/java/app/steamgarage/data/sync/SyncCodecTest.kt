package app.steamgarage.data.sync

import app.steamgarage.data.db.CarEntity
import app.steamgarage.data.db.ItemEntity
import app.steamgarage.data.db.MileageEntity
import app.steamgarage.data.db.ServiceEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SyncCodecTest {

    private val garage = "K7QM2XRA5BWDHJNP0TV9"

    /** Как запись вернётся из Firebase: целые — Long, null-полей нет, могут быть поля из новых версий. */
    private fun fromFirebase(map: Map<String, Any?>): Map<String, Any> =
        map.filterValues { it != null }.mapValues { (_, v) -> if (v is Int) v.toLong() else v!! } + ("futureField" to "x")

    @Test
    fun carSurvivesRoundTrip() {
        val car = CarEntity(
            id = "c1", garageId = garage, updatedAt = 1_700_000_000_123, updatedBy = "device-b", deleted = false,
            name = "Веста", make = "Lada", model = "Vesta", year = 2019, plate = "А123ВС 77", vin = "XTA", note = "зимняя", color = 4,
        )
        assertEquals(car, SyncCodec.decodeCar("c1", garage, fromFirebase(SyncCodec.encode(car))))
    }

    @Test
    fun itemWithoutIntervalsSurvivesRoundTrip() {
        val item = ItemEntity(
            id = "i1", garageId = garage, updatedAt = 5, updatedBy = "a", carId = "c1", name = "Ремень ГРМ",
            category = "BELTS", intervalKm = null, intervalMonths = 60, warnKm = null, warnDays = 30, active = false,
        )
        assertEquals(item, SyncCodec.decodeItem("i1", garage, fromFirebase(SyncCodec.encode(item))))
    }

    @Test
    fun serviceAndTombstoneSurviveRoundTrip() {
        val service = ServiceEntity(
            id = "s1", garageId = garage, updatedAt = 9, updatedBy = "a", deleted = true, carId = "c1", itemId = "i1",
            date = 20_384, mileage = 85_900, costKopecks = null, part = "Shell 5W-40", place = "SELF", batchId = null, authorName = "Папа",
        )
        assertEquals(service, SyncCodec.decodeService("s1", garage, fromFirebase(SyncCodec.encode(service))))
    }

    @Test
    fun mileageSurvivesRoundTrip() {
        val reading = MileageEntity(
            id = "m1", garageId = garage, updatedAt = 7, updatedBy = "b", carId = "c1", date = 20_000, km = 123_456,
            source = "SERVICE", serviceId = "s1", authorName = "Мама",
        )
        assertEquals(reading, SyncCodec.decodeMileage("m1", garage, fromFirebase(SyncCodec.encode(reading))))
    }

    @Test
    fun brokenRecordIsSkipped() {
        val noStamp = mapOf("name" to "Веста")
        assertNull(SyncCodec.decodeCar("c1", garage, noStamp))
        val itemWithoutCar = mapOf("updatedAt" to 1L, "updatedBy" to "a", "name" to "Масло")
        assertNull(SyncCodec.decodeItem("i1", garage, itemWithoutCar))
    }

    @Test
    fun localOnlyFieldsAreNotSent() {
        val car = CarEntity(id = "c1", garageId = garage, updatedAt = 1, updatedBy = "a", dirty = true, name = "Веста")
        val encoded = SyncCodec.encode(car)
        assertFalse("dirty" in encoded || "garageId" in encoded || "id" in encoded)
    }
}
