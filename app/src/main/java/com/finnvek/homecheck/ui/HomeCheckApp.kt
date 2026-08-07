package com.finnvek.homecheck.ui

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.finnvek.homecheck.R
import com.finnvek.homecheck.NOTIFICATION_TARGET_ASSET_PREFIX
import com.finnvek.homecheck.NOTIFICATION_TARGET_MAINTENANCE
import com.finnvek.homecheck.billing.BillingEvent
import com.finnvek.homecheck.data.local.entity.AttachmentEntity
import com.finnvek.homecheck.data.local.entity.AttachmentType
import com.finnvek.homecheck.ui.assetdetail.AssetDetailEvent
import com.finnvek.homecheck.ui.assetdetail.AssetDetailScreen
import com.finnvek.homecheck.ui.assetdetail.AssetDetailViewModel
import com.finnvek.homecheck.ui.assets.AssetFormScreen
import com.finnvek.homecheck.ui.assets.AssetFormViewModel
import com.finnvek.homecheck.ui.assets.AssetsScreen
import com.finnvek.homecheck.ui.assets.AssetsViewModel
import com.finnvek.homecheck.ui.home.HomeScreen
import com.finnvek.homecheck.ui.home.HomeViewModel
import com.finnvek.homecheck.ui.maintenance.MaintenanceEvent
import com.finnvek.homecheck.ui.maintenance.MaintenanceFormScreen
import com.finnvek.homecheck.ui.maintenance.MaintenanceFormViewModel
import com.finnvek.homecheck.ui.maintenance.MaintenanceScreen
import com.finnvek.homecheck.ui.maintenance.MaintenanceViewModel
import com.finnvek.homecheck.ui.onboarding.OnboardingScreen
import com.finnvek.homecheck.ui.premium.PremiumSheet
import com.finnvek.homecheck.ui.settings.SettingsEvent
import com.finnvek.homecheck.ui.settings.SettingsScreen
import com.finnvek.homecheck.ui.settings.SettingsViewModel
import com.finnvek.homecheck.ui.theme.HomeCheckTheme
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.launch

private object Routes {
    const val HOME = "home"
    const val ASSETS = "assets"
    const val MAINTENANCE = "maintenance"
    const val SETTINGS = "settings"
    const val NEW_ASSET = "asset/new"
    const val ASSET = "asset/{assetId}"
    const val EDIT_ASSET = "asset/{assetId}/edit"
    const val NEW_MAINTENANCE = "maintenance/new?assetId={assetId}"
    const val EDIT_MAINTENANCE = "maintenance/{taskId}/edit"

    fun asset(id: String) = "asset/$id"
    fun editAsset(id: String) = "asset/$id/edit"
    fun newMaintenance(assetId: String? = null) = if (assetId == null) "maintenance/new" else "maintenance/new?assetId=$assetId"
    fun editMaintenance(taskId: String) = "maintenance/$taskId/edit"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeCheckApp(
    notificationTarget: String? = null,
    onNotificationHandled: () -> Unit = {},
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val preferences by mainViewModel.preferences.collectAsStateWithLifecycle()
    val billing by mainViewModel.billing.collectAsStateWithLifecycle()
    val showPremium by mainViewModel.showPremium.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val activity = context.findActivity()
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }

