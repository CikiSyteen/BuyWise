package com.buywise.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buywise.app.data.local.RecordStatus
import com.buywise.app.data.repository.AssessmentRecord
import com.buywise.app.domain.model.Decision
import com.buywise.app.ui.assessment.title
import com.buywise.app.ui.util.formatDateTime
import com.buywise.app.ui.util.formatMoney
import com.buywise.app.ui.util.formatScore

@Composable
fun HistoryRoute(
    viewModel: HistoryViewModel,
    onOpenDetail: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(
        uiState = uiState,
        onOpenDetail = onOpenDetail,
        onDelete = viewModel::deleteRecord,
        onMoveToWatchlist = viewModel::moveToWatchlist,
        onMoveToHistory = viewModel::moveToHistory
    )
}

@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onOpenDetail: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onMoveToWatchlist: (Long) -> Unit,
    onMoveToHistory: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "评估记录",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
        )

        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("历史") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("观望清单") }
            )
        }

        val records =
            if (selectedTab == 0) uiState.historyRecords else uiState.watchlistRecords

        if (records.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedTab == 0) {
                        "还没有评估记录\n完成一次评估后点「存入历史」"
                    } else {
                        "观望清单是空的\n决策为「不买」时可将记录加入这里"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(records, key = { it.id }) { record ->
                    RecordListItem(
                        record = record,
                        onClick = { onOpenDetail(record.id) },
                        onDelete = onDelete,
                        onMoveToWatchlist = onMoveToWatchlist,
                        onMoveToHistory = onMoveToHistory
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordListItem(
    record: AssessmentRecord,
    onClick: () -> Unit,
    onDelete: (Long) -> Unit,
    onMoveToWatchlist: (Long) -> Unit,
    onMoveToHistory: (Long) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.result.itemName.ifBlank { "未命名物品" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "¥${formatMoney(record.result.price)} · ${formatDateTime(record.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${formatScore(record.result.score)} 分",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                DecisionBadge(decision = record.result.finalDecision)
            }

            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多操作"
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (record.status == RecordStatus.HISTORY) {
                        DropdownMenuItem(
                            text = { Text("移入观望清单") },
                            onClick = {
                                menuOpen = false
                                onMoveToWatchlist(record.id)
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("移回历史") },
                            onClick = {
                                menuOpen = false
                                onMoveToHistory(record.id)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("删除") },
                        onClick = {
                            menuOpen = false
                            onDelete(record.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DecisionBadge(decision: Decision) {
    val containerColor = when (decision) {
        Decision.BUY -> MaterialTheme.colorScheme.primaryContainer
        Decision.GIVE_UP -> MaterialTheme.colorScheme.errorContainer
        Decision.REFINE -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = when (decision) {
        Decision.BUY -> MaterialTheme.colorScheme.onPrimaryContainer
        Decision.GIVE_UP -> MaterialTheme.colorScheme.onErrorContainer
        Decision.REFINE -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        contentColor = contentColor
    ) {
        Text(
            text = decision.title(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}
