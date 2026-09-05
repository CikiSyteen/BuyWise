package com.buywise.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buywise.app.data.local.PreferencesManager
import com.buywise.app.domain.model.FinanceProfile
import com.buywise.app.ui.util.sanitizeDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val salaryText: String = "",
    val expenseText: String = "",
    val hourlyWage: Double = 0.0,
    val dailySunkCost: Double = 0.0,
    val canContinue: Boolean = false
)

class SettingsViewModel(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val salaryInput = MutableStateFlow("")
    private val expenseInput = MutableStateFlow("")

    /** 已保存的数据只在首次进入时回填一次，之后以用户输入为准 */
    private var seeded = false

    val uiState: StateFlow<SettingsUiState> =
        combine(salaryInput, expenseInput) { salary, expense ->
            val salaryValue = salary.toDoubleOrNull() ?: 0.0
            val expenseValue = expense.toDoubleOrNull() ?: 0.0
            SettingsUiState(
                salaryText = salary,
                expenseText = expense,
                hourlyWage = FinanceProfile(monthlySalary = salaryValue).hourlyWage,
                dailySunkCost = FinanceProfile(monthlyFixedExpense = expenseValue).dailySunkCost,
                canContinue = salaryValue > 0.0
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState()
        )

    init {
        viewModelScope.launch {
            preferencesManager.profileFlow.collect { profile ->
                if (seeded) return@collect
                seeded = true
                if (profile.monthlySalary > 0.0) {
                    salaryInput.update { profile.monthlySalary.plain() }
                }
                if (profile.monthlyFixedExpense > 0.0) {
                    expenseInput.update { profile.monthlyFixedExpense.plain() }
                }
            }
        }
    }

    fun onSalaryChange(value: String) {
        salaryInput.update { sanitizeDecimal(value) }
        persist()
    }

    fun onExpenseChange(value: String) {
        expenseInput.update { sanitizeDecimal(value) }
        persist()
    }

    /** 保存并跳转（保存完成后回调，避免竞态） */
    fun saveAndContinue(onSaved: () -> Unit) {
        viewModelScope.launch {
            persistNow()
            onSaved()
        }
    }

    private fun persist() {
        viewModelScope.launch { persistNow() }
    }

    private suspend fun persistNow() {
        preferencesManager.saveProfile(
            monthlySalary = salaryInput.value.toDoubleOrNull() ?: 0.0,
            monthlyFixedExpense = expenseInput.value.toDoubleOrNull() ?: 0.0
        )
    }

    private fun Double.plain(): String =
        if (this % 1.0 == 0.0) toLong().toString() else toString()
}
