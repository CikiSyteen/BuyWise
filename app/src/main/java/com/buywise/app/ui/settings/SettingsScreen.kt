package com.buywise.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buywise.app.ui.util.formatMoney

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onNavigateToAssessment: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        uiState = uiState,
        onSalaryChange = viewModel::onSalaryChange,
        onExpenseChange = viewModel::onExpenseChange,
        onSaveAndContinue = { viewModel.saveAndContinue(onNavigateToAssessment) }
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onSalaryChange: (String) -> Unit,
    onExpenseChange: (String) -> Unit,
    onSaveAndContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "BuyWise",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "第一步：设定你的个人财务杠杆",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        MetricsHeader(
            hourlyWage = uiState.hourlyWage,
            dailySunkCost = uiState.dailySunkCost
        )

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "基础参数",
                    style = MaterialTheme.typography.titleMedium
                )

                MoneyField(
                    label = "税后月薪",
                    value = uiState.salaryText,
                    onValueChange = onSalaryChange,
                    placeholder = "例如 12000"
                )

                MoneyField(
                    label = "月固定支出（房租 + 基础吃喝）",
                    value = uiState.expenseText,
                    onValueChange = onExpenseChange,
                    placeholder = "例如 4500"
                )

                Text(
                    text = "时薪 H = 月薪 ÷ 21.75 ÷ 8；日沉没成本 S = 月固定支出 ÷ 30。数据会自动保存在本地。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Button(
            onClick = onSaveAndContinue,
            enabled = uiState.canContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存并进入评估")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MetricsHeader(
    hourlyWage: Double,
    dailySunkCost: Double
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            MetricCell(
                modifier = Modifier.weight(1f),
                label = "时薪 H",
                value = "¥${formatMoney(hourlyWage)}",
                unit = "每小时"
            )
            MetricCell(
                modifier = Modifier.weight(1f),
                label = "日沉没成本 S",
                value = "¥${formatMoney(dailySunkCost)}",
                unit = "每天"
            )
        }
    }
}

@Composable
private fun MetricCell(
    modifier: Modifier,
    label: String,
    value: String,
    unit: String
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MoneyField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        suffix = { Text("元") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}
