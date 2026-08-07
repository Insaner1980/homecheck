package com.finnvek.homecheck.ui.assetdetail

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.homecheck.data.files.AttachmentStore
import com.finnvek.homecheck.data.local.entity.AttachmentEntity
import com.finnvek.homecheck.data.local.entity.AttachmentType
import com.finnvek.homecheck.data.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AssetDetailEvent { ASSET_DELETED, DOCUMENT_ADDED, OPERATION_FAILED }

@HiltViewModel
class AssetDetailViewModel @Inject constructor(
    private val repository: HomeRepository,
    val attachmentStore: AttachmentStore,
    savedState: SavedStateHandle,
) : ViewModel() {
    val assetId: String = requireNotNull(savedState["assetId"])
    private val mutableEvents = MutableSharedFlow<AssetDetailEvent>(extraBufferCapacity = 2)
    val events = mutableEvents.asSharedFlow()

    val state = combine(
        repository.asset(assetId),
        repository.tasksForAsset(assetId),
        repository.attachmentsForAsset(assetId),
        repository.historyForAsset(assetId),
    ) { asset, tasks, attachments, history ->
        AssetDetailUiState(asset, tasks, attachments, history, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AssetDetailUiState())

    fun importDocument(uri: Uri, type: AttachmentType) = viewModelScope.launch {
        runCatching {
            val attachment = attachmentStore.importFromUri(assetId, type, uri)
            try {
                repository.upsertAttachment(attachment)
            } catch (error: Throwable) {
                attachmentStore.delete(attachment.localPath)
                throw error
            }
        }.onSuccess { mutableEvents.emit(AssetDetailEvent.DOCUMENT_ADDED) }
            .onFailure { mutableEvents.emit(AssetDetailEvent.OPERATION_FAILED) }
    }

    fun renameAttachment(attachment: AttachmentEntity, name: String) = viewModelScope.launch {
        runCatching { repository.renameAttachment(attachment.id, name) }
            .onFailure { mutableEvents.emit(AssetDetailEvent.OPERATION_FAILED) }
    }

    fun changeType(attachment: AttachmentEntity, type: AttachmentType) = viewModelScope.launch {
        runCatching { repository.changeAttachmentType(attachment.id, type) }
            .onFailure { mutableEvents.emit(AssetDetailEvent.OPERATION_FAILED) }
    }

    fun deleteAttachment(attachment: AttachmentEntity) = viewModelScope.launch {
        runCatching {
            repository.deleteAttachment(attachment.id)
            attachmentStore.delete(attachment.localPath)
        }.onFailure { mutableEvents.emit(AssetDetailEvent.OPERATION_FAILED) }
    }

    fun deleteTask(taskId: String) = viewModelScope.launch {
        runCatching { repository.deleteTask(taskId) }
            .onFailure { mutableEvents.emit(AssetDetailEvent.OPERATION_FAILED) }
    }

    fun deleteAsset() = viewModelScope.launch {
        val attachments = repository.attachmentsNow(assetId)
        runCatching {
            repository.deleteAsset(assetId)
            attachmentStore.deleteAll(attachments)
        }.onSuccess { mutableEvents.emit(AssetDetailEvent.ASSET_DELETED) }
            .onFailure { mutableEvents.emit(AssetDetailEvent.OPERATION_FAILED) }
    }
}