    HomeCheckTheme(
        themeMode = preferences?.themeMode ?: com.finnvek.homecheck.data.preferences.ThemeMode.SYSTEM,
        dynamicColor = preferences?.dynamicColor ?: false,
    ) {
        val loaded = preferences
        if (loaded == null) {
            Surface(modifier = Modifier) {}
        } else if (!loaded.onboardingComplete) {
            OnboardingScreen(onGetStarted = mainViewModel::completeOnboarding)
        } else {
            AppNavigation(navController, mainViewModel, snackbar)
        }
        if (showPremium) {
            ModalBottomSheet(onDismissRequest = mainViewModel::dismissPremium) {
                PremiumSheet(
                    state = billing,
                    onPurchase = { activity?.let(mainViewModel::launchPurchase) },
                    onRestore = mainViewModel::restorePurchase,
                    onDismiss = mainViewModel::dismissPremium,
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        mainViewModel.events.collect { event ->
            if (event == AppEvent.OPEN_NEW_ASSET) navController.navigate(Routes.NEW_ASSET)
        }
    }
    LaunchedEffect(notificationTarget, preferences?.onboardingComplete) {
        if (notificationTarget != null && preferences?.onboardingComplete == true) {
            when {
                notificationTarget == NOTIFICATION_TARGET_MAINTENANCE -> navController.navigatePrimary(Routes.MAINTENANCE)
                notificationTarget.startsWith(NOTIFICATION_TARGET_ASSET_PREFIX) -> {
                    val assetId = notificationTarget.removePrefix(NOTIFICATION_TARGET_ASSET_PREFIX)
                    if (assetId.isNotBlank()) navController.navigate(Routes.asset(assetId))
                }
            }
            onNotificationHandled()
        }
    }
    LaunchedEffect(Unit) {
        mainViewModel.billingEvents.collect { event ->
            val message = when (event) {
                BillingEvent.PURCHASED, BillingEvent.ALREADY_OWNED -> R.string.purchase_completed
                BillingEvent.PENDING -> R.string.purchase_pending
                BillingEvent.CANCELLED -> R.string.purchase_cancelled
                BillingEvent.NOT_FOUND -> R.string.purchase_not_found
                BillingEvent.UNAVAILABLE -> R.string.billing_unavailable
                BillingEvent.FAILED -> R.string.purchase_failed
            }
            if (event == BillingEvent.PURCHASED || event == BillingEvent.ALREADY_OWNED) mainViewModel.dismissPremium()
            snackbar.showSnackbar(resources.getString(message))
        }
    }
}

@Composable
private fun AppNavigation(
    navController: NavHostController,
    mainViewModel: MainViewModel,
    snackbar: SnackbarHostState,
) {
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val primary = route in setOf(Routes.HOME, Routes.ASSETS, Routes.MAINTENANCE)
    Scaffold(
        bottomBar = { if (primary) BottomNavigation(route, navController) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.HOME) {
                val viewModel: HomeViewModel = hiltViewModel()
                val state by viewModel.state.collectAsStateWithLifecycle()
                HomeScreen(
                    state,
                    onSettings = { navController.navigate(Routes.SETTINGS) },
                    onAddAsset = mainViewModel::requestNewAsset,
                    onTask = { navController.navigate(Routes.editMaintenance(it)) },
                    onSeeAllMaintenance = { navController.navigatePrimary(Routes.MAINTENANCE) },
                    onAsset = { navController.navigate(Routes.asset(it)) },
                )
            }
            composable(Routes.ASSETS) {
                val viewModel: AssetsViewModel = hiltViewModel()
                val state by viewModel.state.collectAsStateWithLifecycle()
                AssetsScreen(
                    state,
                    viewModel.attachmentStore,
                    viewModel::setQuery,
                    viewModel::setNeedsAttention,
                    viewModel::setSort,
                    mainViewModel::requestNewAsset,
                    onAsset = { navController.navigate(Routes.asset(it)) },
                )
            }
            composable(Routes.MAINTENANCE) {
                MaintenanceRoute(navController, snackbar, mainViewModel)
            }
            composable(Routes.SETTINGS) {
                SettingsRoute(navController, snackbar, mainViewModel)
            }
            composable(Routes.NEW_ASSET) {
                AssetFormRoute(navController, snackbar, isEditing = false)
            }
            composable(Routes.ASSET, arguments = listOf(navArgument("assetId") { type = NavType.StringType })) {
                AssetDetailRoute(navController, snackbar)
            }
            composable(Routes.EDIT_ASSET, arguments = listOf(navArgument("assetId") { type = NavType.StringType })) {
                AssetFormRoute(navController, snackbar, isEditing = true)
            }
            composable(
                Routes.NEW_MAINTENANCE,
                arguments = listOf(navArgument("assetId") { type = NavType.StringType; defaultValue = "" }),
            ) {
                MaintenanceFormRoute(navController)
            }
            composable(
                Routes.EDIT_MAINTENANCE,
                arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
            ) {
                MaintenanceFormRoute(navController)
            }
        }
    }
}

@Composable
private fun BottomNavigation(route: String?, navController: NavHostController) {
    val items = listOf(
        Triple(Routes.HOME, R.string.home, Icons.Default.Home),
        Triple(Routes.ASSETS, R.string.assets, Icons.Default.Search),
        Triple(Routes.MAINTENANCE, R.string.maintenance, Icons.Default.Check),
    )
    NavigationBar {
        items.forEach { (destination, label, icon) ->
            NavigationBarItem(
                selected = route == destination,
                onClick = { navController.navigatePrimary(destination) },
                icon = { Icon(icon, null) },
                label = { Text(stringResource(label)) },
            )
        }
    }
}

private fun NavHostController.navigatePrimary(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun MaintenanceRoute(
    navController: NavHostController,
    snackbar: SnackbarHostState,
    mainViewModel: MainViewModel,
    viewModel: MaintenanceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    MaintenanceScreen(
        state,
        viewModel::showHistory,
        onAddMaintenance = { navController.navigate(Routes.newMaintenance()) },
        onAddAsset = mainViewModel::requestNewAsset,
        onTask = { navController.navigate(Routes.editMaintenance(it)) },
        onComplete = viewModel::complete,
    )
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is MaintenanceEvent.Completed -> {
                    val result = snackbar.showSnackbar(resources.getString(R.string.maintenance_completed), resources.getString(R.string.undo))
                    if (result == SnackbarResult.ActionPerformed) viewModel.undo(event.result)
                }
                MaintenanceEvent.Failed -> snackbar.showSnackbar(resources.getString(R.string.maintenance_completion_failed))
            }
        }
    }
}

