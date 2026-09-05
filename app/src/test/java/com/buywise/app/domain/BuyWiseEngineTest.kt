package com.buywise.app.domain

import com.buywise.app.domain.model.AssessmentInput
import com.buywise.app.domain.model.Decision
import com.buywise.app.domain.model.FinanceProfile
import com.buywise.app.domain.model.LimitedTimeInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuyWiseEngineTest {

    /** 时薪 100 元/小时的财务画像 */
    private val profile = FinanceProfile(monthlySalary = 17400.0, monthlyFixedExpense = 3000.0)

    // ---------- baseScore ----------

    @Test
    fun `baseScore uses 40-30-30 weights`() {
        assertEquals(100.0, BuyWiseEngine.baseScore(10f, 10f, 10f), 1e-9)
        assertEquals(50.0, BuyWiseEngine.baseScore(0f, 0f, 0f) + 50.0, 1e-9)
        // (10×0.4 + 5×0.3 + 0×0.3) × 10 = 55
        assertEquals(55.0, BuyWiseEngine.baseScore(10f, 5f, 0f), 1e-9)
    }

    // ---------- suggestF ----------

    @Test
    fun `suggestF anchors 2H 5H 20H`() {
        val h = 100.0
        assertEquals(10f, BuyWiseEngine.suggestF(200.0, h))   // ≤ 2H → 10 分
        assertEquals(5f, BuyWiseEngine.suggestF(500.0, h))    // = 5H → 5 分
        assertEquals(0f, BuyWiseEngine.suggestF(2000.0, h))   // ≥ 20H → 0 分
    }

    @Test
    fun `suggestF interpolates between anchors and clamps`() {
        val h = 100.0
        // 3.5H → 中点 → 7.5 分
        assertEquals(7.5f, BuyWiseEngine.suggestF(350.0, h), 0.01f)
        // 0 元价格 → 默认 5 分
        assertEquals(5f, BuyWiseEngine.suggestF(0.0, h))
        // 100H → 钳制在 0
        assertEquals(0f, BuyWiseEngine.suggestF(10000.0, h))
    }

    // ---------- assess：三档分支 ----------

    private fun input(r: Float, e: Float, f: Float, price: Double = 100.0) =
        AssessmentInput(itemName = "测试", price = price, r = r, e = e, f = f)

    @Test
    fun `score above 80 decides BUY`() {
        val result = BuyWiseEngine.assess(profile, input(9f, 9f, 9f))
        assertEquals(Decision.BUY, result.finalDecision)
        assertEquals(Decision.BUY, result.baseDecision)
        assertNull(result.refineDetail)
    }

    @Test
    fun `score below 60 decides GIVE_UP`() {
        val result = BuyWiseEngine.assess(profile, input(2f, 2f, 2f))
        assertEquals(Decision.GIVE_UP, result.finalDecision)
        assertNull(result.refineDetail)
    }

    @Test
    fun `score in 60-79 decides REFINE without refine inputs`() {
        // (7×0.4 + 5×0.3 + 5×0.3) × 10 = 58 → GIVE_UP；改用 68 分组合
        // (8×0.4 + 6×0.3 + 6×0.3) × 10 = 68
        val result = BuyWiseEngine.assess(profile, input(8f, 6f, 6f))
        assertEquals(Decision.REFINE, result.baseDecision)
        assertEquals(Decision.REFINE, result.finalDecision)
        val refine = result.refineDetail
        assertTrue(refine != null && !refine.completed)
    }

    @Test
    fun `refine passing both metrics upgrades to BUY`() {
        // 68 分进入精算；价格 100，残值 0，使用 500 次 → 单次 0.2 ≤ 0.5H=50；效用 10 ≥ 3
        val result = BuyWiseEngine.assess(
            profile,
            AssessmentInput("测试", 100.0, 8f, 6f, 6f, resaleValue = 0.0, estimatedUses = 500.0, annualUtilityValue = 10.0)
        )
        assertEquals(Decision.BUY, result.finalDecision)
        assertTrue(result.refineDetail!!.passed)
    }

    @Test
    fun `refine failing unit cost downgrades to GIVE_UP`() {
        // 价格 100，使用 1 次 → 单次 100 > 50 阈值
        val result = BuyWiseEngine.assess(
            profile,
            AssessmentInput("测试", 100.0, 8f, 6f, 6f, resaleValue = 0.0, estimatedUses = 1.0, annualUtilityValue = 100.0)
        )
        assertEquals(Decision.GIVE_UP, result.finalDecision)
        assertFalse(result.refineDetail!!.passed)
    }

    // ---------- 万能反悔条款 ----------

    @Test
    fun `cooling off triggers when price reaches 10 percent of salary`() {
        // 月薪 17400 的 10% = 1740
        val exactly = BuyWiseEngine.assess(profile, input(10f, 10f, 10f, price = 1740.0))
        assertTrue(exactly.requiresCoolingOff)
        assertEquals(BuyWiseEngine.COOLING_OFF_HOURS, exactly.coolingOffHours)

        val below = BuyWiseEngine.assess(profile, input(10f, 10f, 10f, price = 1739.0))
        assertFalse(below.requiresCoolingOff)
    }

    // ---------- buildRefine ----------

    @Test
    fun `buildRefine without uses is incomplete`() {
        val detail = BuyWiseEngine.buildRefine(100.0, 0.0, 0.0, 0.0, profile.hourlyWage)
        assertNull(detail.realUnitCost)
        assertNull(detail.unitCostPass)
        assertFalse(detail.completed)
        assertFalse(detail.passed)
    }

    @Test
    fun `buildRefine skips opportunity check when utility missing`() {
        val detail = BuyWiseEngine.buildRefine(100.0, 0.0, 500.0, 0.0, profile.hourlyWage)
        assertTrue(detail.completed)
        assertTrue(detail.unitCostPass == true)
        assertNull(detail.opportunityPass)
        // 机会成本通过 = 效用缺失时不拦截
        assertTrue(detail.passed)
    }

    @Test
    fun `buildRefine net cost never negative`() {
        // 残值高于买入价 → 净成本钳制为 0
        val detail = BuyWiseEngine.buildRefine(100.0, 200.0, 10.0, 0.0, profile.hourlyWage)
        assertEquals(0.0, detail.netCost, 1e-9)
        assertEquals(0.0, detail.realUnitCost!!, 1e-9)
    }

    @Test
    fun `buildRefine opportunity fails when utility below gain`() {
        // 机会成本收益 = 100 × 3% = 3；效用 2 < 3 → 未通过
        val detail = BuyWiseEngine.buildRefine(100.0, 0.0, 500.0, 2.0, profile.hourlyWage)
        assertFalse(detail.opportunityPass!!)
        assertFalse(detail.passed)
    }

    // ---------- evaluateLimitedTime ----------

    private fun limitedInput(
        original: Double = 200.0,
        promo: Double = 100.0,
        hours: Double = 24.0,
        q1: Boolean = true,
        q2: Boolean = true,
        q3: Boolean = true
    ) = LimitedTimeInput(original, promo, hours, q1, q2, q3)

    @Test
    fun `limited time passes when V positive and all questions true`() {
        val result = BuyWiseEngine.evaluateLimitedTime(
            profile, 100.0, limitedInput(), resaleValue = 0.0, estimatedUses = 500.0
        )
        // V = (200-100) - 200×30% = 40 > 0
        assertEquals(40.0, result.netValue, 1e-9)
        assertTrue(result.allQuestionsPassed)
        assertEquals(Decision.BUY, result.decision)
    }

    @Test
    fun `discount trap when idle loss exceeds saving`() {
        // V = (110-100) - 110×30% = -23 ≤ 0 → 折扣是陷阱
        val result = BuyWiseEngine.evaluateLimitedTime(
            profile, 100.0, limitedInput(original = 110.0, promo = 100.0),
            resaleValue = 0.0, estimatedUses = 500.0
        )
        assertEquals(-23.0, result.netValue, 1e-9)
        assertEquals(Decision.GIVE_UP, result.decision)
    }

    @Test
    fun `three question filter blocks purchase`() {
        val result = BuyWiseEngine.evaluateLimitedTime(
            profile, 100.0, limitedInput(q1 = false),
            resaleValue = 0.0, estimatedUses = 500.0
        )
        assertFalse(result.allQuestionsPassed)
        assertEquals(Decision.GIVE_UP, result.decision)
    }

    @Test
    fun `T_max is min of remaining hours and worth hours`() {
        // V = 40, H = 100 → worth = 0.4h；剩余 24h → T_max = 0.4
        val bounded = BuyWiseEngine.evaluateLimitedTime(
            profile, 100.0, limitedInput(hours = 24.0), resaleValue = 0.0, estimatedUses = 500.0
        )
        assertEquals(0.4, bounded.maxDecisionHours, 1e-9)

        // 剩余 0.1h < 0.4h → T_max = 0.1
        val tight = BuyWiseEngine.evaluateLimitedTime(
            profile, 100.0, limitedInput(hours = 0.1), resaleValue = 0.0, estimatedUses = 500.0
        )
        assertEquals(0.1, tight.maxDecisionHours, 1e-9)
    }
}
