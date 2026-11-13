package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [VocabularyEntity::class, VocabularyListEntity::class],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vocabularyDao(): VocabularyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `vocabulary` ADD COLUMN `contextCountListen` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `vocabulary` ADD COLUMN `contextCountRead` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `vocabulary` ADD COLUMN `nbrRevisions` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `vocabulary` ADD COLUMN `lastRevisionDate` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `vocabulary` ADD COLUMN `translationRecognized` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `vocabulary` ADD COLUMN `translationNotRecognized` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `vocabulary` ADD COLUMN `aiExamplesJson` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateVocabularyTableV6(db)
            }
        }

        val MIGRATION_4_6 = object : Migration(4, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `vocabulary_lists` ADD COLUMN `sourceLanguage` TEXT NOT NULL DEFAULT 'fr'")
                db.execSQL("ALTER TABLE `vocabulary_lists` ADD COLUMN `targetLanguage` TEXT NOT NULL DEFAULT 'en'")
                migrateVocabularyTableV6(db)
            }
        }

        private fun migrateVocabularyTableV6(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `vocabulary_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `listId` INTEGER NOT NULL DEFAULT 1,
                    `listName` TEXT NOT NULL DEFAULT '',
                    `word` TEXT NOT NULL,
                    `translation` TEXT NOT NULL DEFAULT '',
                    `contextSentence` TEXT NOT NULL DEFAULT '',
                    `aiExamplesJson` TEXT NOT NULL DEFAULT '',
                    `timestamp` INTEGER NOT NULL,
                    `isFavorite` INTEGER NOT NULL DEFAULT 0,
                    `nbrRevisions` INTEGER NOT NULL DEFAULT 0,
                    `lastRevisionDate` INTEGER NOT NULL DEFAULT 0,
                    `translationRecognized` INTEGER NOT NULL DEFAULT 0,
                    `translationNotRecognized` INTEGER NOT NULL DEFAULT 0,
                    `contextCountListen` INTEGER NOT NULL DEFAULT 0,
                    `contextCountRead` INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())
            db.execSQL("""
                INSERT INTO `vocabulary_new` (`id`, `listId`, `listName`, `word`, `translation`, `contextSentence`, `aiExamplesJson`, `timestamp`, `isFavorite`, `nbrRevisions`, `lastRevisionDate`, `translationRecognized`, `translationNotRecognized`, `contextCountListen`, `contextCountRead`)
                SELECT `id`, COALESCE(`listId`, 1), COALESCE(`listName`, ''), `word`, COALESCE(`translation`, ''), COALESCE(`contextSentence`, ''), '', `timestamp`, COALESCE(`isFavorite`, 0), 0, 0, 0, 0, 0, 0
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
                    .addMigrations(MIGRATION_4_6, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .fallbackToDestructiveMigration()
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
