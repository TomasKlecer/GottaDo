package com.klecer.gottado.di

import com.klecer.gottado.data.repository.CategoryRepositoryImpl
import com.klecer.gottado.data.repository.RecordEditOptionsRepositoryImpl
import com.klecer.gottado.data.repository.RoutineRepositoryImpl
import com.klecer.gottado.data.repository.TaskRepositoryImpl
import com.klecer.gottado.data.repository.TrashRepositoryImpl
import com.klecer.gottado.data.repository.WidgetCategoryRepositoryImpl
import com.klecer.gottado.data.repository.WidgetConfigRepositoryImpl
import com.klecer.gottado.domain.repository.CategoryRepository
import com.klecer.gottado.domain.repository.RecordEditOptionsRepository
import com.klecer.gottado.domain.repository.RoutineRepository
import com.klecer.gottado.domain.repository.TaskRepository
import com.klecer.gottado.domain.repository.TrashRepository
import com.klecer.gottado.domain.repository.WidgetCategoryRepository
import com.klecer.gottado.domain.repository.WidgetConfigRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds repository interfaces to their implementations.
 *
 * Extension point: Add bindings here for future repositories, e.g.:
 * - CalendarSyncRepository (Google Calendar sync settings per category)
 * - NotificationConfigRepository (notification rules per category/global)
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWidgetConfigRepository(impl: WidgetConfigRepositoryImpl): WidgetConfigRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindWidgetCategoryRepository(impl: WidgetCategoryRepositoryImpl): WidgetCategoryRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindRoutineRepository(impl: RoutineRepositoryImpl): RoutineRepository

    @Binds
    @Singleton
    abstract fun bindTrashRepository(impl: TrashRepositoryImpl): TrashRepository

    @Binds
    @Singleton
    abstract fun bindRecordEditOptionsRepository(impl: RecordEditOptionsRepositoryImpl): RecordEditOptionsRepository
}
