package com.buywise.app.domain

import com.buywise.app.domain.model.FinanceProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceProfileTest {

    @Test
    fun `hourlyWage = salary div 21_75 div 8`() {
        val profile = FinanceProfile(monthlySalary = 17400.0)
        // 17400 / 21.75 = 800, / 8 = 100
        assertEquals(100.0, profile.hourlyWage, 1e-9)
    }

    @Test
    fun `hourlyWage is zero when salary missing`() {
        assertEquals(0.0, FinanceProfile().hourlyWage, 1e-9)
        assertEquals(0.0, FinanceProfile(monthlySalary = 0.0).hourlyWage, 1e-9)
    }

    @Test
    fun `dailySunkCost = fixedExpense div 30`() {
        val profile = FinanceProfile(monthlyFixedExpense = 4500.0)
        assertEquals(150.0, profile.dailySunkCost, 1e-9)
    }

    @Test
    fun `dailySunkCost is zero when expense missing`() {
        assertEquals(0.0, FinanceProfile().dailySunkCost, 1e-9)
    }

    @Test
    fun `isValid requires positive salary`() {
        assertFalse(FinanceProfile().isValid)
        assertFalse(FinanceProfile(monthlySalary = 0.0).isValid)
        assertTrue(FinanceProfile(monthlySalary = 1.0).isValid)
    }
}
