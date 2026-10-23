package com.spacepilot.free.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.spacepilot.free.BuildConfig
import com.spacepilot.free.R
import com.spacepilot.free.data.model.FileCategory
import com.spacepilot.free.data.model.FileEntry
import com.spacepilot.free.ui.components.ConfirmActionDialog
import com.spacepilot.free.ui.components.FileAnalysisDialog
import com.spacepilot.free.ui.components.PermissionRationaleDialog
import com.spacepilot.free.ui.screens.ApkScreen
import com.spacepilot.free.ui.screens.DuplicatesScreen
import com.spacepilot.free.ui.screens.FileManagerScreen
import com.spacepilot.free.ui.screens.HomeScreen
import com.spacepilot.free.ui.screens.LargeFilesScreen
import com.spacepilot.free.ui.screens.ResultsScreen
import com.spacepilot.free.ui.screens.ScanScreen
import com.spacepilot.free.ui.screens.SettingsScreen
import com.spacepilot.free.ui.theme.SpacePilotTheme
import com.spacepilot.free.util.IntentUtils
import com.spacepilot.free.util.PermissionUtils
import com.spacepilot.free.viewmodel.AppViewModel
import com.spacepilot.free.viewmodel.UiEvent
import kotlinx.coroutines.launch

private object Routes {
    const val HOME = "home"
    const val SCAN = "scan"
    const val DUPLICATES = "duplicates"
    const val FILES = "files"
    const val SETTINGS = "settings"
    const val LARGE = "large"
    const val RESULTS = "results"
    const val APK = "apk"
}

private data class TabItem(
    val route: String,
    @StringRes val label: Int,
    val icon: ImageVector
)

private val tabs = listOf(
    TabItem(Routes.HOME, R.string.nav_home, Icons.Outlined.Home),
    TabItem(Routes.FILES, R.string.nav_files, Icons.Outlined.Folder),
    TabItem(Routes.SCAN, R.string.nav_scan, Icons.Outlined.Radar),
    TabItem(Routes.DUPLICATES, R.string.nav_duplicates, Icons.Outlined.ContentCopy),
    TabItem(Routes.SETTINGS, R.string.nav_settings, Icons.Outlined.Settings)
)

