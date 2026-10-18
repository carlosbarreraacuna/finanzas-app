package com.carlos.finanzas.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TransactionEntity::class,
        UnrecognizedMessageEntity::class,
        HealthEventEntity::class,
        TemplateEntity::class,
        CategoryRuleEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactions(): TransactionDao
    abstract fun unrecognized(): UnrecognizedDao
    abstract fun health(): HealthDao
    abstract fun templates(): TemplateDao
    abstract fun categoryRules(): CategoryRuleDao

    companion object {
        const val FILE_NAME = "finanzas.db"
        /** Debe coincidir con la versión del @Database; la usa BackupManager al restaurar. */
        const val SCHEMA_VERSION = 3

        @Volatile private var instance: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `templates` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`pattern` TEXT NOT NULL, " +
                        "`isIncome` INTEGER NOT NULL, " +
                        "`sender` TEXT, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `category_rules` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`merchantKey` TEXT NOT NULL, " +
                        "`category` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_category_rules_merchantKey` " +
                        "ON `category_rules` (`merchantKey`)"
                )
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    FILE_NAME
                )
                    // IMPORTANTE: nunca usar fallbackToDestructiveMigration().
                    // Cada cambio de esquema necesita su Migration explícita.
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }

        /** Cierra la base y descarta la instancia (se usa antes de restaurar un backup). */
        fun closeAndReset() {
            synchronized(this) {
                instance?.close()
                instance = null
            }
        }
    }
}
