package com.finnvek.homecheck.ui.assets

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.net.toUri
import com.finnvek.homecheck.data.files.AttachmentStore
import com.finnvek.homecheck.data.local.entity.AssetEntity
import com.finnvek.homecheck.data.local.entity.AttachmentType
import com.finnvek.homecheck.data.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AssetSavedEvent(val assetId: String, val photoImportFailed: Boolean)

@HiltViewModel
class AssetFormViewModel @Inject constructor(
    private val repository: HomeRepository,
    private val attachmentStore: AttachmentStore,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val routeAssetId: String? = savedState["assetId"]
    private val mutableState = MutableStateFlow(restoredState())
    val state = mutableState.asStateFlow()
    private val mutableEvents = MutableSharedFlow<AssetSavedEvent>(extraBufferCapacity = 1)
    val events = mutableEvents.asSharedFlow()

    init {
        if (routeAssetId != null && mutableState.value.name.isBlank()) {
            viewModelScope.launch { repository.assetNow(routeAssetId)?.let(::load) }
        }
    }

    fun update(value: AssetFormState) {
        mutableState.value = value
        persist(value)
    }

    fun setPhoto(uri: Uri, temporaryFile: File? = null) = update(
        state.value.copy(pendingPhotoUri = uri.toString(), pendingPhotoTempPath = temporaryFile?.path),
    )

    fun createCameraFile(): File = attachmentStore.createCameraFile()
    fun cameraUri(file: File): Uri = attachmentStore.uriForCameraFile(file)

    fun save() {
        val form = state.value
        if (form.name.isBlank()) {
            update(form.copy(nameError = true))
            return
        }
        if (form.isSaving) return
        mutableState.value = form.copy(isSaving = true, saveError = false)
        viewModelScope.launch {
            val existing = routeAssetId?.let { repository.assetNow(it) }
            val now = System.currentTimeMillis()
            val assetId = existing?.id ?: UUID.randomUUID().toString()
            val asset = AssetEntity(
                id = assetId,
                name = form.name.trim(),
                category = form.category.trim().ifBlank { null },
                location = form.location.trim().ifBlank { null },
                manufacturer = form.manufacturer.trim().ifBlank { null },
                modelNumber = form.modelNumber.trim().ifBlank { null },
                serialNumber = form.serialNumber.trim().ifBlank { null },
                purchaseDate = form.purchaseDate.takeIf(String::isNotBlank)?.let(LocalDate::parse),
                retailer = form.retailer.trim().ifBlank { null },
                warrantyExpirationDate = form.warrantyExpirationDate.takeIf(String::isNotBlank)?.let(LocalDate::parse),
                notes = form.notes.trim().ifBlank { null },
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            )
            runCatching { repository.upsertAsset(asset) }
                .onFailure { mutableState.value = form.copy(isSaving = false, saveError = true); return@launch }
            var photoFailed = false
            form.pendingPhotoUri?.let { uriValue ->
                runCatching {
                    repository.attachmentsNow(assetId)
                        .filter { it.type == AttachmentType.ASSET_PHOTO }
                        .forEach { old -> repository.deleteAttachment(old.id); attachmentStore.delete(old.localPath) }
                    val photo = attachmentStore.importFromUri(assetId, AttachmentType.ASSET_PHOTO, uriValue.toUri())
                    repository.upsertAttachment(photo)
                }.onFailure { photoFailed = true }
            }
            form.pendingPhotoTempPath?.let { File(it).delete() }
            mutableEvents.emit(AssetSavedEvent(assetId, photoFailed))
        }
    }

    private fun load(asset: AssetEntity) = update(
        AssetFormState(
            assetId = asset.id,
            name = asset.name,
            category = asset.category.orEmpty(),
            location = asset.location.orEmpty(),
            manufacturer = asset.manufacturer.orEmpty(),
            modelNumber = asset.modelNumber.orEmpty(),
            serialNumber = asset.serialNumber.orEmpty(),
            purchaseDate = asset.purchaseDate?.toString().orEmpty(),
            retailer = asset.retailer.orEmpty(),
            warrantyExpirationDate = asset.warrantyExpirationDate?.toString().orEmpty(),
            notes = asset.notes.orEmpty(),
        ),
    )

    private fun restoredState() = AssetFormState(
        assetId = routeAssetId,
        name = savedState["form_name"] ?: "",
        category = savedState["form_category"] ?: "",
        location = savedState["form_location"] ?: "",
        manufacturer = savedState["form_manufacturer"] ?: "",
        modelNumber = savedState["form_model"] ?: "",
        serialNumber = savedState["form_serial"] ?: "",
        purchaseDate = savedState["form_purchase"] ?: "",
        retailer = savedState["form_retailer"] ?: "",
        warrantyExpirationDate = savedState["form_warranty"] ?: "",
        notes = savedState["form_notes"] ?: "",
        pendingPhotoUri = savedState["form_photo_uri"],
        pendingPhotoTempPath = savedState["form_photo_temp"],
    )

    private fun persist(value: AssetFormState) {
        savedState["form_name"] = value.name
        savedState["form_category"] = value.category
        savedState["form_location"] = value.location
        savedState["form_manufacturer"] = value.manufacturer
        savedState["form_model"] = value.modelNumber
        savedState["form_serial"] = value.serialNumber
        savedState["form_purchase"] = value.purchaseDate
        savedState["form_retailer"] = value.retailer
        savedState["form_warranty"] = value.warrantyExpirationDate
        savedState["form_notes"] = value.notes
        savedState["form_photo_uri"] = value.pendingPhotoUri
        savedState["form_photo_temp"] = value.pendingPhotoTempPath
    }
}
