package com.spacepilot.free.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spacepilot.free.R
import com.spacepilot.free.data.model.BrowserSort
import com.spacepilot.free.data.model.BrowserState
import com.spacepilot.free.data.model.FileCategory
import com.spacepilot.free.data.model.FileEntry
import com.spacepilot.free.ui.components.BatchRenameDialog
import com.spacepilot.free.ui.components.CreateFolderDialog
import com.spacepilot.free.ui.components.DeleteConfirmationDialog
import com.spacepilot.free.ui.components.EmptyState
import com.spacepilot.free.ui.components.FileListRow
import com.spacepilot.free.ui.components.RenameDialog
import com.spacepilot.free.ui.components.SpaceCard
import java.util.Locale

@Composable
fun FileManagerScreen(
    browser: BrowserState,
    showBottomBar: Boolean,
    onBack: () -> Unit,
    onPickFolder: () -> Unit,
    onDisconnectFolder: () -> Unit,
    onLoadRoot: () -> Unit,
    onOpenDirectory: (FileEntry) -> Unit,
    onGoUp: () -> Unit,
    onRefresh: () -> Unit,
    onOpenFile: (FileEntry) -> Unit,
    onInfo: (FileEntry) -> Unit,
    onCopy: (FileEntry, Boolean) -> Unit,
    onDelete: (List<FileEntry>) -> Unit,
    onRename: (FileEntry, String) -> Unit,
    onCreateFolder: (String) -> Unit,
    onBatchRename: (List<FileEntry>, String) -> Unit,
    onZip: (List<FileEntry>) -> Unit,
    bottomBar: @Composable () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(BrowserSort.NAME) }
    var filter by remember { mutableStateOf<FileCategory?>(null) }
    var sortMenu by remember { mutableStateOf(false) }
    var filterMenu by remember { mutableStateOf(false) }
    var topMenu by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var renameTarget by remember { mutableStateOf<FileEntry?>(null) }
    var createFolderOpen by remember { mutableStateOf(false) }
    var batchRenameOpen by remember { mutableStateOf(false) }
    var deleteTargets by remember { mutableStateOf<List<FileEntry>?>(null) }
    val focusManager = LocalFocusManager.current

    val filtered = remember(browser.items, query, filter, sort) {
        browser.items
            .filter { item ->
                val matchesQuery = query.isBlank() || item.name.contains(query, ignoreCase = true)
                val matchesType = item.isDirectory || filter == null || item.category == filter
                matchesQuery && matchesType
            }
            .sortedWith(
                compareByDescending<FileEntry> { it.isDirectory }.then(
                    when (sort) {
                        BrowserSort.NAME -> compareBy { it.name.lowercase(Locale.getDefault()) }
                        BrowserSort.SIZE -> compareByDescending { it.size }
                        BrowserSort.DATE -> compareByDescending { it.modified }
                    }
                )
            )
    }
    val selectedFiles = remember(selected, filtered) { filtered.filter { it.uri in selected } }
    val toggleSelected: (FileEntry) -> Unit = { item ->
        selected = if (item.uri in selected) selected - item.uri else selected + item.uri
    }
    val exitSelection = {
        selectionMode = false
        selected = emptySet()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    if (selectionMode && selectedFiles.isNotEmpty()) {
                        Text(
                            pluralStringResource(
                                R.plurals.files_selected,
                                selectedFiles.size,
                                selectedFiles.size
                            )
                        )
                    } else {
                        Text(stringResource(R.string.browser_title))
                    }
                },
                navigationIcon = {
                    if (selectionMode) {
                        IconButton(onClick = exitSelection) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.action_clear_selection)
                            )
                        }
                    } else if (!showBottomBar) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.action_back)
                            )
                        }
                    }
                },
                actions = {
                    if (selectionMode) {
                        IconButton(
                            onClick = { selected = filtered.map { it.uri }.toSet() },
                            enabled = filtered.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Outlined.Checklist,
                                contentDescription = stringResource(R.string.action_select_all)
                            )
                        }
                        IconButton(onClick = exitSelection) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.action_clear_selection)
                            )
                        }
                    } else {
                        IconButton(onClick = onRefresh, enabled = browser.rootUri != null) {
                            Icon(
                                Icons.Outlined.Refresh,
                                contentDescription = stringResource(R.string.action_refresh)
                            )
                        }
                        Box {
                            IconButton(onClick = { topMenu = true }) {
                                Icon(
                                    Icons.Outlined.MoreVert,
                                    contentDescription = null
                                )
                            }
                            DropdownMenu(expanded = topMenu, onDismissRequest = { topMenu = false }) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(
                                                if (browser.rootUri == null) R.string.browser_pick_folder
                                                else R.string.browser_change_folder
                                            )
                                        )
                                    },
                                    onClick = {
                                        topMenu = false
                                        onPickFolder()
                                    }
                                )
                                if (browser.rootUri != null) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_create_folder)) },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.CreateNewFolder, contentDescription = null)
                                        },
                                        onClick = {
                                            topMenu = false
                                            createFolderOpen = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_select)) },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.Checklist, contentDescription = null)
                                        },
                                        onClick = {
                                            topMenu = false
                                            selectionMode = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.browser_disconnect)) },
                                        onClick = {
                                            topMenu = false
                                            onDisconnectFolder()
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Column {
                AnimatedVisibility(
                    visible = selectionMode && selectedFiles.isNotEmpty(),
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    SelectionActionBar(
                        selected = selectedFiles,
                        onBatchRename = { batchRenameOpen = true },
                        onZip = { onZip(selectedFiles.filterNot { file -> file.isDirectory }) },
                        onDelete = { deleteTargets = selectedFiles },
                        onClear = { selected = emptySet() }
                    )
                }
                bottomBar()
            }
        }
    ) { innerPadding ->
        if (browser.rootUri == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                EmptyState(
                    icon = Icons.Outlined.FolderOpen,
                    title = stringResource(R.string.browser_no_access_title),
                    message = stringResource(R.string.browser_no_access_message),
                    action = {
                        Button(onClick = onPickFolder) {
                            Text(stringResource(R.string.browser_pick_folder))
                        }
                    }
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (browser.canGoUp) {
                            IconButton(onClick = onGoUp) {
                                Icon(
                                    Icons.Outlined.ArrowUpward,
                                    contentDescription = stringResource(R.string.browser_up)
                                )
                            }
                        } else {
                            Icon(
                                Icons.Outlined.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp).size(20.dp)
                            )
                        }
                        Text(
                            text = browser.breadcrumb,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.browser_items, filtered.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.browser_search_hint)) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Outlined.Clear, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box {
                            TextButton(onClick = { sortMenu = true }) {
                                Icon(Icons.Outlined.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    stringResource(
                                        when (sort) {
                                            BrowserSort.NAME -> R.string.large_sort_name
                                            BrowserSort.SIZE -> R.string.large_sort_size
                                            BrowserSort.DATE -> R.string.large_sort_date
                                        }
                                    )
                                )
                            }
                            DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                                listOf(
                                    BrowserSort.NAME to R.string.large_sort_name,
                                    BrowserSort.SIZE to R.string.large_sort_size,
                                    BrowserSort.DATE to R.string.large_sort_date
                                ).forEach { (mode, label) ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(label)) },
                                        onClick = {
                                            sort = mode
                                            sortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        Box {
                            TextButton(onClick = { filterMenu = true }) {
                                Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    stringResource(
                                        when (filter) {
                                            null -> R.string.filter_all
                                            FileCategory.IMAGES -> R.string.filter_images
                                            FileCategory.VIDEO -> R.string.filter_video
                                            FileCategory.AUDIO -> R.string.filter_audio
                                            FileCategory.DOCUMENTS -> R.string.filter_documents
                                            FileCategory.ARCHIVES -> R.string.filter_archives
                                            FileCategory.APK -> R.string.filter_apk
                                            FileCategory.OTHER -> R.string.filter_other
                                        }
                                    )
                                )
                            }
                            DropdownMenu(expanded = filterMenu, onDismissRequest = { filterMenu = false }) {
                                val options = listOf(
                                    null to R.string.filter_all,
                                    FileCategory.IMAGES to R.string.filter_images,
                                    FileCategory.VIDEO to R.string.filter_video,
                                    FileCategory.AUDIO to R.string.filter_audio,
                                    FileCategory.DOCUMENTS to R.string.filter_documents,
                                    FileCategory.ARCHIVES to R.string.filter_archives,
                                    FileCategory.APK to R.string.filter_apk,
                                    FileCategory.OTHER to R.string.filter_other
                                )
                                options.forEach { (category, label) ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(label)) },
                                        onClick = {
                                            filter = category
                                            filterMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                when {
                    browser.loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    browser.error != null -> {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            SpaceCard {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text(
                                        text = stringResource(R.string.browser_error),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = browser.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = onLoadRoot) {
                                            Text(stringResource(R.string.action_retry))
                                        }
                                        TextButton(onClick = onPickFolder) {
                                            Text(stringResource(R.string.browser_change_folder))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    filtered.isEmpty() -> {
                        EmptyState(
                            icon = Icons.Outlined.FolderOpen,
                            title = stringResource(R.string.browser_empty_folder),
                            message = stringResource(R.string.browser_items, 0),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filtered, key = { it.uri }) { item ->
                                BrowserRow(
                                    item = item,
                                    selectionMode = selectionMode,
                                    selected = item.uri in selected,
                                    onClick = {
                                        if (selectionMode) {
                                            toggleSelected(item)
                                        } else if (item.isDirectory) {
                                            onOpenDirectory(item)
                                        } else {
                                            onOpenFile(item)
                                        }
                                    },
                                    onSelectedChange = { checked ->
                                        selected = if (checked) selected + item.uri else selected - item.uri
                                    },
                                    onInfo = { onInfo(item) },
                                    onRename = { renameTarget = item },
                                    onSelect = {
                                        selectionMode = true
                                        selected = selected + item.uri
                                    },
                                    onCopy = { onCopy(item, false) },
                                    onMove = { onCopy(item, true) },
                                    onDelete = { deleteTargets = listOf(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    renameTarget?.let { target ->
        RenameDialog(
            initialName = target.name,
            onConfirm = { newName ->
                onRename(target, newName)
                renameTarget = null
            },
            onDismiss = { renameTarget = null }
        )
    }

    if (createFolderOpen) {
        CreateFolderDialog(
            onConfirm = { name ->
                onCreateFolder(name)
                createFolderOpen = false
            },
            onDismiss = { createFolderOpen = false }
        )
    }

    if (batchRenameOpen) {
        BatchRenameDialog(
            fileNames = selectedFiles.map { it.name },
            onConfirm = { pattern ->
                onBatchRename(selectedFiles, pattern)
                batchRenameOpen = false
                exitSelection()
            },
            onDismiss = { batchRenameOpen = false }
        )
    }

    deleteTargets?.let { targets ->
        DeleteConfirmationDialog(
            files = targets,
            onConfirm = {
                onDelete(targets)
                deleteTargets = null
                exitSelection()
            },
            onDismiss = { deleteTargets = null }
        )
    }
}

@Composable
private fun SelectionActionBar(
    selected: List<FileEntry>,
    onBatchRename: () -> Unit,
    onZip: () -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit
) {
    val hasFiles = selected.any { !it.isDirectory }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = pluralStringResource(R.plurals.files_selected, selected.size, selected.size),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        TextButton(onClick = onBatchRename) {
            Icon(Icons.Outlined.DriveFileRenameOutline, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.action_batch_rename))
        }
        TextButton(onClick = onZip, enabled = hasFiles) {
            Icon(Icons.Outlined.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.action_zip))
        }
        TextButton(onClick = onDelete) {
            Icon(
                Icons.Outlined.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
        }
        TextButton(onClick = onClear) {
            Text(stringResource(R.string.action_clear_selection))
        }
    }
}

@Composable
private fun BrowserRow(
    item: FileEntry,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onSelectedChange: (Boolean) -> Unit,
    onInfo: () -> Unit,
    onRename: () -> Unit,
    onSelect: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    if (selectionMode) {
        FileListRow(
            file = item,
            onClick = onClick,
            selected = selected,
            onSelectedChange = onSelectedChange,
            showPath = false
        )
        return
    }
    Box {
        FileListRow(
            file = item,
            onClick = onClick,
            onMore = { menuOpen = true },
            showPath = false
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_info)) },
                leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    onInfo()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_rename)) },
                leadingIcon = {
                    Icon(Icons.Outlined.DriveFileRenameOutline, contentDescription = null)
                },
                onClick = {
                    menuOpen = false
                    onRename()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_select)) },
                leadingIcon = { Icon(Icons.Outlined.Checklist, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    onSelect()
                }
            )
            if (!item.isDirectory) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_copy)) },
                    leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onCopy()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_move)) },
                    leadingIcon = { Icon(Icons.Outlined.FolderOpen, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onMove()
                    }
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete)) },
                leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    onDelete()
                }
            )
        }
    }
}
