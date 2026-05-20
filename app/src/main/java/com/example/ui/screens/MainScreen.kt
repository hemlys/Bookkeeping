package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Transaction
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.IncomeColor
import com.example.ui.theme.MintDark
import com.example.ui.theme.MintPrimary
import com.example.ui.viewmodel.BudgetViewModel
import com.example.ui.viewmodel.UiMessage
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: BudgetViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Observers from ViewModel
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val availableMonths by viewModel.availableMonths.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val totalIncome by viewModel.totalIncome.collectAsStateWithLifecycle()
    val totalExpense by viewModel.totalExpense.collectAsStateWithLifecycle()
    val expenseCategories by viewModel.expenseCategoryBreakdown.collectAsStateWithLifecycle()
    val incomeCategories by viewModel.incomeCategoryBreakdown.collectAsStateWithLifecycle()

    // Navigation and Local UI state
    var currentTab by remember { mutableIntStateOf(0) } // 0: Ledger, 1: Chart, 2: Backup/Manage
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTxForEdit by remember { mutableStateOf<Transaction?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Transaction?>(null) }

    // Observe Event Messages from ViewModel
    LaunchedEffect(viewModel.uiMessage) {
        viewModel.uiMessage.collectLatest { message ->
            when (message) {
                is UiMessage.Success -> {
                    Toast.makeText(context, message.text, Toast.LENGTH_SHORT).show()
                }
                is UiMessage.Error -> {
                    Toast.makeText(context, message.text, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp),
                            contentColor = Color.White
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = "Ledger Icon",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "豐足記帳",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                            Text(
                                text = "您的個人財務智囊",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                },
                actions = {
                    // Quick Filter Month Selector in Top Bar
                    MonthSelectorDropDown(
                        availableMonths = availableMonths,
                        selectedMonth = selectedMonth,
                        onMonthSelected = { viewModel.selectMonth(it) }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.shadow(4.dp)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.List, contentDescription = "記帳明細") },
                    label = { Text("記帳明細") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("tab_ledger")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = "分類統計") },
                    label = { Text("分類統計") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("tab_statistics")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.SettingsBackupRestore, contentDescription = "匯出匯入") },
                    label = { Text("匯出匯入") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("tab_backup")
                )
            }
        },
        floatingActionButton = {
            if (currentTab == 0) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .testTag("add_transaction_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "新增記帳",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Main Content Area Switcher based on Selected Tab State
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "TabTransition"
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> LedgerTab(
                        transactions = transactions,
                        totalIncome = totalIncome,
                        totalExpense = totalExpense,
                        onTransactionClick = { selectedTxForEdit = it },
                        onTransactionDelete = { showDeleteConfirmDialog = it }
                    )
                    1 -> StatisticsTab(
                        totalIncome = totalIncome,
                        totalExpense = totalExpense,
                        expenseCategories = expenseCategories,
                        incomeCategories = incomeCategories
                    )
                    2 -> ExportImportTab(
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    // Add and Edit Sheet
    if (showAddDialog) {
        TransactionUpsertDialog(
            onDismiss = { showAddDialog = false },
            onSave = { type, category, amount, date, note ->
                viewModel.addTransaction(type, category, amount, date, note)
                showAddDialog = false
            }
        )
    }

    selectedTxForEdit?.let { transaction ->
        TransactionUpsertDialog(
            transactionToEdit = transaction,
            onDismiss = { selectedTxForEdit = null },
            onSave = { type, category, amount, date, note ->
                viewModel.updateTransaction(
                    transaction.copy(
                        type = type,
                        category = category,
                        amount = amount,
                        date = date,
                        note = note
                    )
                )
                selectedTxForEdit = null
            }
        )
    }

    // Delete Double Confirm Dialog
    showDeleteConfirmDialog?.let { transaction ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            icon = { Icon(imageVector = Icons.Default.Warning, contentDescription = "警告", tint = ExpenseColor) },
            title = { Text("確定刪除此筆記錄？", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "您即將刪除一筆金額為 $${transaction.amount} 元的「${transaction.category}」記錄，此動作無法撤銷。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(transaction)
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ExpenseColor)
                ) {
                    Text("確認刪除", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
}

// ==========================================
// MONTH DROPDOWN COMPONENT IN TOPBAR
// ==========================================
@Composable
fun MonthSelectorDropDown(
    availableMonths: List<String>,
    selectedMonth: String,
    onMonthSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.padding(end = 8.dp)) {
        AssistChip(
            onClick = { expanded = true },
            label = {
                Text(
                    text = if (selectedMonth == "全部") "全部月份" else selectedMonth,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "展開下拉選單",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            },
            shape = RoundedCornerShape(12.dp),
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            ),
            border = null
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            availableMonths.forEach { month ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (month == "全部") "全部月份" else month,
                            fontWeight = if (month == selectedMonth) FontWeight.Bold else FontWeight.Normal,
                            color = if (month == selectedMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onMonthSelected(month)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ==========================================
// 1. LEDGER TAB (記帳明細)
// ==========================================
@Composable
fun LedgerTab(
    transactions: List<Transaction>,
    totalIncome: Double,
    totalExpense: Double,
    onTransactionClick: (Transaction) -> Unit,
    onTransactionDelete: (Transaction) -> Unit
) {
    val netBalance = totalIncome - totalExpense

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Quick Aggregation Summary Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.02f)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "本月結餘",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$${String.format(Locale.getDefault(), "%,.1f", netBalance)}",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (netBalance >= 0) IncomeColor else ExpenseColor,
                            letterSpacing = (-1).sp
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Total Income Block
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = IncomeColor.copy(alpha = 0.12f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = "總收入",
                                        tint = IncomeColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "總收入",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "$${String.format(Locale.getDefault(), "%,.1f", totalIncome)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeColor
                                )
                            }
                        }

                        // Vertical Divider line
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                .align(Alignment.CenterVertically)
                        )

                        // Total Expense Block
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .padding( someLeftOffsetForSymmetric = 12.dp )
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = ExpenseColor.copy(alpha = 0.12f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingDown,
                                        contentDescription = "總支出",
                                        tint = ExpenseColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "總支出",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "$${String.format(Locale.getDefault(), "%,.1f", totalExpense)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseColor
                                )
                            }
                        }
                    }
                }
            }
        }

        // Ledger List Label
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "交易明細 (${transactions.size} 筆)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "點擊可編輯，長按或點刪除按紐移除",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (transactions.isEmpty()) {
            // High UX Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = "空空如也",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                    Text(
                        text = "這個月份毫無項目！",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    )
                    Text(
                        text = "點擊右下角「+」號，新增您的第一筆日常支出或收入吧。",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 240.dp)
                    )
                }
            }
        } else {
            // Lazy Ledger List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(transactions, key = { it.id }) { tx ->
                    TransactionListItem(
                        tx = tx,
                        onClick = { onTransactionClick(tx) },
                        onDeleteClick = { onTransactionDelete(tx) }
                    )
                }
            }
        }
    }
}

