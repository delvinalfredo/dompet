package id.dompetbersama.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.dompetbersama.data.MoneyTransaction
import id.dompetbersama.data.inMonth
import id.dompetbersama.data.limitStatus
import id.dompetbersama.data.totals
import id.dompetbersama.data.walletBalances
import java.time.LocalDate
import java.time.YearMonth
import id.dompetbersama.ui.theme.Dompet

private enum class Tab(val label: String, val icon: ImageVector) {
    Home("Beranda", Icons.Filled.Home),
    Pending("Konfirmasi", Icons.Filled.Notifications),
    Summary("Ringkasan", Icons.AutoMirrored.Filled.List),
    Settings("Pengaturan", Icons.Filled.Settings),
}

private const val SETTING_UNAVAILABLE = "Pengaturan ini tidak tersedia di HP kamu. Cari manual di Setelan sistem."

/** What the bottom sheet is editing. tx null = new manual entry. */
private data class EditRequest(val tx: MoneyTransaction?)

@Composable
fun DompetAppUi(vm: MainViewModel = viewModel()) {
    val confirmed by vm.confirmed.collectAsStateWithLifecycle()
    val pending by vm.pending.collectAsStateWithLifecycle()
    val sources by vm.sources.collectAsStateWithLifecycle()
    val keywordRules by vm.keywordRules.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val money by vm.money.collectAsStateWithLifecycle()
    val lastBackupAt by vm.lastBackupAt.collectAsStateWithLifecycle()
    val restorePlan by vm.restorePlan.collectAsStateWithLifecycle()
    val wallets = remember(money, confirmed) { walletBalances(money.openings, money.main, confirmed) }
    // Refreshed on every resume so "N hari lagi" and the month roll over when the app was left open overnight.
    var today by remember { mutableStateOf(LocalDate.now()) }
    LifecycleResumeEffect(Unit) {
        today = LocalDate.now()
        onPauseOrDispose {}
    }
    val limit = remember(money.monthlyLimit, confirmed, today) {
        money.monthlyLimit?.let { limitStatus(it, confirmed.inMonth(YearMonth.from(today)).totals().expense, today) }
    }
    val hasAccess = rememberNotificationAccess()
    val context = LocalContext.current

    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var editing by remember { mutableStateOf<EditRequest?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        snackbar.showSnackbar(text)
        vm.consumeMessage()
    }

    Scaffold(
        containerColor = Dompet.Paper,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (tab == Tab.Home) {
                ExtendedFloatingActionButton(
                    onClick = { editing = EditRequest(null) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Catat") },
                    containerColor = Dompet.Accent,
                    contentColor = Dompet.Ink,
                )
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Dompet.Paper2) {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = {
                            if (item == Tab.Pending && pending.isNotEmpty()) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = Dompet.Ink, contentColor = Dompet.Paper2) {
                                            Text(if (pending.size > 99) "99+" else "${pending.size}")
                                        }
                                    },
                                ) {
                                    Icon(item.icon, contentDescription = "${pending.size} transaksi perlu dikonfirmasi")
                                }
                            } else {
                                Icon(item.icon, contentDescription = null)
                            }
                        },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Dompet.Accent,
                            selectedIconColor = Dompet.Ink,
                            selectedTextColor = Dompet.Ink,
                            unselectedIconColor = Dompet.Muted,
                            unselectedTextColor = Dompet.Muted,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        when (tab) {
            Tab.Home -> HomeScreen(
                items = confirmed,
                hasAccess = hasAccess,
                contentPadding = padding,
                onOpen = { editing = EditRequest(it) },
                onOpenAccess = {
                    if (!openSystemSetting(context, NOTIFICATION_ACCESS)) vm.notify(SETTING_UNAVAILABLE)
                },
                wallets = wallets,
                limit = limit,
            )
            Tab.Pending -> PendingScreen(
                items = pending,
                hasAccess = hasAccess,
                contentPadding = padding,
                onConfirm = vm::confirm,
                onConfirmTransfer = vm::confirmTransfer,
                onSplitTransfer = vm::splitTransfer,
                onEdit = { editing = EditRequest(it) },
                onDiscard = vm::discard,
                onOpenAccess = {
                    if (!openSystemSetting(context, NOTIFICATION_ACCESS)) vm.notify(SETTING_UNAVAILABLE)
                },
            )
            Tab.Summary -> SummaryScreen(confirmed, padding, onOpen = { editing = EditRequest(it) })
            Tab.Settings -> SettingsScreen(
                sources = sources,
                hasAccess = hasAccess,
                contentPadding = padding,
                onLoadSources = vm::loadSources,
                onToggleSource = vm::setSourceEnabled,
                keywordRules = keywordRules,
                onAddRule = vm::addKeywordRule,
                onRemoveRule = vm::removeKeywordRule,
                onExport = vm::exportCsv,
                onSettingUnavailable = { vm.notify(SETTING_UNAVAILABLE) },
                confirmed = confirmed,
                money = money,
                onSaveWallet = vm::setWallet,
                onRemoveWallet = vm::removeWallet,
                onSetLimit = vm::setMonthlyLimit,
                lastBackupAt = lastBackupAt,
                restorePlan = restorePlan,
                onBackup = vm::backup,
                onPickedRestore = vm::readBackup,
                onConfirmRestore = vm::applyRestore,
                onCancelRestore = vm::cancelRestore,
            )
        }
    }

    editing?.let { request ->
        val tx = request.tx
        TransactionSheet(
            initial = tx,
            onDismiss = { editing = null },
            onSave = { saved, transferChanged ->
                vm.save(saved, transferChanged)
                editing = null
            },
            wallets = money.openings.keys.sorted(),
            defaultWallet = money.main ?: MoneyTransaction.SOURCE_MANUAL,
            onDelete = tx?.let { existing ->
                {
                    vm.discard(existing)
                    editing = null
                }
            },
        )
    }
}
