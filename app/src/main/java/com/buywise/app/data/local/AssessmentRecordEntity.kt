package com.buywise.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 记录归属：普通历史 / 观望清单 */
enum class RecordStatus {
    HISTORY,
    WATCHLIST
}

/**
 * 一次完整评估的快照：基础三维评分、精算明细、限时协议摘要与决策结果。
 * 是否有精算明细由 baseDecision == REFINE 推导，不单独存列。
 */
@Entity(tableName = "assessment_records")
data class AssessmentRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val createdAt: Long,

    // ---- 基础输入 ----
    val itemName: String,
    val price: Double,
    val r: Float,
    val e: Float,
    val f: Float,

    // ---- 基础结果 ----
    val score: Double,
    val baseDecision: String,
    val finalDecision: String,

    // ---- 精算输入快照 ----
    val resaleValue: Double,
    val estimatedUses: Double,
    val annualUtilityValue: Double,

    // ---- 精算结果（baseDecision != REFINE 时为默认值）----
    val netCost: Double,
    val realUnitCost: Double?,
    val unitCostThreshold: Double,
    val unitCostPass: Boolean?,
    val opportunityGain: Double,
    val opportunityPass: Boolean?,
    val refineCompleted: Boolean,
    val refinePassed: Boolean,

    // ---- 万能反悔条款 ----
    val requiresCoolingOff: Boolean,
    val coolingOffHours: Int,

    // ---- 限时决策协议摘要（未执行时为 null）----
    val ltNetValue: Double?,
    val ltMaxDecisionHours: Double?,
    val ltDecision: String?,

    // ---- 归属 ----
    val status: String = RecordStatus.HISTORY.name
)
