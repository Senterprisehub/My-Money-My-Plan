package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.local.TransactionType
import com.example.data.repository.FinanceRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.domain.ParsedFinancialAlert
import com.example.domain.ThemeMode
import com.example.domain.TransactionModel
import com.example.ui.components.AddOrEditTransactionDialog
import com.example.ui.components.CreditCardExpenditureDialog
import com.example.ui.components.DataExportCustomFilterDialog
import com.example.ui.components.DeleteTransactionConfirmationDialog
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LockScreen
import com.example.ui.screens.RecordsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.BankAndUpiAlertParser
import com.example.util.StatementExporter

enum class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val showInBottomBar: Boolean = true
) {
    DASHBOARD("dashboard", "Dashboard", Icons.Default.Dashboard, true),
    RECORDS("records", "Records", Icons.AutoMirrored.Filled.ReceiptLong, true),
    ACCOUNTS("accounts", "Accounts", Icons.Default.AccountBalanceWallet, true),
    BUDGETS("budgets", "Budgets", Icons.Default.Savings, true),
    ANALYTICS("analytics", "Analytics", Icons.Default.PieChart, true),
    SETTINGS("settings", "Settings", Icons.Default.Settings, false)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyMoneyProAppRoot()
        }
    }
}

@Composable
fun MyMoneyProAppRoot() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getInstance(context) }
    val repository = remember { FinanceRepository(database.financeDao()) }
    val preferencesRepository = remember { UserPreferencesRepository(context) }

    val viewModel: FinanceViewModel = viewModel(
        factory = FinanceViewModel.provideFactory(context, repository, preferencesRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Handled gracefully by NotificationHelper */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val isDark = when (uiState.preferences.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    MyApplicationTheme(
        darkTheme = isDark,
        dynamicColor = uiState.preferences.dynamicColorEnabled,
        seedPalette = uiState.preferences.seedPalette,
        fontStyle = uiState.preferences.fontStyle
    ) {
        if (uiState.isVaultLocked) {
            LockScreen(
                pinError = uiState.pinError,
                onBiometricSuccess = { viewModel.unlockWithBiometric() },
                onSubmitPin = { pin -> viewModel.unlockWithPin(pin) }
            )
        } else {
            MyMoneyProMainScaffold(
                viewModel = viewModel,
                uiState = uiState
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyMoneyProMainScaffold(
    viewModel: FinanceViewModel,
    uiState: com.example.ui.viewmodel.FinanceUiState
) {
    val context = LocalContext.current
    var currentTab by rememberSaveable { mutableStateOf(AppDestination.DASHBOARD) }
    var showTransactionDialog by remember { mutableStateOf(false) }
    var showCustomExportDialog by remember { mutableStateOf(false) }
    var showCreditCardExpenditureDialog by remember { mutableStateOf(false) }
    var dialogInitialType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var editingTransaction by remember { mutableStateOf<TransactionModel?>(null) }
    var editingPendingAlertId by remember { mutableStateOf<String?>(null) }
    var deletingTransaction by remember { mutableStateOf<TransactionModel?>(null) }

    val latestUiState by rememberUpdatedState(uiState)
    val tabStateHolder = rememberSaveableStateHolder()

    val requestDeleteTransaction: (Long) -> Unit = remember(viewModel) {
        { txId ->
            val found = latestUiState.allTransactions.find { it.id == txId }
            if (found != null) {
                deletingTransaction = found
            } else {
                viewModel.deleteTransaction(txId)
            }
        }
    }

    val openPendingAlertForEdit: (ParsedFinancialAlert) -> Unit = remember {
        { alert ->
            val stateSnapshot = latestUiState
            val matchedAccount = BankAndUpiAlertParser.matchAccount(alert, stateSnapshot.accounts)
            val matchedCategory = BankAndUpiAlertParser.matchCategory(alert, stateSnapshot.categories)
            val autoNote = buildString {
                append("Auto-detected from ${alert.sourceAppLabel}")
                if (!alert.accountHint.isNullOrBlank()) append(" • Account: ${alert.accountHint}")
                if (!alert.referenceId.isNullOrBlank()) append(" • Ref: ${alert.referenceId}")
            }
            editingTransaction = TransactionModel(
                id = 0L,
                accountId = matchedAccount?.id ?: stateSnapshot.accounts.firstOrNull()?.id ?: 0L,
                accountName = matchedAccount?.name ?: "",
                targetAccountId = null,
                targetAccountName = null,
                categoryId = matchedCategory?.id,
                categoryName = matchedCategory?.name ?: alert.suggestedCategoryName,
                categoryIconKey = matchedCategory?.iconKey ?: "receipt",
                categoryColorHex = matchedCategory?.colorHex ?: "#0061A4",
                type = alert.type,
                amount = alert.amount,
                targetAmount = null,
                currencyCode = matchedAccount?.currencyCode ?: alert.currencyCode,
                targetCurrencyCode = null,
                normalizedAmountInBase = alert.amount,
                title = "[${alert.sourceAppLabel}] ${alert.merchantOrPayee}",
                note = autoNote,
                timestamp = alert.timestamp
            )
            dialogInitialType = alert.type
            editingPendingAlertId = alert.id
            showTransactionDialog = true
        }
    }

    val onOpenAccountLedger: (Long) -> Unit = remember(viewModel) {
        { accId ->
            viewModel.selectAccountFilter(accId)
            currentTab = AppDestination.RECORDS
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val bottomBarDestinations = remember { AppDestination.entries.filter { it.showInBottomBar } }

    LaunchedEffect(uiState.statusBannerMessage) {
        val msg = uiState.statusBannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBannerMessage()
        }
    }

    if (currentTab != AppDestination.DASHBOARD) {
        BackHandler {
            currentTab = AppDestination.DASHBOARD
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 640.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = if (currentTab == AppDestination.DASHBOARD) {
                                "My Money My Plan (MMMP)"
                            } else {
                                currentTab.label
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        if (currentTab == AppDestination.SETTINGS) {
                            IconButton(
                                onClick = { currentTab = AppDestination.DASHBOARD },
                                modifier = Modifier.testTag("top_bar_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Dashboard"
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = viewModel::togglePrivacyMask,
                            modifier = Modifier.testTag("top_bar_privacy_mask_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.preferences.privacyMaskEnabled) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (uiState.preferences.privacyMaskEnabled) {
                                    "Show Balances"
                                } else {
                                    "Hide Balances"
                                }
                            )
                        }
                        if (uiState.preferences.appLockEnabled) {
                            IconButton(
                                onClick = viewModel::lockVaultNow,
                                modifier = Modifier.testTag("top_bar_lock_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Lock Vault"
                                )
                            }
                        }
                        if (currentTab != AppDestination.SETTINGS) {
                            IconButton(
                                onClick = { currentTab = AppDestination.SETTINGS },
                                modifier = Modifier.testTag("top_bar_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Open Settings Menu"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = {
                if (currentTab != AppDestination.SETTINGS) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            editingTransaction = null
                            dialogInitialType = TransactionType.EXPENSE
                            showTransactionDialog = true
                        },
                        shape = CircleShape,
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("Record", fontWeight = FontWeight.SemiBold) },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.testTag("fab_add_record")
                    )
                }
            },
            bottomBar = {
                if (!isWideScreen && currentTab != AppDestination.SETTINGS) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 3.dp,
                        modifier = Modifier.testTag("bottom_navigation_bar")
                    ) {
                        bottomBarDestinations.forEach { dest ->
                            NavigationBarItem(
                                selected = currentTab == dest,
                                onClick = { currentTab = dest },
                                icon = {
                                    Icon(
                                        imageVector = dest.icon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_tab_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.testTag("navigation_rail")
                    ) {
                        AppDestination.entries.forEach { dest ->
                            NavigationRailItem(
                                selected = currentTab == dest,
                                onClick = { currentTab = dest },
                                icon = {
                                    Icon(
                                        imageVector = dest.icon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("rail_tab_${dest.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    Crossfade(
                        targetState = currentTab,
                        animationSpec = tween(durationMillis = 150),
                        label = "tab_crossfade"
                    ) { activeTab ->
                        tabStateHolder.SaveableStateProvider(activeTab.route) {
                            when (activeTab) {
                        AppDestination.DASHBOARD -> {
                            DashboardScreen(
                                uiState = uiState,
                                onPreviousMonth = viewModel::previousMonth,
                                onNextMonth = viewModel::nextMonth,
                                onResetToToday = viewModel::resetToCurrentMonth,
                                onOpenQuickRecord = { tType ->
                                    editingTransaction = null
                                    editingPendingAlertId = null
                                    dialogInitialType = tType
                                    showTransactionDialog = true
                                },
                                onEditTransaction = { tx ->
                                    editingTransaction = tx
                                    editingPendingAlertId = null
                                    showTransactionDialog = true
                                },
                                onDeleteTransaction = requestDeleteTransaction,
                                onApprovePendingAlert = viewModel::approvePendingAlert,
                                onEditPendingAlert = openPendingAlertForEdit,
                                onDismissPendingAlert = viewModel::dismissPendingAlert,
                                onApproveAllPendingAlerts = viewModel::approveAllPendingAlerts,
                                onNavigateToAccounts = { currentTab = AppDestination.ACCOUNTS },
                                onNavigateToBudgets = { currentTab = AppDestination.BUDGETS },
                                onNavigateToRecords = { currentTab = AppDestination.RECORDS },
                                onExportCsv = { viewModel.exportStatement(asPdf = false) },
                                onExportPdf = { viewModel.exportStatement(asPdf = true) },
                                onOpenAccountLedger = onOpenAccountLedger,
                                onOpenCreditCardExpenditure = { showCreditCardExpenditureDialog = true },
                                onOpenCustomExport = { showCustomExportDialog = true }
                            )
                        }
                        AppDestination.RECORDS -> {
                            RecordsScreen(
                                uiState = uiState,
                                onPreviousMonth = viewModel::previousMonth,
                                onNextMonth = viewModel::nextMonth,
                                onResetToToday = viewModel::resetToCurrentMonth,
                                onSelectDateRangePreset = viewModel::selectDateRangePreset,
                                onSetCustomDateRange = viewModel::setCustomDateRange,
                                onSelectAccountFilter = viewModel::selectAccountFilter,
                                onSelectTypeFilter = viewModel::selectTypeFilter,
                                onSelectCategoryFilter = viewModel::selectCategoryFilter,
                                onSelectSortOrder = viewModel::selectSortOrder,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onEditTransaction = { tx ->
                                    editingTransaction = tx
                                    editingPendingAlertId = null
                                    showTransactionDialog = true
                                },
                                onDeleteTransaction = requestDeleteTransaction,
                                onApprovePendingAlert = viewModel::approvePendingAlert,
                                onEditPendingAlert = openPendingAlertForEdit,
                                onDismissPendingAlert = viewModel::dismissPendingAlert,
                                onApproveAllPendingAlerts = viewModel::approveAllPendingAlerts,
                                onExportFilteredCsv = { viewModel.exportStatement(asPdf = false) },
                                onExportFilteredPdf = { viewModel.exportStatement(asPdf = true) }
                            )
                        }
                        AppDestination.ACCOUNTS -> {
                            AccountsScreen(
                                uiState = uiState,
                                onPreviousMonth = viewModel::previousMonth,
                                onNextMonth = viewModel::nextMonth,
                                onResetToToday = viewModel::resetToCurrentMonth,
                                onSaveAccount = viewModel::saveAccount,
                                onDeleteAccount = viewModel::deleteAccount,
                                onUpdateAccountBf = viewModel::updateAccountBroughtForwardSettings,
                                onReconcileAccountBalance = viewModel::reconcileAccountBalanceWithBfAdjustment,
                                onOpenAccountLedger = onOpenAccountLedger
                            )
                        }
                        AppDestination.BUDGETS -> {
                            BudgetsScreen(
                                uiState = uiState,
                                onPreviousMonth = viewModel::previousMonth,
                                onNextMonth = viewModel::nextMonth,
                                onResetToToday = viewModel::resetToCurrentMonth,
                                onSaveBudget = viewModel::saveMonthlyBudget,
                                onDeleteBudget = viewModel::deleteBudget,
                                onCopyPreviousMonthBudgets = viewModel::copyPreviousMonthBudgets,
                                onAddCustomCategory = { name, type, iconKey, colorHex ->
                                    viewModel.addCustomCategory(name, type, iconKey, colorHex)
                                },
                                onUpdateCategory = viewModel::saveOrUpdateCategory,
                                onDeleteCategory = viewModel::deleteCategory
                            )
                        }
                        AppDestination.ANALYTICS -> {
                            AnalyticsScreen(
                                uiState = uiState,
                                onPreviousMonth = viewModel::previousMonth,
                                onNextMonth = viewModel::nextMonth,
                                onResetToToday = viewModel::resetToCurrentMonth,
                                onExportCsv = { viewModel.exportStatement(asPdf = false) },
                                onExportPdf = { viewModel.exportStatement(asPdf = true) },
                                onDismissExportPreview = viewModel::dismissExportPreview,
                                onDownloadStatement = viewModel::downloadStatement,
                                onOpenCustomExport = { showCustomExportDialog = true }
                            )
                        }
                        AppDestination.SETTINGS -> {
                            SettingsScreen(
                                uiState = uiState,
                                onSelectThemeMode = viewModel::setThemeMode,
                                onToggleDynamicColor = viewModel::setDynamicColorEnabled,
                                onSelectSeedPalette = viewModel::setSeedPalette,
                                onSelectBaseCurrency = viewModel::setBaseCurrency,
                                onToggleIncludeBfInNet = viewModel::setIncludeBroughtForwardInNet,
                                onToggleWidget = viewModel::setWidgetEnabled,
                                onMoveWidget = viewModel::moveWidget,
                                onToggleAppLock = viewModel::setAppLockEnabled,
                                onUpdatePin = viewModel::updateVaultPin,
                                onLockVaultNow = viewModel::lockVaultNow,
                                onToggleNotifications = viewModel::setNotificationsEnabled,
                                onToggleTxAlerts = viewModel::setTxAlertsEnabled,
                                onToggleBudgetAlerts = viewModel::setBudgetAlertsEnabled,
                                onToggleAutoTrack = viewModel::setAutoTrackEnabled,
                                onSelectAutoTrackMode = viewModel::setAutoTrackMode,
                                onToggleAutoTrackSource = viewModel::setAutoTrackSourceEnabled,
                                onSendTestAlert = viewModel::triggerTestNotificationAlert,
                                onUpdateCustomFxRate = viewModel::updateCustomExchangeRate,
                                onResetFxRates = viewModel::resetExchangeRates,
                                onToggleFxChartExpanded = viewModel::toggleFxChartExpanded,
                                onSaveAccount = viewModel::saveAccount,
                                onDeleteAccount = viewModel::deleteAccount,
                                onAddCustomCategory = { name, type, iconKey, colorHex ->
                                    viewModel.addCustomCategory(name, type, iconKey, colorHex)
                                },
                                onUpdateCategory = viewModel::saveOrUpdateCategory,
                                onDeleteCategory = viewModel::deleteCategory,
                                onExportVaultBackup = { viewModel.exportEncryptedVaultBackup(shareImmediately = true) },
                                onRestoreLatestBackup = viewModel::restoreLatestVaultBackup,
                                onImportBackupJson = viewModel::restoreVaultBackupFromJson,
                                onSelectFontStyle = viewModel::setFontStyle
                            )
                            }
                        }
                    }
                }
            }
        }
    }
    }

    if (deletingTransaction != null) {
        val txToDelete = deletingTransaction!!
        DeleteTransactionConfirmationDialog(
            transaction = txToDelete,
            rates = uiState.ratesMap,
            onDismiss = { deletingTransaction = null },
            onConfirmDelete = {
                viewModel.deleteTransaction(txToDelete.id)
                deletingTransaction = null
            }
        )
    }

    if (showTransactionDialog) {
        AddOrEditTransactionDialog(
            initialTransaction = editingTransaction,
            initialType = dialogInitialType,
            accounts = uiState.accounts,
            categories = uiState.categories,
            onDismiss = {
                showTransactionDialog = false
                editingTransaction = null
                editingPendingAlertId = null
            },
            onNavigateToAccounts = {
                currentTab = AppDestination.ACCOUNTS
            },
            onAddCategory = { name, type, iconKey, colorHex ->
                viewModel.addCustomCategory(name, type, iconKey, colorHex)
            },
            onDeleteCategory = viewModel::deleteCategory,
            onSave = { id, accId, targetAccId, catId, type, amount, title, note, timestamp ->
                viewModel.addOrUpdateTransaction(
                    id = id,
                    accountId = accId,
                    targetAccountId = targetAccId,
                    categoryId = catId,
                    type = type,
                    amount = amount,
                    title = title,
                    note = note,
                    timestamp = timestamp
                )
                val pendingId = editingPendingAlertId
                if (!pendingId.isNullOrBlank()) {
                    viewModel.dismissPendingAlert(pendingId)
                }
                editingPendingAlertId = null
            }
        )
    }

    if (uiState.latestExportResult != null && currentTab != AppDestination.ANALYTICS) {
        val result = uiState.latestExportResult!!
        AlertDialog(
            onDismissRequest = viewModel::dismissExportPreview,
            title = { Text("My Money My Plan (MMMP) — Statement Export Ready") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = result.fileName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = result.previewSummary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.downloadStatement(result)
                            viewModel.dismissExportPreview()
                        },
                        shape = CircleShape,
                        modifier = Modifier.testTag("export_download_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download")
                    }
                    Button(
                        onClick = {
                            StatementExporter.shareExportedFile(context, result)
                            viewModel.dismissExportPreview()
                        },
                        shape = CircleShape,
                        modifier = Modifier.testTag("export_share_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissExportPreview) {
                    Text("Close")
                }
            }
        )
    }

    if (showCustomExportDialog) {
        DataExportCustomFilterDialog(
            accounts = uiState.accounts,
            categories = uiState.categories,
            baseCurrency = uiState.preferences.baseCurrency,
            onDismiss = { showCustomExportDialog = false },
            onExportAndDownload = { params ->
                viewModel.exportCustomFilteredStatement(params, downloadOnly = true)
                showCustomExportDialog = false
            },
            onExportAndShare = { params ->
                viewModel.exportCustomFilteredStatement(params, downloadOnly = false)
                showCustomExportDialog = false
            }
        )
    }

    if (showCreditCardExpenditureDialog) {
        CreditCardExpenditureDialog(
            creditCardAccounts = uiState.creditCardAccounts,
            totalExpenditureInBase = uiState.totalCreditCardExpenseInBase,
            baseCurrency = uiState.preferences.baseCurrency,
            rates = uiState.ratesMap,
            recentCreditCardTransactions = uiState.periodTransactions.filter { tx ->
                uiState.creditCardAccounts.any { it.id == tx.accountId }
            },
            privacyMaskEnabled = uiState.preferences.privacyMaskEnabled,
            onDismiss = { showCreditCardExpenditureDialog = false },
            onViewCreditCardRecords = { accId ->
                showCreditCardExpenditureDialog = false
                if (accId != null) {
                    onOpenAccountLedger(accId)
                } else {
                    currentTab = AppDestination.RECORDS
                }
            }
        )
    }
}
