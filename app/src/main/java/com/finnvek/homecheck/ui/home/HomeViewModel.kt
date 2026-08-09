package com.finnvek.homecheck.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.homecheck.data.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        repository: HomeRepository,
    ) : ViewModel() {
        val state: StateFlow<HomeUiState> =
            combine(repository.assets, repository.tasks, ::HomeUiState).stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                HomeUiState(),
            )
    }
