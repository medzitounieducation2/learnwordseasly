package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [VocabularyEntity::class, VocabularyListEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vocabularyDao(): VocabularyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_4 = object : Migration(1, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `vocabulary_lists` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                migrateVocabularyTable(db)
            }
        }

        val MIGRATION_2_4 = object : Migration(2, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateVocabularyTable(db)
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateVocabularyTable(db)
            }
        }

        private fun migrateVocabularyTable(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `vocabulary_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `listId` INTEGER NOT NULL DEFAULT 1,
                    `listName` TEXT NOT NULL DEFAULT '',
                    `word` TEXT NOT NULL,
                    `definition` TEXT NOT NULL DEFAULT '',
                    `contextSentence` TEXT NOT NULL DEFAULT '',
                    `timestamp` INTEGER NOT NULL,
                    `isFavorite` INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())
            db.execSQL("""
                INSERT INTO `vocabulary_new` (`id`, `listId`, `listName`, `word`, `definition`, `contextSentence`, `timestamp`, `isFavorite`)
                SELECT `id`, COALESCE(`listId`, 1), COALESCE(`listName`, ''), `word`, COALESCE(`definition`, ''), COALESCE(`contextSentence`, ''), `timestamp`, COALESCE(`isFavorite`, 0)
                FROM `vocabulary`
            """.trimIndent())
            db.execSQL("DROP TABLE `vocabulary`")
            db.execSQL("ALTER TABLE `vocabulary_new` RENAME TO `vocabulary`")
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vocabulaire_lecture.db"
                )
                    .addMigrations(MIGRATION_1_4, MIGRATION_2_4, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
