package com.buywise.app.domain.model

/** 最终决策：买 / 进入精算 / 不买 */
enum class Decision {
    BUY,
    REFINE,
    GIVE_UP
}

/** 三维评分卡 + 精算所需补充参数的输入 */
data class AssessmentInput(
    val itemName: String,
    val price: Double,
    val r: Float,
    val e: Float,
    val f: Float,
    /** 二手预估残值（精算指标①） */
    val resaleValue: Double = 0.0,
    /** 预估总使用次数（精算指标①） */
    val estimatedUses: Double = 0.0,
    /** 该物品带来的全年愉悦/效用估值，用于与机会成本比较（精算指标②） */
    val annualUtilityValue: Double = 0.0
)

/** 精算结果明细 */
data class RefineDetail(
    /** 买入价 - 残值 */
    val netCost: Double,
    /** 单次真实成本 = netCost ÷ 预估总使用次数 */
    val realUnitCost: Double?,
    /** 决策阈值 = 0.5H */
    val unitCostThreshold: Double,
    /** 单次成本是否 ≤ 0.5H */
    val unitCostPass: Boolean?,
    /** 机会成本收益 = 买入价 × 3% */
    val opportunityGain: Double,
    /** 用户填写的全年效用估值 */
    val annualUtilityValue: Double,
    /** 机会成本收益 ≤ 全年效用 才算通过；未填写效用时为 null */
    val opportunityPass: Boolean?,
    /** 精算是否已完成（需要填写预估使用次数） */
    val completed: Boolean,
    /** 两个指标是否都通过 */
    val passed: Boolean
)

/** 评估结果 */
data class AssessmentResult(
    val itemName: String,
    val price: Double,
    /** 基础总分 = (R×0.4 + E×0.3 + F×0.3) × 10 */
    val score: Double,
    /** 仅由基础总分得出的档位 */
    val baseDecision: Decision,
    /** 结合精算后的最终决策 */
    val finalDecision: Decision,
    val refineDetail: RefineDetail?,
    /** 是否触发万能反悔条款（单价 ≥ 月薪 10%） */
    val requiresCoolingOff: Boolean,
    val coolingOffHours: Int
)

/** 限时决策协议输入 */
data class LimitedTimeInput(
    val originalPrice: Double,
    val promoPrice: Double,
    val remainingHours: Double,
    /** ① 反悔测试：没有这个折扣，下个月也会以原价买 */
    val wouldBuyAtFullPrice: Boolean,
    /** ② 配套陷阱测试：无需额外花钱买配件/耗材/保养 */
    val noExtraSpending: Boolean,
    /** ③ 排他性测试：这笔钱没有更明确、更优的去处 */
    val noBetterUse: Boolean
)

/** 限时决策协议结果 */
data class LimitedTimeResult(
    /** 原价 - 活动价 */
    val grossSaving: Double,
    /** 冲动闲置损失 = 原价 × 30% */
    val potentialIdleLoss: Double,
    /** 折扣真实价值 V */
    val netValue: Double,
    /** 最大决策时间 T_max = min(剩余促销时间, V ÷ H) */
    val maxDecisionHours: Double,
    val allQuestionsPassed: Boolean,
    val realUnitCost: Double?,
    val unitCostPass: Boolean?,
    val decision: Decision
)