@Composable
fun SpacePilotApp(viewModel: AppViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val folderUri by viewModel.folderUri.collectAsStateWithLifecycle()
    val operations by viewModel.recentOperations.collectAsStateWithLifecycle()
    val storageInfo by viewModel.storageInfo.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val duplicates by viewModel.duplicates.collectAsStateWithLifecycle()
    val browser by viewModel.browser.collectAsStateWithLifecycle()
    val analysis by viewModel.analysis.collectAsStateWithLifecycle()
    val copyPending by viewModel.copyPending.collectAsStateWithLifecycle()
    val allFiles by viewModel.allFiles.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    var showPermissionDialog by remember { mutableStateOf(false) }
    var pendingInstall by remember { mutableStateOf<FileEntry?>(null) }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                viewModel.setFolderAccess(uri)
                snackbarHostState.showSnackbar(context.getString(R.string.msg_folder_connected))
            }
        }
    }

    val copyDestinationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.onCopyDestinationSelected(uri)
        } else {
            viewModel.cancelCopy()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.startScan()
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onSystemDeleteResult(result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val message = when (event) {
                is UiEvent.Deleted -> if (event.failed > 0) {
                    if (event.count > 0) {
                        context.getString(R.string.msg_delete_failed, event.count, event.failed)
                    } else {
                        context.getString(R.string.delete_failed, event.failed)
                    }
                } else {
                    context.getString(R.string.msg_deleted, event.count)
                }
                is UiEvent.DeletePending -> context.getString(R.string.msg_delete_pending, event.count)
                UiEvent.DeleteCancelled -> context.getString(R.string.msg_delete_cancelled)
                is UiEvent.Copied -> context.getString(
                    if (event.moved) R.string.msg_moved else R.string.msg_copied,
                    event.name
                )
                is UiEvent.CopyFailed -> context.getString(
                    R.string.msg_copy_failed,
                    event.reason ?: context.getString(R.string.common_unknown)
                )
                UiEvent.HistoryCleared -> context.getString(R.string.settings_history_cleared)
                is UiEvent.Message -> event.text
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.deleteRequests.collect { sender ->
            deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
        }
    }

    val folderAccessError by viewModel.folderAccessError.collectAsStateWithLifecycle()
    LaunchedEffect(folderAccessError) {
        val message = folderAccessError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeFolderAccessError()
    }

    LaunchedEffect(copyPending) {
        if (copyPending != null) {
            copyDestinationLauncher.launch(null)
        }
    }

    val navigateTab: (String) -> Unit = { route ->
        if (currentRoute != route) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val openFile: (FileEntry) -> Unit = { file ->
        IntentUtils.openFile(context, file)
    }

    val requestScan: (Boolean) -> Unit = { navigate ->
        if (navigate) {
            navController.navigate(Routes.SCAN) {
                launchSingleTop = true
            }
        }
        if (PermissionUtils.hasAllScanPermissions(context)) {
            viewModel.startScan()
        } else {
            showPermissionDialog = true
        }
    }

    val bottomBar: @Composable () -> Unit = {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            tabs.forEach { tab ->
                NavigationBarItem(
                    selected = currentRoute == tab.route,
                    onClick = { navigateTab(tab.route) },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = { Text(stringResource(tab.label)) }
                )
            }
        }
    }

    SpacePilotTheme(themeMode = settings.themeMode) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0),
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        storageInfo = storageInfo,
                        summary = summary,
                        scanState = scanState,
                        folderUri = folderUri,
                        operations = operations,
                        onScan = { requestScan(false) },
                        onLargeFiles = { navController.navigate(Routes.LARGE) },
                        onDuplicates = { navigateTab(Routes.DUPLICATES) },
                        onFiles = { navigateTab(Routes.FILES) },
                        onApks = { navController.navigate(Routes.APK) },
                        onSettings = { navigateTab(Routes.SETTINGS) },
                        onResults = { navController.navigate(Routes.RESULTS) },
                        onPickFolder = { folderLauncher.launch(null) },
                        onRefreshStorage = viewModel::refreshStorage,
                        onViewScan = { navController.navigate(Routes.SCAN) { launchSingleTop = true } },
                        bottomBar = bottomBar
                    )
                }

                composable(Routes.SCAN) {
                    ScanScreen(
                        scanState = scanState,
                        summary = summary,
                        folderConnected = folderUri != null,
                        onScan = { requestScan(false) },
                        onCancel = viewModel::cancelScan,
                        onResults = { navController.navigate(Routes.RESULTS) },
                        onPickFolder = { folderLauncher.launch(null) },
                        bottomBar = bottomBar
                    )
                }

                composable(Routes.DUPLICATES) {
                    DuplicatesScreen(
                        duplicates = duplicates,
                        scannedFileCount = summary?.scannedFiles ?: 0,
                        onBack = { navController.popBackStack() },
                        onAnalyze = viewModel::analyzeDuplicates,
                        onCancelAnalysis = viewModel::cancelDuplicateAnalysis,
                        onScan = { requestScan(true) },
                        onOpenFile = openFile,
                        onInfo = viewModel::analyzeFile,
                        onDelete = viewModel::deleteFiles,
                        showBottomBar = true,
                        bottomBar = bottomBar
                    )
                }

                composable(Routes.FILES) {
                    LaunchedEffect(folderUri) {
                        if (folderUri != null && browser.rootUri == null) {
                            viewModel.loadBrowserRoot()
                        }
                    }
                    FileManagerScreen(
                        browser = browser,
                        showBottomBar = true,
                        onBack = { navController.popBackStack() },
                        onPickFolder = { folderLauncher.launch(null) },
                        onDisconnectFolder = {
                            viewModel.clearFolderAccess()
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.msg_folder_disconnected)
                                )
                            }
                        },
                        onLoadRoot = viewModel::loadBrowserRoot,
                        onOpenDirectory = viewModel::openBrowserDirectory,
                        onGoUp = viewModel::browserGoUp,
                        onRefresh = viewModel::refreshBrowser,
                        onOpenFile = openFile,
                        onInfo = viewModel::analyzeFile,
                        onCopy = viewModel::requestCopy,
                        onDelete = viewModel::deleteFiles,
                        bottomBar = bottomBar
                    )
                }

                composable(Routes.SETTINGS) {
                    LaunchedEffect(folderUri) {
                        if (folderUri != null && browser.rootUri == null) {
                            viewModel.loadBrowserRoot()
                        }
                    }
                    SettingsScreen(
                        settings = settings,
                        folderUri = folderUri,
                        folderName = browser.rootName.ifBlank {
                            folderUri?.substringAfterLast('/')?.substringAfterLast(':').orEmpty()
                        },
                        versionName = BuildConfig.VERSION_NAME,
                        onThemeChange = viewModel::setTheme,
                        onLanguageChange = { mode ->
                            viewModel.setLanguage(mode)
                            (context as? Activity)?.recreate()
                        },
                        onPickFolder = { folderLauncher.launch(null) },
                        onDisconnectFolder = { viewModel.clearFolderAccess() },
                        onClearHistory = viewModel::clearHistory,
                        onResetSettings = {
                            viewModel.resetSettings()
                            (context as? Activity)?.recreate()
                        },
                        onOpenSystemSettings = { IntentUtils.openAppSettings(context) },
                        bottomBar = bottomBar
                    )
                }

                composable(Routes.LARGE) {
                    LargeFilesScreen(
                        summary = summary,
                        onBack = { navController.popBackStack() },
                        onScan = { requestScan(true) },
                        onOpenFile = openFile,
                        onInfo = viewModel::analyzeFile,
                        onDelete = viewModel::deleteFiles
                    )
                }

                composable(Routes.RESULTS) {
                    ResultsScreen(
                        summary = summary,
                        onBack = { navController.popBackStack() },
                        onScan = { requestScan(true) },
                        onHome = { navigateTab(Routes.HOME) },
                        onLargeFiles = { navController.navigate(Routes.LARGE) },
                        onDuplicates = { navigateTab(Routes.DUPLICATES) },
                        onAnalyzeDuplicates = {
                            viewModel.analyzeDuplicates()
                            navigateTab(Routes.DUPLICATES)
                        },
                        onOpenFile = openFile,
                        onInfo = viewModel::analyzeFile
                    )
                }

                composable(Routes.APK) {
                    ApkScreen(
                        apkFiles = allFiles.filter { it.category == FileCategory.APK },
                        hasScanResults = summary != null,
                        onBack = { navController.popBackStack() },
                        onScan = { requestScan(true) },
                        onInfo = viewModel::analyzeFile,
                        onInstall = { file -> pendingInstall = file }
                    )
                }
            }
        }
    }

    if (showPermissionDialog) {
        PermissionRationaleDialog(
            onConfirm = {
                showPermissionDialog = false
                permissionLauncher.launch(PermissionUtils.scanPermissions())
            },
            onDismiss = { showPermissionDialog = false }
        )
    }

    analysis?.let { state ->
        FileAnalysisDialog(
            state = state,
            onDismiss = viewModel::dismissAnalysis,
            onOpen = { openFile(state.file) },
            onInstall = if (state.file.category == FileCategory.APK) {
                {
                    pendingInstall = state.file
                    viewModel.dismissAnalysis()
                }
            } else {
                null
            }
        )
    }

    pendingInstall?.let { file ->
        ConfirmActionDialog(
            title = stringResource(R.string.apk_title),
            message = stringResource(R.string.apk_safety_note),
            confirmLabel = stringResource(R.string.apk_install),
            onConfirm = {
                if (IntentUtils.installApk(context, file)) {
                    viewModel.recordApkInstall(file)
                }
                pendingInstall = null
            },
            onDismiss = { pendingInstall = null }
        )
    }
}
