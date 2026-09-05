package com.buywise.app.ui.assessment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.buywise.app.domain.model.AssessmentResult
import com.buywise.app.domain.model.Decision
import com.buywise.app.domain.model.FinanceProfile
import com.buywise.app.domain.model.LimitedTimeResult
import com.buywise.app.domain.model.RefineDetail
import com.buywise.app.ui.util.formatHours
import com.buywise.app.ui.util.formatMoney
import com.buywise.app.ui.util.formatScore

// ---------------- 决策文案 ----------------

internal fun Decision.title(): String = when (this) {
    Decision.BUY -> "买"
    Decision.REFINE -> "进入精算"
    Decision.GIVE_UP -> "不买"
}

internal fun Decision.advice(): String = when (this) {
    Decision.BUY -> "综合评分达标，可以直接下单。"
    Decision.REFINE -> "基础分落在 60-79 区间，需要补充残值与使用次数做精算。"
    Decision.GIVE_UP -> "评分或精算未通过，建议放入观望清单并设置降价提醒。"
}

// ---------------- 通用输入 ----------------

@Composable
internal fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    placeholder: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        suffix = suffix?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

@Composable
internal fun DimensionSlider(
    title: String,
    weightLabel: String,
    value: Float,
    description: String,
    onValueChange: (Float) -> Unit,
    suggestedValue: Float? = null,
    onApplySuggestion: (() -> Unit)? = null
) {
    val score = value.toInt()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            if (suggestedValue != null && onApplySuggestion != null) {
                TextButton(onClick = onApplySuggestion) {
                    Text("建议 ${suggestedValue.toInt()} 分")
                }
            }
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = "$score 分 · $weightLabel",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..10f,
            steps = 9,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------- 卡片 ----------------

@Composable
internal fun ProfileBar(profile: FinanceProfile) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            ProfileCell(
                modifier = Modifier.weight(1f),
                label = "时薪 H",
                value = "¥${formatMoney(profile.hourlyWage)}"
            )
            ProfileCell(
                modifier = Modifier.weight(1f),
                label = "日沉没成本 S",
                value = "¥${formatMoney(profile.dailySunkCost)}"
            )
        }
    }
}

