package app.steamgarage.data.backup

import app.steamgarage.data.db.CarEntity
import app.steamgarage.data.db.ItemEntity
import app.steamgarage.data.db.MileageEntity
import app.steamgarage.data.db.ServiceEntity
import app.steamgarage.data.db.TemplateEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupTest {

    private val sample = BackupFile(
        exportedAt = 1_792_740_000_000,
        cars = listOf(
            CarEntity(id = "c1", garageId = null, updatedAt = 10, updatedBy = "a", name = "Веста", make = "Lada", year = 2019, color = 2),
            CarEntity(id = "c2", garageId = "K7QM2XRA5BWDHJNP0TV9", updatedAt = 11, updatedBy = "b", deleted = true, name = "Старая"),
        ),
        items = listOf(
            ItemEntity(id = "i1", garageId = null, updatedAt = 12, updatedBy = "a", carId = "c1", name = "Моторное масло",
                category = "OILS", intervalKm = 10_000, intervalMonths = 12, defaultPart = "5W-40"),
            ItemEntity(id = "i2", garageId = null, updatedAt = 13, updatedBy = "a", carId = "c1", name = "Ремень ГРМ",
                category = "BELTS", intervalKm = null, intervalMonths = null, active = false),
        ),
        services = listOf(
            ServiceEntity(id = "s1", garageId = null, updatedAt = 14, updatedBy = "a", carId = "c1", itemId = "i1",
                date = 20_000, mileage = 76_500, costKopecks = 450_000, place = "SERVICE", batchId = "b1", authorName = "Папа"),
        ),
        mileage = listOf(
            MileageEntity(id = "m1", garageId = null, updatedAt = 15, updatedBy = "a", carId = "c1", date = 20_384, km = 85_900, source = "MANUAL"),
        ),
        templates = listOf(TemplateEntity(id = "t1", name = "Веста", createdAt = 16, itemsJson = "[]")),
    )

    @Test
    fun exportThenImportGivesTheSameData() {
        // Критерий ТЗ: экспорт → импорт даёт те же данные.
        val restored = BackupCodec.decode(BackupCodec.encode(sample))
        assertEquals(sample, restored)
        // На пустой телефон копия ложится целиком.
        assertEquals(sample.cars, BackupMerge.winners(emptyList(), restored.cars))
        assertEquals(sample.items, BackupMerge.winners(emptyList(), restored.items))
        assertEquals(sample.services, BackupMerge.winners(emptyList(), restored.services))
        assertEquals(sample.mileage, BackupMerge.winners(emptyList(), restored.mileage))
        assertEquals(sample.templates, BackupMerge.newTemplates(emptyList(), restored.templates))
    }

    @Test
    fun repeatedImportChangesNothing() {
        val restored = BackupCodec.decode(BackupCodec.encode(sample))
        assertTrue(BackupMerge.winners(sample.cars, restored.cars).isEmpty())
        assertTrue(BackupMerge.newTemplates(sample.templates, restored.templates).isEmpty())
    }

    @Test
    fun newerPhoneVersionWinsAndTombstoneStaysDeleted() {
        val phone = listOf(
            sample.cars[0].copy(updatedAt = 99, name = "Веста после правки"),
            CarEntity(id = "c3", garageId = null, updatedAt = 50, updatedBy = "a", deleted = true, name = "Удалённая"),
        )
        val backup = listOf(
            sample.cars[0],
            CarEntity(id = "c3", garageId = null, updatedAt = 20, updatedBy = "a", name = "Удалённая"),
        )
        assertTrue(BackupMerge.winners(phone, backup).isEmpty())
    }

    @Test
    fun localOnlyFlagIsNotSaved() {
        val json = BackupCodec.encode(BackupFile(cars = listOf(sample.cars[0].copy(dirty = true))))
        assertTrue("dirty" !in json)
        assertEquals(false, BackupCodec.decode(json).cars.single().dirty)
    }

    @Test(expected = BackupFormatException::class)
    fun foreignFileIsRejected() {
        BackupCodec.decode("""{"name": "что-то другое"}""")
    }

    @Test(expected = BackupFormatException::class)
    fun garbageIsRejected() {
        BackupCodec.decode("это не JSON")
    }
}
