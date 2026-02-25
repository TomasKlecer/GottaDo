package com.klecer.gottado.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    @Provides
    @Singleton
    @Named("record_edit_options")
    fun provideRecordEditOptionsPrefs(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences("record_edit_options", Context.MODE_PRIVATE)
}
