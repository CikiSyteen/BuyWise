package com.buywise.app.data.repository

import com.buywise.app.data.local.AssessmentRecordDao
import com.buywise.app.data.local.AssessmentRecordEntity
import com.buywise.app.data.local.RecordStatus
import com.buywise.app.domain.model.AssessmentResult
import com.buywise.app.domain.model.Decision
import com.buywise.app.domain.model.LimitedTimeResult
import com.buywise.app.domain.model.RefineDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 限时决策协议摘要（存档用） */
data class LimitedTimeSummary(
    val netValue: Double,
    val maxDecisionHours: Double,
    val decision: Decision
)

/** 历史记录的领域层展示模型：完整评估结果 + 精算输入快照 + 归属状态 */
data class AssessmentRecord(
    val id: Long,
    val createdAt: Long,
    val status: RecordStatus,
    val r: Float,
    val e: Float,
    val f: Float,
    val resaleValue: Double,
    val estimatedUses: Double,
    val annualUtilityValue: Double,
    val result: AssessmentResult,
    val limitedTime: LimitedTimeSummary?
)

/**
 * 评估历史仓库：负责 entity ↔ domain 模型互转，
 * UI 层只接触 [AssessmentRecord]。
 */
class AssessmentRepository(private val dao: AssessmentRecordDao) {

    val allRecords: Flow<List<AssessmentRecord>> =
        dao.observeAll().map { list -> list.map { it.toRecord() } }

    suspend fun saveRecord(
        result: AssessmentResult,
        r: Float,
        e: Float,
        f: Float,
        resaleValue: Double,
        estimatedUses: Double,
        annualUtilityValue: Double,
        limitedTimeResult: LimitedTimeResult?,
        status: RecordStatus
    ): Long {
        val refine = result.refineDetail
        val entity = AssessmentRecordEntity(
            createdAt = System.currentTimeMillis(),
            itemName = result.itemName,
            price = result.price,
            r = r,
            e = e,
            f = f,
            score = result.score,
            baseDecision = result.baseDecision.name,
            finalDecision = result.finalDecision.name,
            resaleValue = resaleValue,
            estimatedUses = estimatedUses,
            annualUtilityValue = annualUtilityValue,
            netCost = refine?.netCost ?: 0.0,
            realUnitCost = refine?.realUnitCost,
            unitCostThreshold = refine?.unitCostThreshold ?: 0.0,
            unitCostPass = refine?.unitCostPass,
            opportunityGain = refine?.opportunityGain ?: 0.0,
            opportunityPass = refine?.opportunityPass,
            refineCompleted = refine?.completed ?: false,
            refinePassed = refine?.passed ?: false,
            requiresCoolingOff = result.requiresCoolingOff,
            coolingOffHours = result.coolingOffHours,
            ltNetValue = limitedTimeResult?.netValue,
            ltMaxDecisionHours = limitedTimeResult?.maxDecisionHours,
            ltDecision = limitedTimeResult?.decision?.name,
            status = status.name
        )
        return dao.insert(entity)
    }

    suspend fun findById(id: Long): AssessmentRecord? = dao.getById(id)?.toRecord()

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun setStatus(id: Long, status: RecordStatus) =
        dao.updateStatus(id, status.name)

    // ---------- entity -> domain ----------

    private fun AssessmentRecordEntity.toRecord(): AssessmentRecord {
        val base = Decision.valueOf(baseDecision)
        val refineDetail = if (base == Decision.REFINE) {
            RefineDetail(
                netCost = netCost,
                realUnitCost = realUnitCost,
                unitCostThreshold = unitCostThreshold,
                unitCostPass = unitCostPass,
                opportunityGain = opportunityGain,
                annualUtilityValue = annualUtilityValue,
                opportunityPass = opportunityPass,
                completed = refineCompleted,
                passed = refinePassed
            )
        } else {
            null
        }

        return AssessmentRecord(
            id = id,
            createdAt = createdAt,
            status = runCatching { RecordStatus.valueOf(status) }
                .getOrDefault(RecordStatus.HISTORY),
            r = r,
            e = e,
            f = f,
            resaleValue = resaleValue,
            estimatedUses = estimatedUses,
            annualUtilityValue = annualUtilityValue,
            result = AssessmentResult(
                itemName = itemName,
                price = price,
                score = score,
                baseDecision = base,
                finalDecision = Decision.valueOf(finalDecision),
                refineDetail = refineDetail,
                requiresCoolingOff = requiresCoolingOff,
                coolingOffHours = coolingOffHours
            ),
            limitedTime = if (ltNetValue != null && ltMaxDecisionHours != null && ltDecision != null) {
                LimitedTimeSummary(
                    netValue = ltNetValue,
                    maxDecisionHours = ltMaxDecisionHours,
                    decision = Decision.valueOf(ltDecision)
                )
            } else {
                null
            }
        )
    }
}
