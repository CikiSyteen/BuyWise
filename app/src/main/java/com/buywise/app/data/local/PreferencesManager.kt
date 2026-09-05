package com.buywise.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.buywise.app.domain.model.FinanceProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.buyWiseDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "buywise_prefs"
)

/**
 * 基于 DataStore 的轻量偏好存储，负责持久化「月薪」与「月固定支出」。
 */
class PreferencesManager(private val context: Context) {

    private val dataStore: DataStore<Preferences> get() = context.buyWiseDataStore

    val profileFlow: Flow<FinanceProfile> = dataStore.data.map { prefs ->
        FinanceProfile(
            monthlySalary = prefs[KEY_MONTHLY_SALARY] ?: 0.0,
            monthlyFixedExpense = prefs[KEY_MONTHLY_FIXED_EXPENSE] ?: 0.0
        )
    }

    suspend fun saveProfile(monthlySalary: Double, monthlyFixedExpense: Double) {
        dataStore.edit { prefs ->
            prefs[KEY_MONTHLY_SALARY] = monthlySalary
            prefs[KEY_MONTHLY_FIXED_EXPENSE] = monthlyFixedExpense
        }
    }

    private companion object {
        val KEY_MONTHLY_SALARY = doublePreferencesKey("monthly_salary")
        val KEY_MONTHLY_FIXED_EXPENSE = doublePreferencesKey("monthly_fixed_expense")
    }
}
