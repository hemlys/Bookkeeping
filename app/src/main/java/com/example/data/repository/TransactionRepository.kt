package com.example.data.repository

import com.example.data.local.TransactionDao
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) {
    
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    suspend fun insert(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun update(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun delete(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    /**
     * Imports a list of transactions.
     * @param transactions The transactions to import.
     * @param overwrite If true, clears the database before importing. Otherwise, appends.
     */
    suspend fun importTransactions(transactions: List<Transaction>, overwrite: Boolean) {
        if (overwrite) {
            transactionDao.deleteAllTransactions()
        }
        transactionDao.insertAll(transactions)
    }

    suspend fun clearAll() {
        transactionDao.deleteAllTransactions()
    }
}