// Padding helper to allow neat symmetric alignment
private fun Modifier.padding(someLeftOffsetForSymmetric: androidx.compose.ui.unit.Dp): Modifier =
    this.padding(start = someLeftOffsetForSymmetric)

private val Alignment.CenterAlignment: Alignment.Horizontal
    get() = Alignment.CenterHorizontally

// ==========================================
// TRANSACTIONS LIST ITEM CARD
// ==========================================
@Composable
fun TransactionListItem(
    tx: Transaction,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateStr = SimpleDateFormat("MM月dd日", Locale.getDefault()).format(Date(tx.date))
    val isExpense = tx.type == Transaction.TYPE_EXPENSE
    val categoryDetails = getCategoryVisualHelper(tx.category, tx.type)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("transaction_item_${tx.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category Rounded Icon Background
                Surface(
                    shape = CircleShape,
                    color = categoryDetails.color.copy(alpha = 0.12f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = categoryDetails.icon,
                            contentDescription = tx.category,
                            tint = categoryDetails.color,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Title, Notes & Date Detail Text
                Column(modifier = Modifier.widthIn(max = 180.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = tx.category,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Text(
                                text = dateStr,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    if (tx.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tx.note,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Price amount & Quick trash indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${if (isExpense) "-" else "+"}$${String.format(Locale.getDefault(), "%,.1f", tx.amount)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = if (isExpense) ExpenseColor else IncomeColor
                )

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "刪除記錄",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// 2. STATISTICS TAB (分類統計)
// ==========================================
@Composable
fun StatisticsTab(
    totalIncome: Double,
    totalExpense: Double,
    expenseCategories: Map<String, Double>,
    incomeCategories: Map<String, Double>
) {
    var statsType by remember { mutableStateOf(Transaction.TYPE_EXPENSE) } // EXPENSE / INCOME
    val isExpenseMode = statsType == Transaction.TYPE_EXPENSE
    
    val currentTotals = if (isExpenseMode) expenseCategories else incomeCategories
    val overallSum = if (isExpenseMode) totalExpense else totalIncome

    // Sort breakdowns by amount descending
    val sortedBreakdown = remember(currentTotals) {
        currentTotals.toList().sortedByDescending { it.second }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Mode Selector: Expense Segment Button Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { statsType = Transaction.TYPE_EXPENSE },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isExpenseMode) ExpenseColor else MaterialTheme.colorScheme.surface,
                    contentColor = if (isExpenseMode) Color.White else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp),
                border = if (!isExpenseMode) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)) else null,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.TrendingDown, "支出統計", modifier = Modifier.size(16.dp))
                    Text("支出分析", fontWeight = FontWeight.SemiBold)
                }
            }

            Button(
                onClick = { statsType = Transaction.TYPE_INCOME },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isExpenseMode) IncomeColor else MaterialTheme.colorScheme.surface,
                    contentColor = if (!isExpenseMode) Color.White else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp),
                border = if (isExpenseMode) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)) else null,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.TrendingUp, "收入統計", modifier = Modifier.size(16.dp))
                    Text("收入分析", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (sortedBreakdown.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.InsertChart,
                        contentDescription = "無統計資料",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        modifier = Modifier.size(72.dp)
                    )
                    Text(
                        "此月份無「${if (isExpenseMode) "支出" else "收入"}」交易統計",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Circular Canvas Donut Chart Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "類別佔比分佈",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(200.dp)
                            ) {
                                // Dynamic Custom Scaled Donut Canvas Draw
                                DonutChartCanvas(
                                    sortedBreakdown = sortedBreakdown,
                                    overallSum = overallSum,
                                    type = statsType
                                )

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (isExpenseMode) "月總支出" else "月總收入",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = "$${String.format(Locale.getDefault(), "%,.0f", overallSum)}",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isExpenseMode) ExpenseColor else IncomeColor
                                    )
                                    Text(
                                        text = "${sortedBreakdown.size} 分類",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Item lists showing categorized expenditures with visual progress linear bars
                item {
                    Text(
                        text = "各項目金額排行",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                items(sortedBreakdown) { (category, value) ->
                    val percentage = (value / overallSum * 100).toFloat()
                    val categoryHelp = getCategoryVisualHelper(category, statsType)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = categoryHelp.color.copy(alpha = 0.12f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = categoryHelp.icon,
                                                contentDescription = category,
                                                tint = categoryHelp.color,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = category,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = String.format(Locale.getDefault(), "佔比 %.1f%%", percentage),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }

                                Text(
                                    text = "$${String.format(Locale.getDefault(), "%,.1f", value)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Interactive styled matching linear progress indicators with curved edges
                            LinearProgressIndicator(
                                progress = { value.toFloat() / overallSum.toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = categoryHelp.color,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// CUSTOM DONUT CHART CANVAS
// ==========================================
@Composable
fun DonutChartCanvas(
    sortedBreakdown: List<Pair<String, Double>>,
    overallSum: Double,
    type: String
) {
    Canvas(modifier = Modifier.size(190.dp)) {
        if (overallSum <= 0) return@Canvas
        var currentStartAngle = 0f
        
        sortedBreakdown.forEach { (cat, amount) ->
            val sweepAngle = (amount.toFloat() / overallSum.toFloat()) * 360f
            val categoryDetails = getCategoryVisualHelper(cat, type)
            
            drawArc(
                color = categoryDetails.color,
                startAngle = currentStartAngle - 90f, // Rotate to start from vertical top
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
            )
            // Add miniature gaps/spacing between slices by advancing start angle with small delta margin
            currentStartAngle += sweepAngle
        }
    }
}

// ==========================================
// 3. BACKUP & MANAGE TAB (匯出匯入 JSON)
// ==========================================
@Composable
fun ExportImportTab(
    viewModel: BudgetViewModel
) {
    val context = LocalContext.current
    var inputJsonText by remember { mutableStateOf("") }
    var overwriteExisting by remember { mutableStateOf(false) }
    var showDeleteDbWarnDialog by remember { mutableStateOf(false) }

    // SAF File Savers Loaders implementation (Storage Access Framework)
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                val jsonToSave = viewModel.exportToJsonString()
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(jsonToSave.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "成功導出檔案！", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "導出檔案發生錯誤：${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val content = stream.bufferedReader().use { r -> r.readText() }
                    viewModel.importFromJsonString(content, overwriteExisting)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "讀取檔案發生錯誤：${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Explanatory Info Box
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "說明說明",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "此備份功能支持完整的標準 JSON 格式。您可以使用「檔案匯入匯出」來生成及讀取外部備份檔案，也可以單純透過「剪貼簿複製與貼上文字」進行超快速跨裝置轉移備份，不受儲存權限困擾！",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Section A: System File Exporters Importers
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderZip,
                            contentDescription = "SD 備份",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "標準 JSON 檔案匯出匯入",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Mode selections: Overwrite / Append
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (overwriteExisting) "導入時：覆蓋（清除現有全部記錄）" else "導入時：追加（保留現有，追加新記錄）",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (overwriteExisting) ExpenseColor else IncomeColor,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = overwriteExisting,
                            onCheckedChange = { overwriteExisting = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ExpenseColor,
                                checkedTrackColor = ExpenseColor.copy(alpha = 0.3f),
                                uncheckedThumbColor = IncomeColor,
                                uncheckedTrackColor = IncomeColor.copy(alpha = 0.3f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Export File Button
                        Button(
                            onClick = {
                                val cal = Calendar.getInstance()
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(cal.time)
                                exportFileLauncher.launch("豐足記帳備份_$timestamp.json")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("export_file_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.DriveFolderUpload, "匯出", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("匯出備份檔", fontWeight = FontWeight.Bold)
                        }

                        // Import File Button
                        Button(
                            onClick = {
                                importFileLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("import_file_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MintDark),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, "匯入", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("匯入備份檔", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section B: Quick Text Clipboard Block (Foolproof fallback)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPasteGo,
                            contentDescription = "剪貼簿備份",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "快速剪貼簿文字備份",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons (Copy current db to keyboard clipboard)
                    Button(
                        onClick = {
                            val rawJson = viewModel.exportToJsonString()
                            if (rawJson.isEmpty() || rawJson == "[]") {
                                Toast.makeText(context, "目前資料庫無任何資產記錄，無法導出", Toast.LENGTH_SHORT).show()
                            } else {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("記帳JSON備份", rawJson)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "已成功複製 JSON 備份內容至剪貼簿！", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("copy_json_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), contentColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, "複製", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("複製當前全體資料庫 (JSON)", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "欲從剪貼簿恢復？請貼上備份 JSON 文字：",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputJsonText,
                        onValueChange = { inputJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("import_text_input"),
                        placeholder = {
                            Text(
                                "在此粘貼形如 [ {\"type\": \"EXPENSE\", ...} ] 的 JSON 字串備份內容...",
                                fontSize = 12.sp
                            )
                        },
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.importFromJsonString(inputJsonText, overwriteExisting)
                            inputJsonText = "" // Clear textbox on trigger
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("import_paste_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = if (overwriteExisting) ExpenseColor else IncomeColor),
                        shape = RoundedCornerShape(12.dp),
                        enabled = inputJsonText.isNotBlank()
                    ) {
                        Icon(Icons.Default.FileDownload, "匯入", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (overwriteExisting) "覆蓋現有，貼上導入" else "追加保留，貼上導入",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Section C: Wipe Database Actions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ExpenseColor.copy(alpha = 0.03f)),
                border = BorderStroke(1.dp, ExpenseColor.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "危險操作區域",
                        color = ExpenseColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "一鍵清除本機資料庫儲存的所有收支歷史明細（此操作完全不可逆，建議操作前先複製進行備份）。",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { showDeleteDbWarnDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("clear_all_db_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "清空數據", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("清空本機全部記帳數據", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDeleteDbWarnDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDbWarnDialog = false },
            icon = { Icon(imageVector = Icons.Default.ReportProblem, contentDescription = "危險警告", tint = ExpenseColor, modifier = Modifier.size(36.dp)) },
            title = { Text("您確定要彻底清空資料嗎？", fontWeight = FontWeight.Black) },
            text = {
                Text("這會永久抹除您這台設備上的所有記帳項目，且無法從垃圾桶找回。如果您沒有導出備份，數據將全面葬送。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showDeleteDbWarnDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseColor)
                ) {
                    Text("我確定，全面清除", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDbWarnDialog = false }) {
                    Text("取消離開")
                }
            }
        )
    }
}

// ==========================================
// ADD & EDIT TRANSACTION MODAL BOTTOM DIALOG
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionUpsertDialog(
    transactionToEdit: Transaction? = null,
    onDismiss: () -> Unit,
    onSave: (type: String, category: String, amount: Double, date: Long, note: String) -> Unit
) {
    var type by remember { mutableStateOf(transactionToEdit?.type ?: Transaction.TYPE_EXPENSE) }
    var selectedCategory by remember { mutableStateOf(transactionToEdit?.category ?: "") }
    var amountText by remember { mutableStateOf(transactionToEdit?.amount?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "") }
    var noteText by remember { mutableStateOf(transactionToEdit?.note ?: "") }
    var epochDate by remember { mutableLongStateOf(transactionToEdit?.date ?: System.currentTimeMillis()) }

    var isDatePickerVisible by remember { mutableStateOf(false) }
    val showExpense = type == Transaction.TYPE_EXPENSE
    val availableCategories = if (showExpense) Transaction.EXPENSE_CATEGORIES else Transaction.INCOME_CATEGORIES

    // Auto resolve category on toggled type if unselected or incompatible
    LaunchedEffect(type) {
        if (!availableCategories.contains(selectedCategory)) {
            selectedCategory = availableCategories.first()
        }
    }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(16.dp, shape = RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header details
                Text(
                    text = if (transactionToEdit == null) "新增記帳記錄" else "編輯財務記錄",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // 1. Expense/Income Toggle Switches
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (showExpense) ExpenseColor else Color.Transparent)
                            .clickable { type = Transaction.TYPE_EXPENSE }
                            .testTag("toggle_expense"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "支出",
                            color = if (showExpense) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!showExpense) IncomeColor else Color.Transparent)
                            .clickable { type = Transaction.TYPE_INCOME }
                            .testTag("toggle_income"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "收入",
                            color = if (!showExpense) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // 2. Numeric Input Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        // Clean numeric matching formatting check (allows decimal numbers)
                        if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                            amountText = input
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("amount_input"),
                    label = { Text("輸入金額") },
                    placeholder = { Text("請輸入金額") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.AttachMoney,
                            contentDescription = "金額符號",
                            tint = if (showExpense) ExpenseColor else IncomeColor
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // 3. Category Selector Grid
                Text(
                    text = "選擇分類：",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                // Horizontal flowing scroll grid for category selection (beautiful design matching Material 3 grids)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableCategories) { cat ->
                        val isSelected = cat == selectedCategory
                        val visualHelp = getCategoryVisualHelper(cat, type)
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) visualHelp.color.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (isSelected) visualHelp.color else Color.Transparent,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("category_option_$cat")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = visualHelp.icon,
                                    contentDescription = cat,
                                    tint = if (isSelected) visualHelp.color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = cat,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) visualHelp.color else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // 4. Input Remarks Detail Notes
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_input"),
                    label = { Text("備註欄（非必填）") },
                    placeholder = { Text("例如：與朋友午餐聚會...") },
                    leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = "備註備註") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // 5. Calendar Date Picker Button
                val rawFormattedDate = SimpleDateFormat("yyyy 年 MM 月 dd 日", Locale.getDefault()).format(Date(epochDate))
                OutlinedCard(
                    onClick = { isDatePickerVisible = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = "日期日期",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(text = rawFormattedDate, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        Icon(
                            Icons.Default.EditCalendar,
                            contentDescription = "變更日期",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Action Confirm Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("取消關閉")
                    }

                    Button(
                        onClick = {
                            val parsedAmount = amountText.toDoubleOrNull()
                            if (parsedAmount == null || parsedAmount <= 0) {
                                Toast.makeText(context, "請輸入合理的正數金額", Toast.LENGTH_SHORT).show()
                            } else {
                                onSave(type, selectedCategory, parsedAmount, epochDate, noteText)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("save_transaction_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("確認儲存", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Modal Date Picker Sheet overlay
    if (isDatePickerVisible) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = epochDate)
        DatePickerDialog(
            onDismissRequest = { isDatePickerVisible = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            epochDate = it
                        }
                        isDatePickerVisible = false
                    }
                ) {
                    Text("確認選擇", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isDatePickerVisible = false }) {
                    Text("取消關閉")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ==========================================
// CATEGORY GRAPHICAL MAP HELPERS
// ==========================================
data class CategoryVisualInfo(val icon: ImageVector, val color: Color)

fun getCategoryVisualHelper(category: String, type: String): CategoryVisualInfo {
    return if (type == Transaction.TYPE_EXPENSE) {
        when (category) {
            "餐飲" -> CategoryVisualInfo(Icons.Default.Restaurant, Color(0xFFFF9800))     // Orange Amber
            "交通" -> CategoryVisualInfo(Icons.Default.DirectionsCar, Color(0xFF2196F3))   // Sky Blue
            "購物" -> CategoryVisualInfo(Icons.Default.LocalMall, Color(0xFFE91E63))       // Pink Fuchsia
            "娛樂" -> CategoryVisualInfo(Icons.Default.SportsEsports, Color(0xFF9C27B0))   // Purple Orchid
            "居住" -> CategoryVisualInfo(Icons.Default.HomeWork, Color(0xFF795548))        // Warm Slate Brown
            "醫療" -> CategoryVisualInfo(Icons.Default.LocalHospital, Color(0xFFE53935))   // Crimson Red
            "教育" -> CategoryVisualInfo(Icons.Default.School, Color(0xFF00B0FF))          // Cyan Electric
            "投資" -> CategoryVisualInfo(Icons.Default.CurrencyExchange, Color(0xFF00E676)) // Bright Lime Green
            else -> CategoryVisualInfo(Icons.Default.Category, Color(0xFF9E9E9E))          // Cool Grey
        }
    } else {
        when (category) {
            "薪資" -> CategoryVisualInfo(Icons.Default.Payments, Color(0xFF43A047))          // Mint Green
            "獎金" -> CategoryVisualInfo(Icons.Default.EmojiEvents, Color(0xFFFFB300))       // Gold Medal
            "投資收益" -> CategoryVisualInfo(Icons.Default.ShowChart, Color(0xFF00ACC1))     // Peacock Teal
            "零用錢" -> CategoryVisualInfo(Icons.Default.Savings, Color(0xFFEC407A))         // Piggy Pink
            "兼職" -> CategoryVisualInfo(Icons.Default.Badge, Color(0xFF5C6BC0))             // Lavender Blue
            else -> CategoryVisualInfo(Icons.Default.Work, Color(0xFF78909C))              // Blue Gray
        }
    }
}
