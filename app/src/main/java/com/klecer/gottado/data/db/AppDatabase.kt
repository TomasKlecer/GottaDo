package com.klecer.gottado.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.klecer.gottado.data.db.converter.AppConverters
import com.klecer.gottado.data.db.dao.CalendarDismissedDao
import com.klecer.gottado.data.db.dao.CategoryDao
import com.klecer.gottado.data.db.dao.RoutineDao
import com.klecer.gottado.data.db.dao.TaskDao
import com.klecer.gottado.data.db.dao.TrashEntryDao
import com.klecer.gottado.data.db.dao.WidgetCategoryJoinDao
import com.klecer.gottado.data.db.dao.WidgetConfigDao
import com.klecer.gottado.data.db.entity.CalendarDismissedEntity
import com.klecer.gottado.data.db.entity.CategoryEntity
import com.klecer.gottado.data.db.entity.RoutineEntity
import com.klecer.gottado.data.db.entity.TaskEntity
import com.klecer.gottado.data.db.entity.TrashEntryEntity
import com.klecer.gottado.data.db.entity.WidgetCategoryJoinEntity
import com.klecer.gottado.data.db.entity.CalendarSyncRuleEntity
import com.klecer.gottado.data.db.entity.WidgetConfigEntity
import com.klecer.gottado.data.db.entity.WidgetInstanceEntity
import com.klecer.gottado.data.db.dao.CalendarSyncRuleDao
import com.klecer.gottado.data.db.dao.WidgetInstanceDao

@Database(
    entities = [
        WidgetConfigEntity::class,
        CategoryEntity::class,
        WidgetCategoryJoinEntity::class,
        TaskEntity::class,
        RoutineEntity::class,
        TrashEntryEntity::class,
        CalendarDismissedEntity::class,
        WidgetInstanceEntity::class,
        CalendarSyncRuleEntity::class
    ],
    version = 14,
    exportSchema = true
)
@TypeConverters(AppConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun widgetConfigDao(): WidgetConfigDao
    abstract fun categoryDao(): CategoryDao
    abstract fun widgetCategoryJoinDao(): WidgetCategoryJoinDao
    abstract fun taskDao(): TaskDao
    abstract fun routineDao(): RoutineDao
    abstract fun trashEntryDao(): TrashEntryDao
    abstract fun calendarDismissedDao(): CalendarDismissedDao
    abstract fun widgetInstanceDao(): WidgetInstanceDao
    abstract fun calendarSyncRuleDao(): CalendarSyncRuleDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE task ADD COLUMN textColor INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE category ADD COLUMN color INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE widget_config ADD COLUMN titleColor INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE widget_config ADD COLUMN subtitleColor INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE widget_config ADD COLUMN bulletSizeDp INTEGER NOT NULL DEFAULT 16")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE widget_config ADD COLUMN checkboxSizeDp INTEGER NOT NULL DEFAULT 16")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE widget_config ADD COLUMN showTitleOnWidget INTEGER NOT NULL DEFAULT 1")
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routine ADD COLUMN name TEXT DEFAULT NULL")
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE category ADD COLUMN syncWithCalendarToday INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE task ADD COLUMN fromCalendarSync INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE category ADD COLUMN showCalendarIcon INTEGER NOT NULL DEFAULT 1")
                db.execSQL("CREATE TABLE IF NOT EXISTS calendar_dismissed (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, categoryId INTEGER NOT NULL, eventTitle TEXT NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_calendar_dismissed_categoryId_eventTitle ON calendar_dismissed (categoryId, eventTitle)")
            }
        }
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE category ADD COLUMN showDeleteButton INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE widget_config ADD COLUMN buttonsAtBottom INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS widget_instance (appWidgetId INTEGER NOT NULL PRIMARY KEY, presetId INTEGER NOT NULL)")
                db.execSQL("INSERT OR IGNORE INTO widget_instance (appWidgetId, presetId) SELECT widgetId, widgetId FROM widget_config")
            }
        }
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS calendar_sync_rule (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, categoryId INTEGER NOT NULL, ruleType TEXT NOT NULL)")
                db.execSQL("INSERT INTO calendar_sync_rule (categoryId, ruleType) SELECT id, 'TODAY' FROM category WHERE syncWithCalendarToday = 1")
                val cursor = db.query("SELECT id, categoryType FROM category WHERE categoryType != 'normal'")
                while (cursor.moveToNext()) {
                    val catId = cursor.getLong(0)
                    val type = cursor.getString(1)
                    val rule = when (type) {
                        "today" -> "TODAY"
                        "tomorrow" -> "TOMORROW"
                        "monday" -> "CLOSEST_MONDAY"
                        "tuesday" -> "CLOSEST_TUESDAY"
                        "wednesday" -> "CLOSEST_WEDNESDAY"
                        "thursday" -> "CLOSEST_THURSDAY"
                        "friday" -> "CLOSEST_FRIDAY"
                        "saturday" -> "CLOSEST_SATURDAY"
                        "sunday" -> "CLOSEST_SUNDAY"
                        else -> null
                    }
                    if (rule != null) {
                        db.execSQL("INSERT OR IGNORE INTO calendar_sync_rule (categoryId, ruleType) SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM calendar_sync_rule WHERE categoryId = ? AND ruleType = ?)",
                            arrayOf<Any>(catId, rule, catId, rule))
                    }
                }
                cursor.close()
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE category ADD COLUMN notifyOnTime INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE category ADD COLUMN notifyMinutesBefore INTEGER NOT NULL DEFAULT 15")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE category ADD COLUMN autoSortTimedEntries INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE category ADD COLUMN timedEntriesAscending INTEGER NOT NULL DEFAULT 1")
            }
        }
    }
}
