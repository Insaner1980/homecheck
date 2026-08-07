package com.finnvek.homecheck.ui.assets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.homecheck.data.repository.HomeRepository
import com.finnvek.homecheck.data.files.AttachmentStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class AssetsViewModel @Inject constructor(
    repository: HomeRepository,
    val attachmentStore: AttachmentStore,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val needsAttention = MutableStateFlow(false)
    private val sort = MutableStateFlow(AssetSort.RECENT)

    private val records = combine(
        repository.assets,
        repository.tasks,
        repository.attachments,
    ) { assets, tasks, attachments ->
        AssetsUiState(assets = assets, tasks = tasks, attachments = attachments)
    }

    private val controls = combine(query, needsAttention, sort) { queryValue, attention, sortValue ->
        Triple(queryValue, attention, sortValue)
    }

    val state: StateFlow<AssetsUiState> = combine(records, controls) { records, controls ->
        records.copy(
            query = controls.first,
            needsAttentionOnly = controls.second,
            sort = controls.third,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AssetsUiState())

    fun setQuery(value: String) { query.value = value }
    fun setNeedsAttention(value: Boolean) { needsAttention.value = value }
    fun setSort(value: AssetSort) { sort.value = value }
}
