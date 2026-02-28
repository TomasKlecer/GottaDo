package com.klecer.gottado.di

import android.content.Context
import androidx.room.Room
import com.klecer.gottado.data.db.AppDatabase
import com.klecer.gottado.data.db.dao.CalendarDismissedDao
import com.klecer.gottado.data.db.dao.CategoryDao
import com.klecer.gottado.data.db.dao.RoutineDao
import com.klecer.gottado.data.db.dao.TaskDao
import com.klecer.gottado.data.db.dao.TrashEntryDao
import com.klecer.gottado.data.db.dao.WidgetCategoryJoinDao
import com.klecer.gottado.data.db.dao.WidgetConfigDao
import com.klecer.gottado.data.db.dao.CalendarSyncRuleDao
import com.klecer.gottado.data.db.dao.WidgetInstanceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "gottado.db")
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10, AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13, AppDatabase.MIGRATION_13_14, AppDatabase.MIGRATION_14_15, AppDatabase.MIGRATION_15_16, AppDatabase.MIGRATION_16_17, AppDatabase.MIGRATION_17_18)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    @Singleton
    fun provideWidgetConfigDao(db: AppDatabase): WidgetConfigDao = db.widgetConfigDao()

    @Provides
    @Singleton
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    @Singleton
    fun provideWidgetCategoryJoinDao(db: AppDatabase): WidgetCategoryJoinDao = db.widgetCategoryJoinDao()

    @Provides
    @Singleton
    fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()

    @Provides
    @Singleton
    fun provideRoutineDao(db: AppDatabase): RoutineDao = db.routineDao()

    @Provides
    @Singleton
    fun provideTrashEntryDao(db: AppDatabase): TrashEntryDao = db.trashEntryDao()

    @Provides
    @Singleton
    fun provideCalendarDismissedDao(db: AppDatabase): CalendarDismissedDao = db.calendarDismissedDao()

    @Provides
    @Singleton
    fun provideWidgetInstanceDao(db: AppDatabase): WidgetInstanceDao = db.widgetInstanceDao()

    @Provides
    @Singleton
    fun provideCalendarSyncRuleDao(db: AppDatabase): CalendarSyncRuleDao = db.calendarSyncRuleDao()
}
