package com.klecer.gottado.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.klecer.gottado.data.db.converter.AppConverters
import com.klecer.gottado.data.db.dao.CategoryDao
import com.klecer.gottado.data.db.dao.RoutineDao
import com.klecer.gottado.data.db.dao.TaskDao
import com.klecer.gottado.data.db.dao.TrashEntryDao
import com.klecer.gottado.data.db.dao.WidgetCategoryJoinDao
import com.klecer.gottado.data.db.dao.WidgetConfigDao
import com.klecer.gottado.data.db.entity.CategoryEntity
import com.klecer.gottado.data.db.entity.RoutineEntity
import com.klecer.gottado.data.db.entity.TaskEntity
import com.klecer.gottado.data.db.entity.TrashEntryEntity
import com.klecer.gottado.data.db.entity.WidgetCategoryJoinEntity
import com.klecer.gottado.data.db.entity.WidgetConfigEntity

@Database(
    entities = [
        WidgetConfigEntity::class,
        CategoryEntity::class,
        WidgetCategoryJoinEntity::class,
        TaskEntity::class,
        RoutineEntity::class,
        TrashEntryEntity::class
    ],
    version = 5,
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
    }
}