@Composable
private fun ProfileCell(modifier: Modifier, label: String, value: String) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
internal fun ResultCard(
    result: AssessmentResult,
    onSaveToHistory: (() -> Unit)? = null,
    onAddToWatchlist: (() -> Unit)? = null
) {
    val isBuy = result.finalDecision == Decision.BUY
    val containerColor = when (result.finalDecision) {
        Decision.BUY -> MaterialTheme.colorScheme.primaryContainer
        Decision.GIVE_UP -> MaterialTheme.colorScheme.errorContainer
        Decision.REFINE -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = when (result.finalDecision) {
        Decision.BUY -> MaterialTheme.colorScheme.onPrimaryContainer
        Decision.GIVE_UP -> MaterialTheme.colorScheme.onErrorContainer
        Decision.REFINE -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = containerColor,
                contentColor = contentColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "决策建议：${result.finalDecision.title()}",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = result.finalDecision.advice(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Text(
                text = if (result.itemName.isBlank()) "基础总分" else "${result.itemName} · 基础总分",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${formatScore(result.score)} / 100",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "计分：(R×0.4 + E×0.3 + F×0.3) × 10",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (result.requiresCoolingOff && isBuy) {
                Text(
                    text = "万能反悔条款已触发：单价 ≥ 月薪 10%，请强制冷静 ${result.coolingOffHours} 小时。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (onSaveToHistory != null || onAddToWatchlist != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (onSaveToHistory != null) {
                        OutlinedButton(
                            onClick = onSaveToHistory,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("存入历史")
                        }
                    }
                    if (onAddToWatchlist != null) {
                        FilledTonalButton(
                            onClick = onAddToWatchlist,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("加入观望清单")
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun RefineCard(
    detail: RefineDetail,
    resaleText: String,
    usesText: String,
    utilityText: String,
    canRun: Boolean,
    onResaleChange: (String) -> Unit,
    onUsesChange: (String) -> Unit,
    onUtilityChange: (String) -> Unit,
    onRunRefine: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "第三步 · 精算", style = MaterialTheme.typography.titleMedium)

            NumberField(
                label = "二手预估残值（去闲鱼搜同款 1 年后售价）",
                value = resaleText,
                onValueChange = onResaleChange,
                suffix = "元",
                placeholder = "例如 800"
            )
            NumberField(
                label = "预估总使用次数",
                value = usesText,
                onValueChange = onUsesChange,
                suffix = "次",
                placeholder = "例如 200"
            )
            NumberField(
                label = "全年愉悦 / 效用估值（用于对比机会成本）",
                value = utilityText,
                onValueChange = onUtilityChange,
                suffix = "元",
                placeholder = "例如 300"
            )

            Text(
                text = "指标① 单次真实成本 = (买入价 - 残值) ÷ 预估总使用次数 = ${
                    detail.realUnitCost?.let { "¥${formatMoney(it)}" } ?: "待填写"
                }（阈值 ≤ ¥${formatMoney(detail.unitCostThreshold)}，即 0.5H）",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "指标② 机会成本 = 买入价 × 3% = ¥${formatMoney(detail.opportunityGain)}；" +
                    "若该金额 > 全年效用估值，则「钱生钱」更划算。",
                style = MaterialTheme.typography.bodyMedium
            )

            if (detail.completed) {
                Text(
                    text = "指标① ${if (detail.unitCostPass == true) "通过" else "未通过"} · " +
                        "指标② ${
                            when (detail.opportunityPass) {
                                true -> "通过"
                                false -> "未通过"
                                null -> "未填写效用估值，暂不拦截"
                            }
                        }",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            androidx.compose.material3.Button(
                onClick = onRunRefine,
                enabled = canRun,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("完成精算")
            }
        }
    }
}

@Composable
internal fun LimitedTimeCard(
    enabled: Boolean,
    originalPriceText: String,
    promoPriceText: String,
    remainingHoursText: String,
    q1: Boolean,
    q2: Boolean,
    q3: Boolean,
    result: LimitedTimeResult?,
    onToggle: (Boolean) -> Unit,
    onOriginalPriceChange: (String) -> Unit,
    onPromoPriceChange: (String) -> Unit,
    onRemainingHoursChange: (String) -> Unit,
    onQ1Change: (Boolean) -> Unit,
    onQ2Change: (Boolean) -> Unit,
    onQ3Change: (Boolean) -> Unit,
    onRun: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "第四步 · 限时决策协议",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = enabled, onCheckedChange = onToggle)
            }

            if (enabled) {
                NumberField(
                    label = "原价",
                    value = originalPriceText,
                    onValueChange = onOriginalPriceChange,
                    suffix = "元"
                )
                NumberField(
                    label = "活动价",
                    value = promoPriceText,
                    onValueChange = onPromoPriceChange,
                    suffix = "元"
                )
                NumberField(
                    label = "折扣剩余时间",
                    value = remainingHoursText,
                    onValueChange = onRemainingHoursChange,
                    suffix = "小时"
                )

                Text(
                    text = "三问快筛（必须全部勾选才买）",
                    style = MaterialTheme.typography.titleSmall
                )
                QuestionRow("① 没有这个折扣，我下个月也会以原价买", q1, onQ1Change)
                QuestionRow("② 买完后不需要额外花钱买配件 / 耗材 / 保养", q2, onQ2Change)
                QuestionRow("③ 这笔钱没有更明确、更优的去处", q3, onQ3Change)

                androidx.compose.material3.Button(
                    onClick = onRun,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("执行限时决策")
                }

                result?.let {
                    Text(
                        text = "折扣真实价值 V = ¥${formatMoney(it.netValue)}" +
                            "（省 ¥${formatMoney(it.grossSaving)} − 闲置损失 ¥${formatMoney(it.potentialIdleLoss)}）",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "最大决策时间 T_max = ${formatHours(it.maxDecisionHours)} 小时",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (it.decision == Decision.BUY)
                            "结论：V > 0 且三问通过，请在 T_max 截止前买入"
                        else
                            "结论：${if (it.netValue <= 0.0) "折扣是陷阱（V ≤ 0），不买立省 100%"; else "三问未全通过或单次成本超标，坚决放弃"}",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (it.decision == Decision.BUY)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestionRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