@Composable
private fun AssetFormRoute(
    navController: NavHostController,
    snackbar: SnackbarHostState,
    isEditing: Boolean,
    viewModel: AssetFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    var cameraFile by remember { mutableStateOf<File?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.setPhoto(it) }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = cameraFile
        if (success && file != null) viewModel.setPhoto(viewModel.cameraUri(file), file) else file?.delete()
    }
    AssetFormScreen(
        state,
        viewModel::update,
        viewModel::save,
        onBack = navController::popBackStack,
        onChoosePhoto = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onTakePhoto = {
            viewModel.createCameraFile().also { file -> cameraFile = file; camera.launch(viewModel.cameraUri(file)) }
        },
        onPickPurchaseDate = { showDatePicker(context, state.purchaseDate) { viewModel.update(state.copy(purchaseDate = it)) } },
        onPickWarrantyDate = { showDatePicker(context, state.warrantyExpirationDate) { viewModel.update(state.copy(warrantyExpirationDate = it)) } },
    )
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (isEditing) navController.popBackStack() else {
                navController.navigate(Routes.asset(event.assetId)) { popUpTo(Routes.NEW_ASSET) { inclusive = true } }
            }
            if (event.photoImportFailed) snackbar.showSnackbar(resources.getString(R.string.photo_import_failed))
        }
    }
}

@Composable
private fun AssetDetailRoute(
    navController: NavHostController,
    snackbar: SnackbarHostState,
    viewModel: AssetDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    var documentType by remember { mutableStateOf(AttachmentType.OTHER) }
    var preview by remember { mutableStateOf<AttachmentEntity?>(null) }
    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importDocument(it, documentType) }
    }
    fun open(attachment: AttachmentEntity) {
        val file = viewModel.attachmentStore.fileFor(attachment.localPath)
        if (!file.isFile) {
            scope.launch { snackbar.showSnackbar(resources.getString(R.string.file_missing)) }
        } else if (attachment.mimeType.startsWith("image/")) {
            preview = attachment
        } else {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW).setDataAndType(viewModel.attachmentStore.uriFor(attachment.localPath), attachment.mimeType)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                )
            } catch (_: ActivityNotFoundException) {
                scope.launch { snackbar.showSnackbar(resources.getString(R.string.no_app_for_document)) }
            }
        }
    }
    fun share(attachment: AttachmentEntity) {
        val file = viewModel.attachmentStore.fileFor(attachment.localPath)
        if (!file.isFile) {
            scope.launch { snackbar.showSnackbar(resources.getString(R.string.file_missing)) }
            return
        }
        val intent = Intent(Intent.ACTION_SEND)
            .setType(attachment.mimeType)
            .putExtra(Intent.EXTRA_STREAM, viewModel.attachmentStore.uriFor(attachment.localPath))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(intent, resources.getString(R.string.share)))
    }
    AssetDetailScreen(
        state,
        viewModel.attachmentStore,
        navController::popBackStack,
        onEdit = { navController.navigate(Routes.editAsset(viewModel.assetId)) },
        viewModel::deleteAsset,
        onAddMaintenance = { navController.navigate(Routes.newMaintenance(viewModel.assetId)) },
        onEditTask = { navController.navigate(Routes.editMaintenance(it)) },
        onDeleteTask = viewModel::deleteTask,
        onAddDocument = { type -> documentType = type; documentPicker.launch(arrayOf("application/pdf", "image/*")) },
        onOpenAttachment = ::open,
        onShareAttachment = ::share,
        onRenameAttachment = viewModel::renameAttachment,
        onChangeAttachmentType = viewModel::changeType,
        onDeleteAttachment = viewModel::deleteAttachment,
    )
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                AssetDetailEvent.ASSET_DELETED -> navController.popBackStack()
                AssetDetailEvent.DOCUMENT_ADDED -> snackbar.showSnackbar(resources.getString(R.string.document_added))
                AssetDetailEvent.OPERATION_FAILED -> snackbar.showSnackbar(resources.getString(R.string.operation_failed))
            }
        }
    }
    preview?.let { attachment ->
        AlertDialog(
            onDismissRequest = { preview = null },
            text = {
                com.finnvek.homecheck.ui.components.LocalImage(
                    file = viewModel.attachmentStore.fileFor(attachment.localPath),
                    contentDescription = attachment.displayName,
                )
            },
            confirmButton = { TextButton(onClick = { preview = null }) { Text(stringResource(R.string.close)) } },
        )
    }
}

