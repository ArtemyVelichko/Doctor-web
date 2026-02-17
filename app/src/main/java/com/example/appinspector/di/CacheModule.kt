package com.example.appinspector.di

import com.example.appinspector.data.cache.AppIconCache
import com.example.appinspector.data.cache.AppIconCacheImpl
import com.example.appinspector.data.cache.AppIconLoader
import com.example.appinspector.data.cache.AppIconLoaderImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CacheModule {

    @Binds
    @Singleton
    abstract fun bindAppIconLoader(impl: AppIconLoaderImpl): AppIconLoader

    @Binds
    @Singleton
    abstract fun bindAppIconCache(impl: AppIconCacheImpl): AppIconCache
}
