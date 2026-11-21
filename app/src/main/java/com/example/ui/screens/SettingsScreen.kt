package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.AccountType
import com.example.data.local.TransactionType
import com.example.domain.AccountModel
import com.example.domain.AutoTrackMode
import com.example.domain.AutoTrackSource
import com.example.domain.CategoryModel
import com.example.domain.DashboardWidgetType
import com.example.domain.ExchangeRateModel
import com.example.domain.MaterialYouPalette
import com.example.domain.ThemeMode
import com.example.ui.components.AccountManagementSectionCard
import com.example.ui.components.AddOrEditAccountDialog
import com.example.ui.components.CategoryManagementSectionCard
import com.example.ui.components.CreateCategoryDialog
import com.example.ui.components.DeleteAccountConfirmationDialog
import com.example.ui.components.colorFromHex
import com.example.ui.viewmodel.FinanceUiState
import com.example.util.BankAndUpiAlertParser

@Composable
fun SettingsScreen(
    uiState: FinanceUiState,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit,
    onSelectSeedPalette: (MaterialYouPalette) -> Unit,
    onSelectBaseCurrency: (String) -> Unit,
    onToggleIncludeBfInNet: (Boolean) -> Unit,
    onToggleWidget: (DashboardWidgetType, Boolean) -> Unit,
    onMoveWidget: (DashboardWidgetType, Boolean) -> Unit,
    onToggleAppLock: (Boolean) -> Unit,
    onUpdatePin: (String) -> Unit,
    onLockVaultNow: () -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onToggleTxAlerts: (Boolean) -> Unit,
    onToggleBudgetAlerts: (Boolean) -> Unit,
    onToggleAutoTrack: (Boolean) -> Unit,
    onSelectAutoTrackMode: (AutoTrackMode) -> Unit,
    onToggleAutoTrackSource: (AutoTrackSource, Boolean) -> Unit = { _, _ -> },
    onSendTestAlert: () -> Unit,
    onUpdateCustomFxRate: (String, Double) -> Unit,
    onResetFxRates: () -> Unit,
    onToggleFxChartExpanded: () -> Unit = {},
    onSaveAccount: (
        id: Long,
        name: String,
        accountType: AccountType,
        currencyCode: String,
        initialBroughtForwardBalance: Double,
        carryOverEnabled: Boolean,
        colorHex: String,
        accountNumber: String
    ) -> Unit,
    onDeleteAccount: (accountId: Long, accountName: String) -> Unit,
    onAddCustomCategory: (name: String, type: TransactionType, iconKey: String, colorHex: String) -> Unit,
    onUpdateCategory: (id: Long, name: String, type: TransactionType, iconKey: String, colorHex: String) -> Unit = { _, n, t, i, c ->
        onAddCustomCategory(n, t, i, c)
    },
    onDeleteCategory: (categoryId: Long, categoryName: String) -> Unit,
    onExportVaultBackup: () -> Unit = {},
    onRestoreLatestBackup: () -> Unit = {},
    onImportBackupJson: (String) -> Unit = {},
    onSelectFontStyle: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isListenerPermissionGranted by remember {
        mutableStateOf(BankAndUpiAlertParser.isNotificationListenerEnabled(context))
    }

    var showConfirmLatestRestoreDialog by remember { mutableStateOf(false) }
    var pendingImportedBackupJson by remember { mutableStateOf<String?>(null) }

    val backupFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            }.getOrNull()?.takeIf { it.isNotBlank() }?.let { rawJson ->
                pendingImportedBackupJson = rawJson
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isListenerPermissionGranted = BankAndUpiAlertParser.isNotificationListenerEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showChangePinDialog by remember { mutableStateOf(false) }
    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryModel?>(null) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountModel?>(null) }
    var deletingAccount by remember { mutableStateOf<AccountModel?>(null) }
    var editingRate by remember { mutableStateOf<ExchangeRateModel?>(null) }
    var showDisableAppLockDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 0. Auto-Track Banking, UPI (Super Money, GPay, PhonePe, etc.) & SMS Alerts Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auto_track_settings_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Auto-Track Banking, UPI & SMS Alerts",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Super Money, Google Pay, PhonePe, Paytm, BHIM, CRED, Amazon Pay, Banking Apps & Bank SMS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = uiState.preferences.autoTrackEnabled,
                            onCheckedChange = onToggleAutoTrack,
                            modifier = Modifier.testTag("auto_track_master_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (uiState.preferences.autoTrackEnabled) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (uiState.preferences.autoTrackEnabled && isListenerPermissionGranted) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = when {
                                            !uiState.preferences.autoTrackEnabled -> "Auto-Tracker Switched OFF"
                                            isListenerPermissionGranted -> "Live Notification Listener Active"
                                            else -> "Notification Access Required for Live Tracking"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "2-Minute Smart Duplicate Filter + Automatic Account Matching active",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.preferences.autoTrackEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "When a Bank SMS or UPI Notification is Detected:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AutoTrackMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = uiState.preferences.autoTrackMode == mode,
                                    onClick = { onSelectAutoTrackMode(mode) },
                                    shape = CircleShape,
                                    label = { Text(mode.label) },
                                    modifier = Modifier.testTag("auto_track_mode_chip_${mode.name}")
                                )
                            }
                        }

                        Text(
                            text = uiState.preferences.autoTrackMode.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Monitored Live UPI & Banking Sources (${uiState.preferences.enabledAutoTrackSources.size}/${AutoTrackSource.entries.size} Active):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AutoTrackSource.entries.forEach { source ->
                                val isSourceEnabled = source in uiState.preferences.enabledAutoTrackSources
                                FilterChip(
                                    selected = isSourceEnabled,
                                    onClick = { onToggleAutoTrackSource(source, !isSourceEnabled) },
                                    shape = CircleShape,
                                    label = { Text(source.label) },
                                    modifier = Modifier.testTag("auto_track_source_chip_${source.name}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { BankAndUpiAlertParser.openNotificationListenerSettings(context) },
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_notification_access_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isListenerPermissionGranted) {
                                    "Manage Android Notification Access (Granted)"
                                } else {
                                    "Grant Android Notification Access"
                                }
                            )
                        }
                    }
                }
            }
        }
        // 1. Material Design 3 (Material You) Theme, Dynamic Wallpaper Color & Tonal Seed Palettes
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("material_you_theme_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Theme & Appearance (Material You)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Material Design 3 dynamic color & tonal seed palettes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = uiState.preferences.themeMode == mode,
                                onClick = { onSelectThemeMode(mode) },
                                shape = CircleShape,
                                label = { Text(mode.label) },
                                modifier = Modifier.testTag("theme_chip_${mode.name}")
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Material You Dynamic Wallpaper Color",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Automatically adapts app colors to your Android 12+ system wallpaper",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = uiState.preferences.dynamicColorEnabled,
                            onCheckedChange = onToggleDynamicColor,
                            modifier = Modifier.testTag("dynamic_color_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Custom Material You Tonal Seed Palettes:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MaterialYouPalette.entries.forEach { palette ->
                            val isSelected = !uiState.preferences.dynamicColorEnabled &&
                                uiState.preferences.seedPalette == palette
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectSeedPalette(palette) },
                                shape = CircleShape,
                                leadingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(colorFromHex(palette.primaryPreviewHex))
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(colorFromHex(palette.containerPreviewHex))
                                        )
                                    }
                                },
                                label = { Text(palette.label) },
                                modifier = Modifier.testTag("seed_palette_chip_${palette.name}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Display Font Style:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Default", "Calibri", "Times Roman", "Serif", "Monospace", "Bold Sans").forEach { fontName ->
                            val isSelected = uiState.preferences.fontStyle == fontName
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectFontStyle(fontName) },
                                shape = CircleShape,
                                label = { Text(fontName) },
                                modifier = Modifier.testTag("font_style_chip_$fontName")
                            )
                        }
                    }
                }
            }
        }

        // 2. Accounts (Add, Edit & Delete) inside Settings Menu
        item {
            AccountManagementSectionCard(
                accounts = uiState.accounts,
                rates = uiState.ratesMap,
                onAddAccountClick = {
                    editingAccount = null
                    showAddAccountDialog = true
                },
                onEditAccountClick = { acc -> editingAccount = acc },
                onDeleteAccountClick = { acc -> deletingAccount = acc }
            )
        }

        // 3. Categories (Add, Edit & Delete) inside Settings Menu
        item {
            CategoryManagementSectionCard(
                categories = uiState.categories,
                onAddCategoryClick = {
                    editingCategory = null
                    showCreateCategoryDialog = true
                },
                onEditCategoryClick = { cat ->
                    editingCategory = cat
                    showCreateCategoryDialog = true
                },
                onDeleteCategory = onDeleteCategory
            )
        }

        // 4. Dashboard Widgets Customization (Consolidated inside Settings Menu)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DashboardCustomize,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dashboard Widgets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Show, hide, or reorder widgets on your Home Dashboard:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.preferences.orderedWidgets.forEachIndexed { index, widget ->
                            val isEnabled = widget in uiState.preferences.enabledWidgets
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = widget.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = widget.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { onMoveWidget(widget, true) },
                                            enabled = index > 0,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move widget up")
                                        }
                                        IconButton(
                                            onClick = { onMoveWidget(widget, false) },
                                            enabled = index < uiState.preferences.orderedWidgets.lastIndex,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move widget down")
                                        }
                                        Switch(
                                            checked = isEnabled,
                                            onCheckedChange = { onToggleWidget(widget, it) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Biometric & 4-Digit PIN Vault Security Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Biometric & 4-Digit PIN Lock",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Require Biometric / PIN on Launch",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Protects AES-256-GCM encrypted records with Fingerprint/Face or PIN (Default: 1234)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = uiState.preferences.appLockEnabled,
                            onCheckedChange = { enabled ->
                                if (!enabled && uiState.preferences.appLockEnabled) {
                                    showDisableAppLockDialog = true
                                } else {
                                    onToggleAppLock(enabled)
                                }
                            },
                            modifier = Modifier.testTag("app_lock_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showChangePinDialog = true },
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("change_pin_button")
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Change PIN", maxLines = 1)
                        }
                        FilledTonalButton(
                            onClick = onLockVaultNow,
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_lock_now_button")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lock Now", maxLines = 1)
                        }
                    }
                }
            }
        }

        // 5b. Encrypted Vault Backup & Restore Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vault_backup_restore_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Encrypted Vault Backup & Restore",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "AES-256-GCM encrypted snapshot of all accounts, B/F settings, categories, budgets, FX rates & transactions",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onExportVaultBackup,
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_vault_backup_button")
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export & Share Encrypted Backup")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { showConfirmLatestRestoreDialog = true },
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("restore_latest_backup_button")
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore Latest", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                backupFilePickerLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_backup_file_button")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import .JSON", maxLines = 1)
                        }
                    }
                }
            }
        }

        // 6. Real-Time Push Notification Alerts Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Notifications & Budget Alerts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enable Local Push Notifications",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = uiState.preferences.notificationsEnabled,
                            onCheckedChange = onToggleNotifications
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Instant Transaction & B/F Alerts",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = uiState.preferences.txAlertsEnabled,
                            onCheckedChange = onToggleTxAlerts,
                            enabled = uiState.preferences.notificationsEnabled
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Budget 80% Warning & 100% Limit Alerts",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = uiState.preferences.budgetAlertsEnabled,
                            onCheckedChange = onToggleBudgetAlerts,
                            enabled = uiState.preferences.notificationsEnabled
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onSendTestAlert,
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("send_test_alert_button")
                    ) {
                        Text("Send Test Spending Notification")
                    }
                }
            }
        }

        // 7. Multi-Currency & Balance Brought Forward (B/F) Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CurrencyExchange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Currency & Brought Forward (B/F)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = onResetFxRates) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset FX rates to defaults")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Include Account Brought Forward (B/F) in Monthly Closing Balance",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Carries over cumulative prior-period net balances into each month's opening B/F",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = uiState.preferences.includeBroughtForwardInNet,
                            onCheckedChange = onToggleIncludeBfInNet,
                            modifier = Modifier.testTag("carry_over_bf_switch")
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Text(
                        text = "Primary Base Currency:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.exchangeRates.forEach { rate ->
                            FilterChip(
                                selected = uiState.preferences.baseCurrency == rate.currencyCode,
                                onClick = { onSelectBaseCurrency(rate.currencyCode) },
                                shape = CircleShape,
                                label = { Text("${rate.currencyCode} (${rate.symbol.trim()})") }
                            )
                        }
                    }
                }
            }
        }

        // Currency conversion chart in retract mode with small expand button
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fx_conversion_chart_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Offline Currency Rates",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "${uiState.exchangeRates.size} Currencies",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (uiState.preferences.isFxChartExpanded) {
                                    "Tap any rate to customize offline conversion values"
                                } else {
                                    "Retracted • Tap expand to view and override conversion rates"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onToggleFxChartExpanded,
                            modifier = Modifier.testTag("expand_fx_chart_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.preferences.isFxChartExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (uiState.preferences.isFxChartExpanded) "Collapse currency conversion chart" else "Expand currency conversion chart",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    AnimatedVisibility(visible = uiState.preferences.isFxChartExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.exchangeRates.forEach { rate ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${rate.currencyCode} — ${rate.displayName} (${rate.symbol.trim()})",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = if (rate.isCustomOverride) "Custom Offline Rate" else "Standard Offline Rate",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (rate.isCustomOverride) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "1 USD = ${"%.4f".format(rate.rateToUsd)} ${rate.currencyCode}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (rate.currencyCode != "USD") {
                                                IconButton(
                                                    onClick = { editingRate = rate },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Override ${rate.currencyCode} rate",
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showChangePinDialog) {
        var newPin by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Update 4-Digit Vault PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter a 4-digit numeric PIN. It will be salted and hashed with SHA-256 on device.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { newPin = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = { Text("4-Digit PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPin.length == 4) {
                            onUpdatePin(newPin)
                            showChangePinDialog = false
                        }
                    }
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showChangePinDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDisableAppLockDialog) {
        com.example.ui.components.DisableAppLockConfirmationDialog(
            expectedPinHash = uiState.preferences.pinHash,
            onDismiss = { showDisableAppLockDialog = false },
            onConfirmDisable = {
                onToggleAppLock(false)
                showDisableAppLockDialog = false
            }
        )
    }

    if (editingRate != null) {
        val rate = editingRate!!
        var rateText by rememberSaveable { mutableStateOf(rate.rateToUsd.toString()) }
        AlertDialog(
            onDismissRequest = { editingRate = null },
            title = { Text("Override Offline Rate — ${rate.currencyCode}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Specify how many ${rate.currencyCode} (${rate.displayName}) equal 1.00 USD for offline multi-currency conversion:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = { rateText = it },
                        label = { Text("Units of ${rate.currencyCode} per 1 USD") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = rateText.trim().toDoubleOrNull()
                        if (parsed != null && parsed > 0.0) {
                            onUpdateCustomFxRate(rate.currencyCode, parsed)
                            editingRate = null
                        }
                    }
                ) {
                    Text("Save Rate")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { editingRate = null }) { Text("Cancel") }
            }
        )
    }

    if (showCreateCategoryDialog || editingCategory != null) {
        CreateCategoryDialog(
            initialCategory = editingCategory,
            onDismiss = {
                showCreateCategoryDialog = false
                editingCategory = null
            },
            onSave = { name, type, iconKey, colorHex ->
                val existing = editingCategory
                if (existing != null) {
                    onUpdateCategory(existing.id, name, type, iconKey, colorHex)
                } else {
                    onAddCustomCategory(name, type, iconKey, colorHex)
                }
                showCreateCategoryDialog = false
                editingCategory = null
            }
        )
    }

    if (showAddAccountDialog || editingAccount != null) {
        AddOrEditAccountDialog(
            initialAccount = editingAccount,
            availableCurrencies = uiState.exchangeRates.map { it.currencyCode }.ifEmpty { listOf("USD", "EUR", "GBP", "INR", "JPY") },
            onDismiss = {
                showAddAccountDialog = false
                editingAccount = null
            },
            onSave = { id, name, type, currency, initialBf, carryOver, colorHex, acctNum ->
                onSaveAccount(id, name, type, currency, initialBf, carryOver, colorHex, acctNum)
                showAddAccountDialog = false
                editingAccount = null
            }
        )
    }

    if (deletingAccount != null) {
        val accToDelete = deletingAccount!!
        DeleteAccountConfirmationDialog(
            account = accToDelete,
            appLockEnabled = uiState.preferences.appLockEnabled,
            expectedPinHash = uiState.preferences.pinHash,
            onDismiss = { deletingAccount = null },
            onConfirmDelete = {
                onDeleteAccount(accToDelete.id, accToDelete.name)
                deletingAccount = null
            }
        )
    }

    if (showConfirmLatestRestoreDialog || pendingImportedBackupJson != null) {
        val isImportedFile = pendingImportedBackupJson != null
        AlertDialog(
            onDismissRequest = {
                showConfirmLatestRestoreDialog = false
                pendingImportedBackupJson = null
            },
            title = {
                Text(
                    if (isImportedFile) {
                        "Confirm Encrypted .JSON Vault Restore?"
                    } else {
                        "Confirm Latest Snapshot Restore?"
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Restoring this AES-256-GCM encrypted backup will replace your current accounts, B/F settings, categories, monthly budgets, exchange rates, and ledger records with the backup snapshot.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Tip: You can tap 'Export & Share Encrypted Backup' first if you want to save a copy of your current state before restoring.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val imported = pendingImportedBackupJson
                        showConfirmLatestRestoreDialog = false
                        pendingImportedBackupJson = null
                        if (imported != null) {
                            onImportBackupJson(imported)
                        } else {
                            onRestoreLatestBackup()
                        }
                    },
                    modifier = Modifier.testTag("confirm_vault_restore_button")
                ) {
                    Text("Confirm & Restore Vault")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showConfirmLatestRestoreDialog = false
                        pendingImportedBackupJson = null
                    },
                    modifier = Modifier.testTag("cancel_vault_restore_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
