package com.thingstoorder.app.reminders

import com.thingstoorder.app.data.ReminderMode
import com.thingstoorder.app.data.ReminderSetting
import java.time.LocalTime
import java.time.ZonedDateTime

object ReminderSchedule {
    fun slots(setting: ReminderSetting): List<LocalTime> = when (setting.mode) {
        null -> emptyList()
        ReminderMode.Once -> listOf(LocalTime.of(9, 0))
        ReminderMode.Twice -> listOf(LocalTime.of(9, 0), LocalTime.of(18, 0))
        ReminderMode.Custom -> generateSequence(8) { it + setting.hours }.takeWhile { it <= 22 }.map { LocalTime.of(it, 0) }.toList()
    }

    fun next(setting: ReminderSetting, now: ZonedDateTime): ZonedDateTime? {
        val slots = slots(setting)
        if (slots.isEmpty()) return null
        val today = now.toLocalDate()
        slots.map { today.atTime(it).atZone(now.zone) }.firstOrNull { it.isAfter(now) }?.let { return it }
        return today.plusDays(1).atTime(slots.first()).atZone(now.zone)
    }
}
