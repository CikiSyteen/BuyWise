package com.buywise.app.domain

import com.buywise.app.domain.model.AssessmentInput
import com.buywise.app.domain.model.AssessmentResult
import com.buywise.app.domain.model.Decision
import com.buywise.app.domain.model.FinanceProfile
import com.buywise.app.domain.model.LimitedTimeInput
import com.buywise.app.domain.model.LimitedTimeResult
import com.buywise.app.domain.model.RefineDetail

/**
 * BuyWise 三维量化决策模型（算法文档第二~五步）
 *
 * 纯 Kotlin、无 Android 依赖，便于单独单元测试。
 */
object BuyWiseEngine {

    // ---------- 权重与阈值 ----------
    const val WEIGHT_R = 0.4f
    const val WEIGHT_E = 0.3f
    const val WEIGHT_F = 0.3f

    const val SCORE_BUY = 80.0
    const val SCORE_REFINE = 60.0

    /** 单次真实成本阈值：≤ 0.5H */
    const val UNIT_COST_HOURS_RATIO = 0.5

    /** 机会成本：定投年化 3% */
    const val ANNUAL_RETURN_RATE = 0.03

    /** 冲动导致的潜在闲置损失：原价 × 30% */
    const val IMPULSE_LOSS_RATE = 0.30

    /** 万能反悔条款：单价 ≥ 月薪 10% */
    const val COOLING_OFF_RATE = 0.10
    const val COOLING_OFF_HOURS = 12

    // ---------- 第二步：三维核心评分卡 ----------

    /** 基础总分 = (R×0.4 + E×0.3 + F×0.3) × 10 */
    fun baseScore(r: Float, e: Float, f: Float): Double =
        (r * WEIGHT_R + e * WEIGHT_E + f * WEIGHT_F) * 10.0

    /**
     * 财务健康度 F 的系统建议值（供 UI 参考，用户仍可手动调整）
     * 10 分：价格 ≤ 2H；5 分：价格 ≈ 5H；0 分：价格 ≥ 20H，中间线性插值。
     */
    fun suggestF(price: Double, hourlyWage: Double): Float {
        if (price <= 0.0 || hourlyWage <= 0.0) return 5f
        val ratio = price / hourlyWage
        val score = when {
            ratio <= 2.0 -> 10.0
            ratio <= 5.0 -> 10.0 - (ratio - 2.0) / 3.0 * 5.0
            ratio <= 20.0 -> 5.0 - (ratio - 5.0) / 15.0 * 5.0
            else -> 0.0
        }
        return score.toFloat().coerceIn(0f, 10f)
    }

    /** 主评估入口 */
    fun assess(profile: FinanceProfile, input: AssessmentInput): AssessmentResult {
        val score = baseScore(input.r, input.e, input.f)

        val baseDecision = when {
            score >= SCORE_BUY -> Decision.BUY
            score >= SCORE_REFINE -> Decision.REFINE
            else -> Decision.GIVE_UP
        }

        val refine = if (baseDecision == Decision.REFINE) {
            buildRefine(
                price = input.price,
                resaleValue = input.resaleValue,
                estimatedUses = input.estimatedUses,
                annualUtilityValue = input.annualUtilityValue,
                hourlyWage = profile.hourlyWage
            )
        } else {
            null
        }

        val finalDecision = when {
            baseDecision != Decision.REFINE -> baseDecision
            refine == null || !refine.completed -> Decision.REFINE
            refine.passed -> Decision.BUY
            else -> Decision.GIVE_UP
        }

        return AssessmentResult(
            itemName = input.itemName,
            price = input.price,
            score = score,
            baseDecision = baseDecision,
            finalDecision = finalDecision,
            refineDetail = refine,
            requiresCoolingOff = profile.monthlySalary > 0.0 &&
                input.price >= profile.monthlySalary * COOLING_OFF_RATE,
            coolingOffHours = COOLING_OFF_HOURS
        )
    }

    /** 第三步：精算两个硬指标 */
    fun buildRefine(
        price: Double,
        resaleValue: Double,
        estimatedUses: Double,
        annualUtilityValue: Double,
        hourlyWage: Double
    ): RefineDetail {
        val netCost = (price - resaleValue).coerceAtLeast(0.0)
        val realUnitCost = if (estimatedUses > 0.0) netCost / estimatedUses else null
        val threshold = UNIT_COST_HOURS_RATIO * hourlyWage
        val unitCostPass = realUnitCost?.let { it <= threshold }
        val opportunityGain = price * ANNUAL_RETURN_RATE
        val opportunityPass =
            if (annualUtilityValue > 0.0) opportunityGain <= annualUtilityValue else null

        return RefineDetail(
            netCost = netCost,
            realUnitCost = realUnitCost,
            unitCostThreshold = threshold,
            unitCostPass = unitCostPass,
            opportunityGain = opportunityGain,
            annualUtilityValue = annualUtilityValue,
            opportunityPass = opportunityPass,
            completed = realUnitCost != null,
            passed = unitCostPass == true && opportunityPass != false
        )
    }

    // ---------- 第四步：限时决策协议 ----------

    /**
     * V =（原价 - 活动价）- 原价 × 30%
     * T_max = min（剩余促销时间，V ÷ H）
     * 通过条件：V > 0 且 三问快筛全通过 且 单次成本 ≤ 0.5H
     */
    fun evaluateLimitedTime(
        profile: FinanceProfile,
        price: Double,
        input: LimitedTimeInput,
        resaleValue: Double,
        estimatedUses: Double
    ): LimitedTimeResult {
        val grossSaving = (input.originalPrice - input.promoPrice).coerceAtLeast(0.0)
        val potentialIdleLoss = input.originalPrice * IMPULSE_LOSS_RATE
        val netValue = grossSaving - potentialIdleLoss

        val worthHours =
            if (profile.hourlyWage > 0.0) (netValue / profile.hourlyWage).coerceAtLeast(0.0)
            else 0.0
        val maxDecisionHours =
            if (input.remainingHours > 0.0) minOf(input.remainingHours, worthHours)
            else worthHours

        val allQuestionsPassed =
            input.wouldBuyAtFullPrice && input.noExtraSpending && input.noBetterUse

        val detail = buildRefine(
            price = price,
            resaleValue = resaleValue,
            estimatedUses = estimatedUses,
            annualUtilityValue = 0.0,
            hourlyWage = profile.hourlyWage
        )

        val decision =
            if (netValue > 0.0 && allQuestionsPassed && detail.unitCostPass != false) Decision.BUY
            else Decision.GIVE_UP

        return LimitedTimeResult(
            grossSaving = grossSaving,
            potentialIdleLoss = potentialIdleLoss,
            netValue = netValue,
            maxDecisionHours = maxDecisionHours,
            allQuestionsPassed = allQuestionsPassed,
            realUnitCost = detail.realUnitCost,
            unitCostPass = detail.unitCostPass,
            decision = decision
        )
    }
}
