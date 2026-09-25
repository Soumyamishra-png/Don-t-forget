package com.thingstoorder.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QcAppTest {
    @Test
    fun matchesOnlyWatchedApps() {
        assertEquals(QcApp.Blinkit, QcApp.matching("com.grofers.customerapp", setOf(QcApp.Blinkit, QcApp.Zepto)))
        assertNull(QcApp.matching("com.grofers.customerapp", setOf(QcApp.Zepto)))
        assertNull(QcApp.matching("com.example.other", setOf(QcApp.Blinkit, QcApp.Zepto)))
        assertNull(QcApp.matching(null, setOf(QcApp.Blinkit)))
    }

    @Test
    fun instamartIsDetectedInsideTheSwiggyApp() {
        assertEquals(QcApp.Instamart, QcApp.matching("in.swiggy.android", setOf(QcApp.Instamart)))
    }

    @Test
    fun comingSoonAppsHaveNoPackagesToWatch() {
        val comingSoon = QcApp.entries.filter { it.comingSoon }
        assertEquals(listOf(QcApp.AmazonFresh, QcApp.FlipkartMinutes), comingSoon)
        assertTrue(comingSoon.all { it.packages.isEmpty() })
    }

    @Test
    fun idsRoundTrip() {
        QcApp.entries.forEach { assertEquals(it, QcApp.fromId(it.id)) }
        assertNull(QcApp.fromId("unknown"))
    }
}
