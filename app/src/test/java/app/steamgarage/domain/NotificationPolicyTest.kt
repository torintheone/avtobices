package app.steamgarage.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPolicyTest {

    private val day = 20_000L

    @Test
    fun soonNotifiesOnceAndRerunDoesNotRepeat() {
        // Критерий приёмки этапа 2: «Скоро» приходит при ближайшей проверке, а повторно — нет.
        val first = NotificationPolicy.decide(ItemStatus.SOON, NotifyState(), day)
        assertTrue(first.notify)
        val rerunSameDay = NotificationPolicy.decide(ItemStatus.SOON, first.state, day)
        assertFalse(rerunSameDay.notify)
        val nextDay = NotificationPolicy.decide(ItemStatus.SOON, rerunSameDay.state, day + 1)
        assertFalse(nextDay.notify)
        val muchLater = NotificationPolicy.decide(ItemStatus.SOON, nextDay.state, day + 30)
        assertFalse(muchLater.notify)
    }

    @Test
    fun overdueRepeatsEverySevenDays() {
        var state = NotifyState()
        val first = NotificationPolicy.decide(ItemStatus.OVERDUE, state, day)
        assertTrue(first.notify)
        state = first.state
        for (d in 1 until 7) {
            val r = NotificationPolicy.decide(ItemStatus.OVERDUE, state, day + d)
            assertFalse("день $d", r.notify)
            state = r.state
        }
        val week = NotificationPolicy.decide(ItemStatus.OVERDUE, state, day + 7)
        assertTrue(week.notify)
        val sameDayRerun = NotificationPolicy.decide(ItemStatus.OVERDUE, week.state, day + 7)
        assertFalse(sameDayRerun.notify)
    }

    @Test
    fun soonThenOverdueNotifiesAgain() {
        val soon = NotificationPolicy.decide(ItemStatus.SOON, NotifyState(), day)
        val overdue = NotificationPolicy.decide(ItemStatus.OVERDUE, soon.state, day + 2)
        assertTrue(overdue.notify)
        assertEquals(ItemStatus.OVERDUE, overdue.state.lastStatus)
    }

    @Test
    fun replacementResetsState() {
        val overdue = NotificationPolicy.decide(ItemStatus.OVERDUE, NotifyState(), day)
        val replaced = NotificationPolicy.decide(ItemStatus.OK, overdue.state, day + 1)
        assertFalse(replaced.notify)
        assertEquals(NotifyState(), replaced.state)
        val soonAgain = NotificationPolicy.decide(ItemStatus.SOON, replaced.state, day + 200)
        assertTrue(soonAgain.notify)
    }

    @Test
    fun snoozeSilencesForAWeekThenReminds() {
        val soon = NotificationPolicy.decide(ItemStatus.SOON, NotifyState(), day)
        val snoozed = NotificationPolicy.snooze(soon.state, day)
        for (d in 0 until 7) {
            val r = NotificationPolicy.decide(ItemStatus.OVERDUE, snoozed, day + d)
            assertFalse("день $d", r.notify)
        }
        val remind = NotificationPolicy.decide(ItemStatus.SOON, snoozed, day + 7)
        assertTrue(remind.notify)
        assertEquals(null, remind.state.remindOnDay)
    }

    @Test
    fun item300KmLeftWithThreshold500GivesSoonOnce() {
        // Сценарий проверки этапа 2 целиком: расчёт статуса и решение об уведомлении.
        val today = LocalDate.of(2026, 10, 23)
        val state = StatusCalculator.evaluate(
            rule = ItemRule(intervalKm = 10_000, intervalMonths = null, warnKm = 500),
            services = listOf(ServicePoint(today.minusMonths(6), 50_000)),
            currentKm = 59_700,
            averageDailyKm = null,
            today = today,
        )
        assertEquals(ItemStatus.SOON, state.status)
        assertEquals(300, state.kmLeft)
        val first = NotificationPolicy.decide(state.status, NotifyState(), today.toEpochDay())
        assertTrue(first.notify)
        val nextCheck = NotificationPolicy.decide(state.status, first.state, today.toEpochDay())
        assertFalse(nextCheck.notify)
    }

    @Test
    fun mileageReminderAfterFourteenDaysWithoutReadings() {
        assertFalse(NotificationPolicy.shouldRemindMileage(true, day - 13, null, day))
        assertTrue(NotificationPolicy.shouldRemindMileage(true, day - 14, null, day))
        assertTrue(NotificationPolicy.shouldRemindMileage(true, null, null, day))
        assertFalse(NotificationPolicy.shouldRemindMileage(false, day - 100, null, day))
        // Уже напоминали 3 дня назад — ждём.
        assertFalse(NotificationPolicy.shouldRemindMileage(true, day - 20, day - 3, day))
        assertTrue(NotificationPolicy.shouldRemindMileage(true, day - 30, day - 14, day))
    }
}
