package com.buywise.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buywise.app.data.local.RecordStatus
import com.buywise.app.data.repository.AssessmentRecord
import com.buywise.app.data.repository.AssessmentRepository
import com.buywise.app.ui.assessment.ResultCard
import com.buywise.app.ui.util.formatDateTime
import com.buywise.app.ui.util.formatHours
import com.buywise.app.ui.util.formatMoney
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryDetailViewModel(
    private val recordId: Long,
    private val repository: AssessmentRepository
) : ViewModel() {

    private val _record = MutableStateFlow<AssessmentRecord?>(null)
    val record: StateFlow<AssessmentRecord?> = _record.asStateFlow()

    init {
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch { _record.value = repository.findById(recordId) }
    }

    fun moveToWatchlist() {
        viewModelScope.launch {
            repository.setStatus(recordId, RecordStatus.WATCHLIST)
            refresh()
        }
    }

    fun moveToHistory() {
        viewModelScope.launch {
            repository.setStatus(recordId, RecordStatus.HISTORY)
            refresh()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteById(recordId)
            onDeleted()
        }
    }
}

@Composable
fun HistoryDetailRoute(
    viewModel: HistoryDetailViewModel,
    onBack: () -> Unit
) {
    val record by viewModel.record.collectAsStateWithLifecycle()
    HistoryDetailScreen(
        record = record,
        onMoveToWatchlist = viewModel::moveToWatchlist,
        onMoveToHistory = viewModel::moveToHistory,
        onDelete = { viewModel.delete(onBack) }
    )
}

@Composable
fun HistoryDetailScreen(
    record: AssessmentRecord?,
    onMoveToWatchlist: () -> Unit,
    onMoveToHistory: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (record == null) {
        Column(modifier = modifier.fillMaxSize()) {
            HistoryDetailTopBar()
            Text(
                text = "记录不存在或已删除",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HistoryDetailTopBar()

        Text(
            text = "评估时间：${formatDateTime(record.createdAt)} · " +
                if (record.status == RecordStatus.WATCHLIST) "观望中" else "已归档",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ResultCard(result = record.result)

        record.result.refineDetail?.let { refine ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "精算明细", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "净成本 = (买入价 - 残值) = ¥${formatMoney(refine.netCost)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "指标① 单次真实成本 = ${
                            refine.realUnitCost?.let { "¥${formatMoney(it)}" } ?: "未填写使用次数"
                        }（阈值 ≤ ¥${formatMoney(refine.unitCostThreshold)}，即 0.5H）",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "指标② 机会成本收益 = ¥${formatMoney(refine.opportunityGain)} " +
                            "vs 全年效用估值 ¥${formatMoney(refine.annualUtilityValue)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (refine.completed) {
                        Text(
                            text = "结论：指标① ${if (refine.unitCostPass == true) "通过" else "未通过"} · " +
                                "指标② ${
                                    when (refine.opportunityPass) {
                                        true -> "通过"
                                        false -> "未通过"
                                        null -> "未填写效用估值，未拦截"
                                    }
                                }",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        record.limitedTime?.let { lt ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "限时决策协议", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "折扣真实价值 V = ¥${formatMoney(lt.netValue)}；" +
                            "最大决策时间 T_max = ${formatHours(lt.maxDecisionHours)} 小时",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "结论：${lt.decision.name.let { if (it == "BUY") "买入" else "放弃" }}",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (lt.decision == com.buywise.app.domain.model.Decision.BUY)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "三维评分", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "需求刚性 R ${record.r.toInt()} 分（权重 40%）· " +
                        "使用效率 E ${record.e.toInt()} 分（30%）· " +
                        "财务健康度 F ${record.f.toInt()} 分（30%）",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (record.status == RecordStatus.HISTORY) {
                OutlinedButton(
                    onClick = onMoveToWatchlist,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("移入观望清单")
                }
            } else {
                OutlinedButton(
                    onClick = onMoveToHistory,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("移回历史")
                }
            }
            FilledTonalButton(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text("删除记录")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun HistoryDetailTopBar() {
    Text(text = "评估详情", style = MaterialTheme.typography.headlineMedium)
}
