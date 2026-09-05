package com.buywise.app.ui.assessment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import com.buywise.app.domain.ScoreDescriptors
import com.buywise.app.domain.model.Decision
import com.buywise.app.ui.util.sanitizeDecimal

@Composable
fun AssessmentRoute(viewModel: AssessmentViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    AssessmentScreen(
        uiState = uiState,
        onItemNameChange = viewModel::onItemNameChange,
        onPriceChange = viewModel::onPriceChange,
        onRChange = viewModel::onRChange,
        onEChange = viewModel::onEChange,
        onFChange = viewModel::onFChange,
        onApplySuggestedF = viewModel::applySuggestedF,
        onCalculate = viewModel::calculate,
        onResaleChange = viewModel::onResaleValueChange,
        onUsesChange = viewModel::onEstimatedUsesChange,
        onUtilityChange = viewModel::onAnnualUtilityChange,
        onRunRefine = viewModel::runRefine,
        onLimitedTimeToggle = viewModel::onLimitedTimeToggle,
        onOriginalPriceChange = viewModel::onOriginalPriceChange,
        onPromoPriceChange = viewModel::onPromoPriceChange,
        onRemainingHoursChange = viewModel::onRemainingHoursChange,
        onQ1Change = viewModel::onQ1Change,
        onQ2Change = viewModel::onQ2Change,
        onQ3Change = viewModel::onQ3Change,
        onRunLimitedTime = viewModel::runLimitedTime,
        onSaveToHistory = {
            viewModel.saveResult(toWatchlist = false) {
                Toast.makeText(context, "已存入历史", Toast.LENGTH_SHORT).show()
            }
        },
        onAddToWatchlist = {
            viewModel.saveResult(toWatchlist = true) {
                Toast.makeText(context, "已加入观望清单", Toast.LENGTH_SHORT).show()
            }
        }
    )
}

@Composable
fun AssessmentScreen(
    uiState: AssessmentUiState,
    onItemNameChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onRChange: (Float) -> Unit,
    onEChange: (Float) -> Unit,
    onFChange: (Float) -> Unit,
    onApplySuggestedF: () -> Unit,
    onCalculate: () -> Unit,
    onResaleChange: (String) -> Unit,
    onUsesChange: (String) -> Unit,
    onUtilityChange: (String) -> Unit,
    onRunRefine: () -> Unit,
    onLimitedTimeToggle: (Boolean) -> Unit,
    onOriginalPriceChange: (String) -> Unit,
    onPromoPriceChange: (String) -> Unit,
    onRemainingHoursChange: (String) -> Unit,
    onQ1Change: (Boolean) -> Unit,
    onQ2Change: (Boolean) -> Unit,
    onQ3Change: (Boolean) -> Unit,
    onRunLimitedTime: () -> Unit,
    onSaveToHistory: () -> Unit,
    onAddToWatchlist: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "购买评估", style = MaterialTheme.typography.headlineMedium)

        ProfileBar(profile = uiState.profile)

        // ---- 基础信息 ----
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "这件东西", style = MaterialTheme.typography.titleMedium)
                androidx.compose.material3.OutlinedTextField(
                    value = uiState.itemName,
                    onValueChange = onItemNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("物品名称") },
                    placeholder = { Text("例如 人体工学椅") },
                    singleLine = true
                )
                NumberField(
                    label = "价格",
                    value = uiState.priceText,
                    onValueChange = onPriceChange,
                    suffix = "元",
                    placeholder = "例如 1999"
                )
            }
        }

        // ---- 三维评分卡 ----
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(text = "第二步 · 三维核心评分卡", style = MaterialTheme.typography.titleMedium)

                DimensionSlider(
                    title = "需求刚性 R",
                    weightLabel = "40%",
                    value = uiState.r,
                    description = ScoreDescriptors.forR(uiState.r.toInt()),
                    onValueChange = onRChange
                )
                DimensionSlider(
                    title = "使用效率 E",
                    weightLabel = "30%",
                    value = uiState.e,
                    description = ScoreDescriptors.forE(uiState.e.toInt()),
                    onValueChange = onEChange
                )
                DimensionSlider(
                    title = "财务健康度 F",
                    weightLabel = "30%",
                    value = uiState.f,
                    description = ScoreDescriptors.forF(uiState.f.toInt()),
                    onValueChange = onFChange,
                    suggestedValue = uiState.suggestedF,
                    onApplySuggestion = onApplySuggestedF
                )

                Button(
                    onClick = onCalculate,
                    enabled = uiState.canCalculate,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("计算")
                }
            }
        }

        // ---- 结果 ----
        uiState.result?.let { result ->
            ResultCard(
                result = result,
                onSaveToHistory = onSaveToHistory,
                onAddToWatchlist =
                    if (result.finalDecision == Decision.GIVE_UP) onAddToWatchlist else null
            )

            if (result.baseDecision == Decision.REFINE && result.refineDetail != null) {
                RefineCard(
                    detail = result.refineDetail,
                    resaleText = uiState.resaleText,
                    usesText = uiState.usesText,
                    utilityText = uiState.utilityText,
                    canRun = uiState.refineReady,
                    onResaleChange = onResaleChange,
                    onUsesChange = onUsesChange,
                    onUtilityChange = onUtilityChange,
                    onRunRefine = onRunRefine
                )
            }
        }

        // ---- 限时决策协议 ----
        LimitedTimeCard(
            enabled = uiState.limitedTimeEnabled,
            originalPriceText = uiState.originalPriceText,
            promoPriceText = uiState.promoPriceText,
            remainingHoursText = uiState.remainingHoursText,
            q1 = uiState.q1WouldBuyAtFullPrice,
            q2 = uiState.q2NoExtraSpending,
            q3 = uiState.q3NoBetterUse,
            result = uiState.limitedTimeResult,
            onToggle = onLimitedTimeToggle,
            onOriginalPriceChange = onOriginalPriceChange,
            onPromoPriceChange = onPromoPriceChange,
            onRemainingHoursChange = onRemainingHoursChange,
            onQ1Change = onQ1Change,
            onQ2Change = onQ2Change,
            onQ3Change = onQ3Change,
            onRun = onRunLimitedTime
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
