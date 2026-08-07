package com.finnvek.homecheck.ui.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.homecheck.data.repository.CompletionResult
import com.finnvek.homecheck.data.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface MaintenanceEvent {
    data class Completed(val result: CompletionResult) : MaintenanceEvent
    data object Failed : MaintenanceEvent
}

@HiltViewModel
class MaintenanceViewModel @Inject constructor(
    private val repository: HomeRepository,
) : ViewModel() {
    private val showHistory = MutableStateFlow(false)
    private val mutableEvents = MutableSharedFlow<MaintenanceEvent>(extraBufferCapacity = 1)
    val events = mutableEvents.asSharedFlow()

    val state = combine(repository.assets, repository.tasks, repository.history, showHistory) { assets, tasks, history, historyVisible ->
        MaintenanceUiState(assets, tasks, history, historyVisible)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MaintenanceUiState())

    fun showHistory(value: Boolean) { showHistory.value = value }

    fun complete(taskId: String) = viewModelScope.launch {
        runCatching { repository.completeMaintenance(taskId, LocalDate.now(), null) }
            .onSuccess { mutableEvents.emit(MaintenanceEvent.Completed(it)) }
            .onFailure { mutableEvents.emit(MaintenanceEvent.Failed) }
    }

    fun undo(result: CompletionResult) = viewModelScope.launch { repository.undoCompletion(result) }
}
