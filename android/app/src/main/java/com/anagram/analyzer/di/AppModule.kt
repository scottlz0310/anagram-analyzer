package com.anagram.analyzer.di

import android.content.Context
import com.anagram.analyzer.data.db.AnagramDao
import com.anagram.analyzer.data.db.AnagramDatabase
import com.anagram.analyzer.data.seed.AssetSeedEntryLoader
import com.anagram.analyzer.data.seed.SeedEntryLoader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideAnagramDatabase(
        @ApplicationContext context: Context,
    ): AnagramDatabase = AnagramDatabase.getInstance(context)

    @Provides
    fun provideAnagramDao(database: AnagramDatabase): AnagramDao = database.anagramDao()

    @Provides
    @Singleton
    fun provideSeedEntryLoader(
        @ApplicationContext context: Context,
    ): SeedEntryLoader = AssetSeedEntryLoader(context)

    @Provides
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
