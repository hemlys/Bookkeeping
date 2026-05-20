package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Transaction
import com.example.data.repository.TransactionRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository
    
    // Moshi JSON adapter configuration
    private val moshi = Moshi.Builder().build()
    private val listType = Types.newParameterizedType(List::class.java, Transaction::class.java)
    private val jsonAdapter = moshi.adapter<List<Transaction>>(listType)

    // Raw stream of all transactions from Room
    val allTransactions: StateFlow<List<Transaction>>

    // Selected month filter (Format: "yyyy-MM", e.g., "2026-05")
    private val _selectedMonth = MutableStateFlow("")
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    // Screen states & Event messages
    private val _uiMessage = MutableSharedFlow<UiMessage>()
    val uiMessage = _uiMessage.asSharedFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = TransactionRepository(db.transactionDao)
        
        allTransactions = repository.allTransactions
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        // Default selected month to current month
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        _selectedMonth.value = sdf.format(Date())
    }

    // Filtered transaction list based on selectedMonth
    val filteredTransactions = combine(allTransactions, selectedMonth) { txList, month ->
        if (month.isEmpty() || month == "全部") {
            txList
        } else {
            txList.filter { tx ->
                val txMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(tx.date))
                txMonth == month
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Unique months available in database for dropdown selector
    val availableMonths = allTransactions.map { txList ->
        val months = txList.map { tx ->
            SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(tx.date))
        }.distinct().sortedDescending().toMutableList()
        
        // Ensure current month is always available even if empty
        val current = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        if (!months.contains(current)) {
            months.add(0, current)
        }
        months.add("全部")
        months
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listListOf(SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()), "全部"))

    // Calculations for the current filtered scope
    val totalIncome = filteredTransactions.map { list ->
        list.filter { it.type == Transaction.TYPE_INCOME }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense = filteredTransactions.map { list ->
        list.filter { it.type == Transaction.TYPE_EXPENSE }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val expenseCategoryBreakdown = filteredTransactions.map { list ->
        list.filter { it.type == Transaction.TYPE_EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val incomeCategoryBreakdown = filteredTransactions.map { list ->
        list.filter { it.type == Transaction.TYPE_INCOME }
            .groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun selectMonth(month: String) {
        _selectedMonth.value = month
    }

    // Database Actions
    fun addTransaction(type: String, category: String, amount: Double, date: Long, note: String) {
        viewModelScope.launch {
            if (amount <= 0) {
                _uiMessage.emit(UiMessage.Error("金額必須大於 0"))
                return@launch
            }
            val tx = Transaction(type = type, category = category, amount = amount, date = date, note = note)
            repository.insert(tx)
            _uiMessage.emit(UiMessage.Success("已新增一筆記錄"))
        }
    }

    fun updateTransaction(tx: Transaction) {
        viewModelScope.launch {
            if (tx.amount <= 0) {
                _uiMessage.emit(UiMessage.Error("金額必須大於 0"))
                return@launch
            }
            repository.update(tx)
            _uiMessage.emit(UiMessage.Success("已更新記錄"))
        }
    }

    fun deleteTransaction(tx: Transaction) {
        viewModelScope.launch {
            repository.delete(tx)
            _uiMessage.emit(UiMessage.Success("已刪除記錄"))
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAll()
            _uiMessage.emit(UiMessage.Success("所有資料已清除"))
        }
    }

    // JSON Export / Import Core Logic
    fun exportToJsonString(): String {
        return try {
            val list = allTransactions.value
            // Beautify output with indent for easy copy paste
            jsonAdapter.indent("  ").toJson(list)
        } catch (e: Exception) {
            ""
        }
    }

    fun importFromJsonString(jsonString: String, overwrite: Boolean) {
        viewModelScope.launch {
            if (jsonString.trim().isEmpty()) {
                _uiMessage.emit(UiMessage.Error("導入內容不可為空"))
                return@launch
            }
            try {
                // Strip redundant outer spaces before passing to Moshi
                val cleanedJson = jsonString.trim()
                val importedList = jsonAdapter.fromJson(cleanedJson)
                if (importedList.isNullOrEmpty()) {
                    _uiMessage.emit(UiMessage.Error("沒有找到任何可導入的記帳記錄"))
                    return@launch
                }
                repository.importTransactions(importedList, overwrite)
                _uiMessage.emit(UiMessage.Success("成功導入 ${importedList.size} 筆記錄！"))
            } catch (e: Exception) {
                _uiMessage.emit(UiMessage.Error("JSON 格式不正確，導入失敗：${e.localizedMessage}"))
            }
        }
    }

    // Fallback static list factory matching type signature
    private fun <T> listListOf(vararg elements: T): List<T> {
        return elements.toList()
    }
}

sealed class UiMessage {
    data class Success(val text: String) : UiMessage()
    data class Error(val text: String) : UiMessage()
}