@Composable
private fun MaintenanceFormRoute(
    navController: NavHostController,
    viewModel: MaintenanceFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val assets by viewModel.assets.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    MaintenanceFormScreen(
        state,
        assets,
        viewModel::update,
        onPickDueDate = { showDatePicker(context, state.dueDate) { viewModel.update(state.copy(dueDate = it, dueDateError = false)) } },
        onSave = {
            if (state.reminderEnabled && Build.VERSION.SDK_INT >= 33 && !notificationsGranted(context)) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            viewModel.save()
        },
        onBack = navController::popBackStack,
    )
    LaunchedEffect(viewModel) { viewModel.saved.collect { navController.popBackStack() } }
}

@Composable
private fun SettingsRoute(
    navController: NavHostController,
    snackbar: SnackbarHostState,
    mainViewModel: MainViewModel,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val baseState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val backup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri -> uri?.let(viewModel::backup) }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> restoreUri = uri }
    SettingsScreen(
        state = baseState.copy(notificationsGranted = notificationsGranted(context)),
        onBack = navController::popBackStack,
        onTheme = viewModel::setTheme,
        onDynamicColor = viewModel::setDynamicColor,
        onReminders = { enabled ->
            if (enabled && Build.VERSION.SDK_INT >= 33 && !notificationsGranted(context)) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            viewModel.setReminders(enabled)
        },
        onReminderTime = {
            val preferences = baseState.preferences
            TimePickerDialog(context, { _, hour, minute -> viewModel.setReminderTime(hour, minute) }, preferences.reminderHour, preferences.reminderMinute, false).show()
        },
        onRequestNotificationPermission = { if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS) },
        onBackup = { backup.launch("homecheck-backup-${LocalDate.now()}.homecheck") },
        onRestoreBackup = { restore.launch(arrayOf("application/zip", "application/octet-stream")) },
        onUnlockPremium = mainViewModel::openPremium,
        onRestorePurchase = viewModel::restorePurchase,
    )
    restoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { restoreUri = null },
            title = { Text(stringResource(R.string.restore_backup_question)) },
            text = { Text(stringResource(R.string.restore_backup_warning)) },
            confirmButton = { TextButton(onClick = { restoreUri = null; viewModel.restore(uri) }) { Text(stringResource(R.string.restore)) } },
            dismissButton = { TextButton(onClick = { restoreUri = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            snackbar.showSnackbar(resources.getString(when (event) {
                SettingsEvent.BACKUP_SUCCEEDED -> R.string.backup_succeeded
                SettingsEvent.BACKUP_FAILED -> R.string.backup_failed
                SettingsEvent.RESTORE_SUCCEEDED -> R.string.restore_succeeded
                SettingsEvent.RESTORE_FAILED -> R.string.restore_failed
            }))
        }
    }
}

private fun showDatePicker(context: Context, current: String, onDate: (String) -> Unit) {
    val initial = runCatching { LocalDate.parse(current) }.getOrDefault(LocalDate.now())
    DatePickerDialog(
        context,
        { _, year, month, day -> onDate(LocalDate.of(year, month + 1, day).toString()) },
        initial.year,
        initial.monthValue - 1,
        initial.dayOfMonth,
    ).show()
}

private fun notificationsGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
