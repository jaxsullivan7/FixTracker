package com.fixtracker.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FormatTest {

    private val today = LocalDate.of(2026, 11, 4)

    @Test
    fun `relative due labels`() {
        assertEquals("Ayer", dueDateLabel(today.minusDays(1), today))
        assertEquals("Hoy", dueDateLabel(today, today))
        assertEquals("Mañana", dueDateLabel(today.plusDays(1), today))
        assertEquals("En 5 días", dueDateLabel(today.plusDays(5), today))
    }

    @Test
    fun `kilometres use spanish grouping`() {
        assertEquals("96.420", formatKm(96_420))
    }

    @Test
    fun `consumption and litres are formatted`() {
        assertTrue(formatConsumption(5.56).startsWith("5,6"))
        assertTrue(formatLiters(42.0).startsWith("42,0"))
    }
}
