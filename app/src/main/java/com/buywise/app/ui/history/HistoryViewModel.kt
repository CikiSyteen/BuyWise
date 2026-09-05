package com.buywise.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buywise.app.data.local.RecordStatus
import com.buywise.app.data.repository.AssessmentRecord
import com.buywise.app.data.repository.AssessmentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val historyRecords: List<AssessmentRecord> = emptyList(),
    val watchlistRecords: List<AssessmentRecord> = emptyList()
)

class HistoryViewModel(
    private val repository: AssessmentRepository
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> =
        repository.allRecords
            .map { all ->
                HistoryUiState(
                    historyRecords = all.filter { it.status == RecordStatus.HISTORY },
                    watchlistRecords = all.filter { it.status == RecordStatus.WATCHLIST }
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = HistoryUiState()
            )

    fun deleteRecord(id: Long) {
        viewModelScope.launch { repository.deleteById(id) }
    }

    fun moveToWatchlist(id: Long) {
        viewModelScope.launch { repository.setStatus(id, RecordStatus.WATCHLIST) }
    }

    fun moveToHistory(id: Long) {
        viewModelScope.launch { repository.setStatus(id, RecordStatus.HISTORY) }
    }
}
