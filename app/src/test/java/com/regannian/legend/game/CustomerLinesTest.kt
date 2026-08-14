package com.regannian.legend.game

import com.regannian.legend.game.copy.CustomerLines
import com.regannian.legend.game.copy.SeatMood
import com.regannian.legend.game.model.CustomerStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomerLinesTest {

    @Test
    fun waiting_lines_follow_patience() {
        assertEquals(CustomerLines.HOPE, CustomerLines.waitingLine(90))
        assertEquals(CustomerLines.IMPATIENT, CustomerLines.waitingLine(50))
        assertEquals(CustomerLines.FED_UP, CustomerLines.waitingLine(10))
    }

    @Test
    fun serve_and_mood() {
        assertEquals(CustomerLines.HAPPY, CustomerLines.serveLine(true))
        assertEquals(CustomerLines.RESIGNED, CustomerLines.serveLine(false))
        assertEquals(SeatMood.Hopeful, CustomerLines.moodOf(CustomerStatus.WaitingServe, 80, null))
        assertEquals(SeatMood.Resigned, CustomerLines.moodOf(CustomerStatus.Served, 50, CustomerLines.RESIGNED))
        assertEquals(SeatMood.Angry, CustomerLines.moodOf(CustomerStatus.LeftAngry, 0, CustomerLines.FED_UP))
    }
}
