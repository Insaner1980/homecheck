package com.finnvek.homecheck.ui.maintenance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.homecheck.data.local.entity.MaintenanceTaskEntity
import com.finnvek.homecheck.data.repository.HomeRepository
import com.finnvek.homecheck.domain.RecurrenceUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MaintenanceFormViewModel @Inject constructor(
    private val repository: HomeRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val taskId: String? = savedState["taskId"]
    private val routeAssetId: String? = savedState["assetId"]
    private val mutableState = MutableStateFlow(restoredState())
    val state = mutableState.asStateFlow()
    val assets = repository.assets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val mutableSaved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saved = mutableSaved.asSharedFlow()

    init {
        if (taskId != null && mutableState.value.title.isBlank()) {
            viewModelScope.launch { repository.task(taskId)?.let(::load) }
        }
    }

    fun update(value: MaintenanceFormState) {
        mutableState.value = value
        savedState["maintenance_asset"] = value.assetId
        savedState["maintenance_title"] = value.title
        savedState["maintenance_due"] = value.dueDate
        savedState["maintenance_notes"] = value.notes
        savedState["maintenance_interval"] = value.recurrenceInterval
        savedState["maintenance_unit"] = value.recurrenceUnit?.name
        savedState["maintenance_reminder"] = value.reminderEnabled
    }

    fun save() {
        val form = state.value
        val interval = form.recurrenceInterval.toIntOrNull()
        val recurrenceError = form.recurrenceUnit != null && (interval == null || interval <= 0)
        val validated = form.copy(
            assetError = form.assetId.isBlank(),
            titleError = form.title.isBlank(),
            dueDateError = form.dueDate.isBlank(),
            recurrenceError = recurrenceError,
            saveError = false,
        )
        update(validated)
        if (validated.assetError || validated.titleError || validated.dueDateError || validated.recurrenceError || form.isSaving) return
        mutableState.value = validated.copy(isSaving = true)
        viewModelScope.launch {
            val existing = taskId?.let { repository.task(it) }
            val now = System.currentTimeMillis()
            runCatching {
                repository.upsertTask(
                    MaintenanceTaskEntity(
                        id = existing?.id ?: UUID.randomUUID().toString(),
                        assetId = form.assetId,
                        title = form.title.trim(),
                        dueDate = LocalDate.parse(form.dueDate),
                        notes = form.notes.trim().ifBlank { null },
                        recurrenceInterval = if (form.recurrenceUnit == null) null else interval,
                        recurrenceUnit = form.recurrenceUnit,
                        reminderEnabled = form.reminderEnabled,
                        createdAt = existing?.createdAt ?: now,
                        updatedAt = now,
                    ),
                )
            }.onSuccess { mutableSaved.emit(Unit) }
                .onFailure { mutableState.value = validated.copy(isSaving = false, saveError = true) }
        }
    }

    private fun load(task: MaintenanceTaskEntity) = update(
        MaintenanceFormState(
            taskId = task.id,
            assetId = task.assetId,
            title = task.title,
            dueDate = task.dueDate.toString(),
            notes = task.notes.orEmpty(),
            recurrenceInterval = task.recurrenceInterval?.toString().orEmpty(),
            recurrenceUnit = task.recurrenceUnit,
            reminderEnabled = task.reminderEnabled,
        ),
    )

    private fun restoredState() = MaintenanceFormState(
        taskId = taskId,
        assetId = savedState["maintenance_asset"] ?: routeAssetId.orEmpty(),
        title = savedState["maintenance_title"] ?: "",
        dueDate = savedState["maintenance_due"] ?: "",
        notes = savedState["maintenance_notes"] ?: "",
        recurrenceInterval = savedState["maintenance_interval"] ?: "",
        recurrenceUnit = (savedState.get<String>("maintenance_unit"))?.let { runCatching { RecurrenceUnit.valueOf(it) }.getOrNull() },
        reminderEnabled = savedState["maintenance_reminder"] ?: true,
    )
}
