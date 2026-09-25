package com.thingstoorder.app.reminders

import com.thingstoorder.app.data.ReminderMode
import com.thingstoorder.app.data.ReminderSetting
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderScheduleTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private fun at(day: Int, hour: Int, minute: Int = 0) = ZonedDateTime.of(2026, 9, day, hour, minute, 0, 0, zone)

    private val once = ReminderSetting(ReminderMode.Once, 4)
    private val twice = ReminderSetting(ReminderMode.Twice, 4)

    @Test
    fun offHasNoSlotsAndNoNextTime() {
        val off = ReminderSetting(null, 4)
        assertEquals(emptyList<LocalTime>(), ReminderSchedule.slots(off))
        assertNull(ReminderSchedule.next(off, at(25, 12)))
    }

    @Test
    fun onceADayFiresAtNineAndRollsToTomorrow() {
        assertEquals(at(25, 9), ReminderSchedule.next(once, at(25, 8)))
        assertEquals(at(26, 9), ReminderSchedule.next(once, at(25, 9)))
        assertEquals(at(26, 9), ReminderSchedule.next(once, at(25, 22, 30)))
    }

    @Test
    fun twiceADayUsesNineAndSix() {
        assertEquals(at(25, 9), ReminderSchedule.next(twice, at(25, 7)))
        assertEquals(at(25, 18), ReminderSchedule.next(twice, at(25, 10)))
        assertEquals(at(26, 9), ReminderSchedule.next(twice, at(25, 19)))
    }

    @Test
    fun customSlotsRunFromEightToTenPm() {
        fun hours(h: Int) = ReminderSchedule.slots(ReminderSetting(ReminderMode.Custom, h)).map { it.hour }
        assertEquals(listOf(8, 10, 12, 14, 16, 18, 20, 22), hours(2))
        assertEquals(listOf(8, 12, 16, 20), hours(4))
        assertEquals(listOf(8, 14, 20), hours(6))
        assertEquals(listOf(8, 16), hours(8))
    }

    @Test
    fun customPicksTheNextSlotAfterNow() {
        val every4 = ReminderSetting(ReminderMode.Custom, 4)
        assertEquals(at(25, 20), ReminderSchedule.next(every4, at(25, 18, 1)))
        assertEquals(at(26, 8), ReminderSchedule.next(every4, at(25, 21)))
    }

    @Test
    fun labelsAndTimesReadNaturally() {
        assertEquals("Off", ReminderSetting(null, 4).label)
        assertEquals("Twice a day", twice.label)
        assertEquals("9:00 am · 6:00 pm", twice.times)
        assertEquals("Every 6 hours", ReminderSetting(ReminderMode.Custom, 6).label)
    }
}
