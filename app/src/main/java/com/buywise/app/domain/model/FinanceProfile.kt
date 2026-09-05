package com.buywise.app.domain.model

/**
 * 个人财务杠杆（算法文档第一步）
 *
 * 时薪 H = 税后月薪 ÷ 21.75 ÷ 8
 * 日沉没成本 S = 月固定支出 ÷ 30
 */
data class FinanceProfile(
    val monthlySalary: Double = 0.0,
    val monthlyFixedExpense: Double = 0.0
) {

    val hourlyWage: Double
        get() = if (monthlySalary <= 0.0) 0.0
        else monthlySalary / WORK_DAYS_PER_MONTH / WORK_HOURS_PER_DAY

    val dailySunkCost: Double
        get() = if (monthlyFixedExpense <= 0.0) 0.0
        else monthlyFixedExpense / DAYS_PER_MONTH

    val isValid: Boolean
        get() = monthlySalary > 0.0

    companion object {
        const val WORK_DAYS_PER_MONTH = 21.75
        const val WORK_HOURS_PER_DAY = 8.0
        const val DAYS_PER_MONTH = 30.0
    }
}
