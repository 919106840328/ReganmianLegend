package com.regannian.legend.game

import com.regannian.legend.game.tutorial.TutorialSteps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorialStepsTest {

    @Test
    fun five_onboarding_steps() {
        assertEquals(5, TutorialSteps.ALL.size)
        assertTrue(TutorialSteps.ALL.all { it.title.isNotBlank() && it.body.isNotBlank() })
    }
}
