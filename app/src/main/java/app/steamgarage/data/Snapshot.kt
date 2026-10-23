package app.steamgarage.data

import app.steamgarage.data.db.CarEntity
import app.steamgarage.data.db.ItemEntity
import app.steamgarage.data.db.MileageEntity
import app.steamgarage.data.db.ServiceEntity
import app.steamgarage.domain.Category
import app.steamgarage.domain.ItemRule
import app.steamgarage.domain.ItemState
import app.steamgarage.domain.MileageForecast
import app.steamgarage.domain.Reading
import app.steamgarage.domain.ServicePoint
import app.steamgarage.domain.StatusCalculator
import app.steamgarage.domain.ThresholdSettings
import java.time.LocalDate

/** Все живые (не удалённые) записи базы разом. Данных мало, поэтому экраны считают всё в памяти. */
data class GarageSnapshot(
    val cars: List<CarEntity> = emptyList(),
    val items: List<ItemEntity> = emptyList(),
    val services: List<ServiceEntity> = emptyList(),
    val mileage: List<MileageEntity> = emptyList(),
)

/** Расходник с вычисленным статусом и историей замен (новые сверху). */
data class ItemSummary(
    val item: ItemEntity,
    val category: Category,
    val state: ItemState,
    val services: List<ServiceEntity>,
)

/** Машина с текущим пробегом и расходниками, отсортированными по срочности. */
data class CarSummary(
    val car: CarEntity,
    val currentKm: Int?,
    val averageDailyKm: Double?,
    /** День последнего показания пробега (номер дня от эпохи). */
    val lastReadingDay: Long?,
    val items: List<ItemSummary>,
) {
    /** Расходники, которые отслеживаются (не выключены). */
    val activeItems: List<ItemSummary> get() = items.filter { it.item.active }
}

/** Сначала отслеживаемые, затем по срочности статуса и износу. */
val ItemUrgencyOrder: Comparator<ItemSummary> =
    compareByDescending<ItemSummary> { it.item.active }
        .thenByDescending { it.state.status.urgency }
        .thenByDescending { it.state.wear ?: 0f }
        .thenBy { it.item.name.lowercase() }

fun ServiceEntity.toPoint() = ServicePoint(LocalDate.ofEpochDay(date), mileage)

fun GarageSnapshot.summarize(today: LocalDate, thresholds: ThresholdSettings): List<CarSummary> {
    val carIds = cars.map { it.id }.toSet()
    val itemsByCar = items.filter { it.carId in carIds }.groupBy { it.carId }
    val servicesByItem = services.groupBy { it.itemId }
    val readingsByCar = mileage.groupBy { it.carId }
    return cars.map { car ->
        val readings = readingsByCar[car.id].orEmpty()
        val points = readings.map { Reading(LocalDate.ofEpochDay(it.date), it.km) }
        val currentKm = MileageForecast.currentKm(points)
        val average = MileageForecast.averageDailyKm(points, today)
        val summaries = itemsByCar[car.id].orEmpty().map { item ->
            val history = servicesByItem[item.id].orEmpty()
                .sortedWith(compareByDescending<ServiceEntity> { it.date }.thenByDescending { it.mileage })
            val state = StatusCalculator.evaluate(
                rule = ItemRule(item.intervalKm, item.intervalMonths, item.warnKm, item.warnDays),
                services = history.map { it.toPoint() },
                currentKm = currentKm,
                averageDailyKm = average,
                today = today,
                thresholds = thresholds,
            )
            ItemSummary(item, Category.parse(item.category), state, history)
        }.sortedWith(ItemUrgencyOrder)
        CarSummary(
            car = car,
            currentKm = currentKm,
            averageDailyKm = average,
            lastReadingDay = readings.maxOfOrNull { it.date },
            items = summaries,
        )
    }
}
