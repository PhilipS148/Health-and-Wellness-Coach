package io.github.philips148.wellnesscoach

import io.github.philips148.wellnesscoach.util.GoalsValidator
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GoalsValidatorTest {

    @Test
    fun wholeNumberGoal_validNumber_isAccepted() {
        assertNull(GoalsValidator.wholeNumberGoalError("2500", "Daily calorie goal"))
    }

    @Test
    fun wholeNumberGoal_withSurroundingSpaces_isAccepted() {
        assertNull(GoalsValidator.wholeNumberGoalError("  64  ", "Daily water goal"))
    }

    @Test
    fun wholeNumberGoal_blank_isRejected() {
        assertNotNull(GoalsValidator.wholeNumberGoalError("", "Daily calorie goal"))
    }

    @Test
    fun wholeNumberGoal_letters_isRejected() {
        assertNotNull(GoalsValidator.wholeNumberGoalError("abc", "Daily calorie goal"))
    }

    @Test
    fun wholeNumberGoal_decimal_isRejected() {
        assertNotNull(GoalsValidator.wholeNumberGoalError("1.5", "Daily step goal"))
    }

    @Test
    fun wholeNumberGoal_zero_isRejected() {
        assertNotNull(GoalsValidator.wholeNumberGoalError("0", "Daily step goal"))
    }

    @Test
    fun wholeNumberGoal_negative_isRejected() {
        assertNotNull(GoalsValidator.wholeNumberGoalError("-100", "Daily calorie goal"))
    }

    @Test
    fun sleepGoal_decimal_isAccepted() {
        assertNull(GoalsValidator.sleepGoalError("7.5"))
    }

    @Test
    fun sleepGoal_exactly24_isAccepted() {
        assertNull(GoalsValidator.sleepGoalError("24"))
    }

    @Test
    fun sleepGoal_over24_isRejected() {
        assertNotNull(GoalsValidator.sleepGoalError("24.5"))
    }

    @Test
    fun sleepGoal_zero_isRejected() {
        assertNotNull(GoalsValidator.sleepGoalError("0"))
    }

    @Test
    fun sleepGoal_blank_isRejected() {
        assertNotNull(GoalsValidator.sleepGoalError(""))
    }

    @Test
    fun sleepGoal_notANumber_isRejected() {
        assertNotNull(GoalsValidator.sleepGoalError("NaN"))
    }
}