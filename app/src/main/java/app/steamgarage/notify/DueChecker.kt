package app.steamgarage.notify

import app.steamgarage.AppContainer
import app.steamgarage.data.db.CarNotifyStateEntity
import app.steamgarage.data.db.NotifyStateEntity
import app.steamgarage.data.summarize
import app.steamgarage.domain.ItemStatus
import app.steamgarage.domain.NotificationPolicy
import app.steamgarage.domain.NotifyState
import java.time.LocalDate
import kotlinx.coroutines.flow.first

/**
 * Проверка сроков: считает статусы, решает по правилам [NotificationPolicy], кому писать,
 * показывает уведомления и запоминает, о чём уже сообщено. Запускается ежедневно и после изменений.
 */
class DueChecker(private val container: AppContainer) {
    private val dao = container.database.notify()

    suspend fun run(today: LocalDate = LocalDate.now()) {
        val context = container.appContext
        // Без разрешения молчим и ничего не помечаем: уведомления придут, когда его дадут.
        if (!Notifier.canNotify(context)) return
        val settings = container.settings.current()
        val day = today.toEpochDay()
        val cars = container.repository.snapshot.first().summarize(today, settings.thresholds)
        val itemStates = dao.itemStates().associateBy { it.itemId }
        val carStates = dao.carStates().associateBy { it.carId }

        for (car in cars) {
            if (car.car.id in settings.mutedCars) continue
            val toNotify = car.activeItems.filter { summary ->
                val previous = itemStates[summary.item.id]?.toDomain() ?: NotifyState()
                val decision = NotificationPolicy.decide(summary.state.status, previous, day)
                if (decision.state != previous) dao.upsertItemState(decision.state.toEntity(summary.item.id))
                decision.notify
            }
            if (toNotify.isNotEmpty()) Notifier.showDue(context, car, toNotify)

            val hasKmIntervals = car.activeItems.any { it.item.intervalKm != null }
            // Нет ни одного показания — отсчитываем от последней замены.
            val reference = car.lastReadingDay ?: car.items.flatMap { it.services }.maxOfOrNull { it.date }
            val lastReminder = carStates[car.car.id]?.lastMileageReminderDay
            if (reference != null &&
                NotificationPolicy.shouldRemindMileage(hasKmIntervals, reference, lastReminder, day, settings.mileageReminderDays)
            ) {
                Notifier.showMileageReminder(context, car, settings.mileageReminderDays)
                dao.upsertCarState(CarNotifyStateEntity(car.car.id, day))
            }
        }
    }

    /** «Напомнить через неделю» для расходников из уведомления. */
    suspend fun snooze(itemIds: List<String>, today: LocalDate = LocalDate.now()) {
        val day = today.toEpochDay()
        itemIds.forEach { id ->
            val state = dao.itemState(id)?.toDomain() ?: NotifyState()
            dao.upsertItemState(NotificationPolicy.snooze(state, day).toEntity(id))
        }
    }
}

private fun NotifyStateEntity.toDomain() = NotifyState(
    lastStatus = lastStatus?.let { name -> ItemStatus.entries.firstOrNull { it.name == name } },
    lastNotifiedDay = lastNotifiedDay,
    remindOnDay = remindOnDay,
)

private fun NotifyState.toEntity(itemId: String) = NotifyStateEntity(
    itemId = itemId,
    lastStatus = lastStatus?.name,
    lastNotifiedDay = lastNotifiedDay,
    remindOnDay = remindOnDay,
)
