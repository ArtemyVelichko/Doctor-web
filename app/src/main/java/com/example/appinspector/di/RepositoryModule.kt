package com.example.appinspector.di

import com.example.appinspector.data.repository.AllAppsRepository
import com.example.appinspector.data.repository.AllAppsRepositoryImpl
import com.example.appinspector.data.repository.AppDetailsRepository
import com.example.appinspector.data.repository.AppDetailsRepositoryImpl
import com.example.appinspector.data.repository.AppIconRepository
import com.example.appinspector.data.repository.AppIconRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAllAppsRepository(impl: AllAppsRepositoryImpl): AllAppsRepository

    @Binds
    @Singleton
    abstract fun bindAppIconRepository(impl: AppIconRepositoryImpl): AppIconRepository

    @Binds
    @Singleton
    abstract fun bindAppDetailsRepository(impl: AppDetailsRepositoryImpl): AppDetailsRepository
}
