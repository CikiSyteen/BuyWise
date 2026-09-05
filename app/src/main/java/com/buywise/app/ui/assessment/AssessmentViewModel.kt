package com.buywise.app.ui.assessment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buywise.app.data.local.PreferencesManager
import com.buywise.app.domain.BuyWiseEngine
import com.buywise.app.domain.model.AssessmentInput
import com.buywise.app.domain.model.AssessmentResult
import com.buywise.app.domain.model.FinanceProfile
import com.buywise.app.domain.model.LimitedTimeInput
import com.buywise.app.domain.model.LimitedTimeResult
import com.buywise.app.ui.util.sanitizeDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class AssessmentUiState(
    val profile: FinanceProfile = FinanceProfile(),
    val itemName: String = "",
    val priceText: String = "",

    val r: Float = 5f,
    val e: Float = 5f,
    val f: Float = 5f,
    /** 系统根据价格与 H 反推的 F 建议值 */
    val suggestedF: Float = 5f,

    val result: AssessmentResult? = null,

    // 精算（60-79 分区间）
    val resaleText: String = "",
    val usesText: String = "",
    val utilityText: String = "",

    // 限时决策协议
    val limitedTimeEnabled: Boolean = false,
    val originalPriceText: String = "",
    val promoPriceText: String = "",
    val remainingHoursText: String = "",
    val q1WouldBuyAtFullPrice: Boolean = false,
    val q2NoExtraSpending: Boolean = true,
    val q3NoBetterUse: Boolean = true,
    val limitedTimeResult: LimitedTimeResult? = null
) {
    val priceValue: Double get() = priceText.toDoubleOrNull() ?: 0.0
    val canCalculate: Boolean get() = priceValue > 0.0
    val refineReady: Boolean get() = (usesText.toDoubleOrNull() ?: 0.0) > 0.0
}

class AssessmentViewModel(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val state = MutableStateFlow(AssessmentUiState())

    val uiState: StateFlow<AssessmentUiState> =
        combine(state, preferencesManager.profileFlow) { current, profile ->
            current.copy(
                profile = profile,
                suggestedF = BuyWiseEngine.suggestF(current.priceValue, profile.hourlyWage)
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AssessmentUiState()
        )

    // ---------- 基础输入 ----------

    fun onItemNameChange(value: String) {
        state.update { it.copy(itemName = value) }
    }

    fun onPriceChange(value: String) {
        state.update { it.copy(priceText = sanitizeDecimal(value), result = null) }
    }

    fun onRChange(value: Float) {
        state.update { it.copy(r = value, result = null) }
    }

    fun onEChange(value: Float) {
        state.update { it.copy(e = value, result = null) }
    }

    fun onFChange(value: Float) {
        state.update { it.copy(f = value, result = null) }
    }

    fun applySuggestedF() {
        state.update { it.copy(f = it.suggestedF, result = null) }
    }

    // ---------- 第二步：计算基础总分 ----------

    fun calculate() {
        val current = state.value
        if (!current.canCalculate) return

        val input = AssessmentInput(
            itemName = current.itemName,
            price = current.priceValue,
            r = current.r,
            e = current.e,
            f = current.f,
            resaleValue = current.resaleText.toDoubleOrNull() ?: 0.0,
            estimatedUses = current.usesText.toDoubleOrNull() ?: 0.0,
            annualUtilityValue = current.utilityText.toDoubleOrNull() ?: 0.0
        )

        state.update {
            it.copy(result = BuyWiseEngine.assess(it.profile, input))
        }
    }

    // ---------- 第三步：精算 ----------

    fun onResaleValueChange(value: String) {
        state.update { it.copy(resaleText = sanitizeDecimal(value)) }
    }

    fun onEstimatedUsesChange(value: String) {
        state.update { it.copy(usesText = sanitizeDecimal(value)) }
    }

    fun onAnnualUtilityChange(value: String) {
        state.update { it.copy(utilityText = sanitizeDecimal(value)) }
    }

    fun runRefine() {
        calculate()
    }

    // ---------- 第四步：限时决策协议 ----------

    fun onLimitedTimeToggle(value: Boolean) {
        state.update { it.copy(limitedTimeEnabled = value, limitedTimeResult = null) }
    }

    fun onOriginalPriceChange(value: String) {
        state.update { it.copy(originalPriceText = sanitizeDecimal(value), limitedTimeResult = null) }
    }

    fun onPromoPriceChange(value: String) {
        state.update { it.copy(promoPriceText = sanitizeDecimal(value), limitedTimeResult = null) }
    }

    fun onRemainingHoursChange(value: String) {
        state.update { it.copy(remainingHoursText = sanitizeDecimal(value), limitedTimeResult = null) }
    }

    fun onQ1Change(value: Boolean) {
        state.update { it.copy(q1WouldBuyAtFullPrice = value, limitedTimeResult = null) }
    }

    fun onQ2Change(value: Boolean) {
        state.update { it.copy(q2NoExtraSpending = value, limitedTimeResult = null) }
    }

    fun onQ3Change(value: Boolean) {
        state.update { it.copy(q3NoBetterUse = value, limitedTimeResult = null) }
    }

    fun runLimitedTime() {
        val current = state.value
        if (!current.canCalculate) return

        val input = LimitedTimeInput(
            originalPrice = current.originalPriceText.toDoubleOrNull() ?: 0.0,
            promoPrice = current.promoPriceText.toDoubleOrNull() ?: 0.0,
            remainingHours = current.remainingHoursText.toDoubleOrNull() ?: 0.0,
            wouldBuyAtFullPrice = current.q1WouldBuyAtFullPrice,
            noExtraSpending = current.q2NoExtraSpending,
            noBetterUse = current.q3NoBetterUse
        )

        val result = BuyWiseEngine.evaluateLimitedTime(
            profile = current.profile,
            price = current.priceValue,
            input = input,
            resaleValue = current.resaleText.toDoubleOrNull() ?: 0.0,
            estimatedUses = current.usesText.toDoubleOrNull() ?: 0.0
        )

        state.update { it.copy(limitedTimeResult = result) }
    }
}
