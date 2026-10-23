package app.steamgarage.domain

/**
 * Что уже сообщено о расходнике (хранится локально на устройстве).
 * Дни — номера дней от эпохи (LocalDate.toEpochDay()).
 */
data class NotifyState(
    /** Статус, о котором уведомили последним: SOON, OVERDUE или null. */
    val lastStatus: ItemStatus? = null,
    val lastNotifiedDay: Long? = null,
    /** «Напомнить через неделю»: до этого дня молчим, в этот день напоминаем. */
    val remindOnDay: Long? = null,
)

/** Решение ежедневной проверки по одному расходнику. */
data class NotifyDecision(val notify: Boolean, val state: NotifyState)

/**
 * Правила уведомлений: только при переходе в «Скоро» и в «Пора менять»;
 * «Пора менять» повторяется раз в [Defaults.OVERDUE_REPEAT_DAYS] дней, пока замену не записали.
 */
object NotificationPolicy {

    fun decide(status: ItemStatus, state: NotifyState, today: Long): NotifyDecision {
        if (status == ItemStatus.OK || status == ItemStatus.NO_DATA) {
            // Заменили или поправили интервал — начинаем с чистого листа.
            return NotifyDecision(false, NotifyState())
        }
        state.remindOnDay?.let { remindOn ->
            return if (today < remindOn) {
                NotifyDecision(false, state)
            } else {
                NotifyDecision(true, NotifyState(status, today, null))
            }
        }
        return when (status) {
            ItemStatus.SOON -> when (state.lastStatus) {
                null -> NotifyDecision(true, NotifyState(ItemStatus.SOON, today))
                // Уже сообщали; если было «Пора менять» (интервал увеличили) — просто запоминаем «Скоро».
                else -> NotifyDecision(false, state.copy(lastStatus = ItemStatus.SOON))
            }
            else -> {
                // Пора менять: при переходе в этот статус и затем раз в неделю.
                val last = state.lastNotifiedDay
                val repeatDue = last == null || today - last >= Defaults.OVERDUE_REPEAT_DAYS
                if (state.lastStatus != ItemStatus.OVERDUE || repeatDue) {
                    NotifyDecision(true, NotifyState(ItemStatus.OVERDUE, today))
                } else {
                    NotifyDecision(false, state)
                }
            }
        }
    }

    /** «Напомнить через неделю». */
    fun snooze(state: NotifyState, today: Long): NotifyState =
        state.copy(remindOnDay = today + Defaults.SNOOZE_DAYS)

    /**
     * «Обновите пробег»: у машины есть интервалы по км, а показаний не было [reminderDays] дней.
     * Повторно — не раньше чем через столько же дней после прошлого напоминания.
     */
    fun shouldRemindMileage(
        hasKmIntervals: Boolean,
        lastReadingDay: Long?,
        lastReminderDay: Long?,
        today: Long,
        reminderDays: Int = Defaults.MILEAGE_REMINDER_DAYS,
    ): Boolean {
        if (!hasKmIntervals || reminderDays <= 0) return false
        if (lastReadingDay != null && today - lastReadingDay < reminderDays) return false
        if (lastReminderDay != null && today - lastReminderDay < reminderDays) return false
        return true
    }
}
