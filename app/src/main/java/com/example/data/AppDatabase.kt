package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [VocabularyEntity::class, VocabularyListEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vocabularyDao(): VocabularyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `vocabulary_lists` ADD COLUMN `sourceLanguage` TEXT NOT NULL DEFAULT 'fr'")
                db.execSQL("ALTER TABLE `vocabulary_lists` ADD COLUMN `targetLanguage` TEXT NOT NULL DEFAULT 'en'")
                db.execSQL("ALTER TABLE `vocabulary` ADD COLUMN `translation` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_5 = object : Migration(3, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateVocabularyTableV5(db)
                ensureListsTableV5(db)
            }
        }

        val MIGRATION_2_5 = object : Migration(2, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateVocabularyTableV5(db)
                ensureListsTableV5(db)
            }
        }

        val MIGRATION_1_5 = object : Migration(1, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureListsTableV5(db)
                migrateVocabularyTableV5(db)
            }
        }

        private fun ensureListsTableV5(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `vocabulary_lists` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `sourceLanguage` TEXT NOT NULL DEFAULT 'fr',
                    `targetLanguage` TEXT NOT NULL DEFAULT 'en',
                    `createdAt` INTEGER NOT NULL
                )
            """.trimIndent())
        }

        private fun migrateVocabularyTableV5(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `vocabulary_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `listId` INTEGER NOT NULL DEFAULT 1,
                    `listName` TEXT NOT NULL DEFAULT '',
                    `word` TEXT NOT NULL,
                    `translation` TEXT NOT NULL DEFAULT '',
                    `definition` TEXT NOT NULL DEFAULT '',
                    `contextSentence` TEXT NOT NULL DEFAULT '',
                    `timestamp` INTEGER NOT NULL,
                    `isFavorite` INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())
            db.execSQL("""
                INSERT INTO `vocabulary_new` (`id`, `listId`, `listName`, `word`, `translation`, `definition`, `contextSentence`, `timestamp`, `isFavorite`)
                SELECT `id`, COALESCE(`listId`, 1), COALESCE(`listName`, ''), `word`, '', COALESCE(`definition`, ''), COALESCE(`contextSentence`, ''), `timestamp`, COALESCE(`isFavorite`, 0)
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
                    .addMigrations(MIGRATION_1_5, MIGRATION_2_5, MIGRATION_3_5, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
