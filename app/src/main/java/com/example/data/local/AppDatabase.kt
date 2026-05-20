package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Transaction

@Database(entities = [Transaction::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract val transactionDao: TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "abundance_ledger_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
