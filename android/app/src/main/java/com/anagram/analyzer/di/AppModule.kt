package com.anagram.analyzer.di

import android.content.Context
import android.util.Log
import com.anagram.analyzer.data.datastore.DataStoreInputHistoryStore
import com.anagram.analyzer.data.datastore.DataStoreSearchSettingsStore
import com.anagram.analyzer.data.datastore.InputHistoryStore
import com.anagram.analyzer.data.datastore.SearchSettingsStore
import com.anagram.analyzer.data.db.AnagramDao
import com.anagram.analyzer.data.db.AnagramDatabase
import com.anagram.analyzer.data.db.CandidateDetailCacheDao
import com.anagram.analyzer.data.seed.AdditionalSeedEntryLoader
import com.anagram.analyzer.data.seed.AssetAdditionalSeedEntryLoader
import com.anagram.analyzer.data.seed.AssetCandidateDetailLoader
import com.anagram.analyzer.data.seed.AssetSeedEntryLoader
import com.anagram.analyzer.data.seed.CandidateDetailLoader
import com.anagram.analyzer.data.seed.CandidateDetailRemoteDataSource
import com.anagram.analyzer.data.seed.JishoCandidateDetailRemoteDataSource
import com.anagram.analyzer.data.seed.SeedEntryLoader
import com.anagram.analyzer.domain.model.PreloadLogger
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
    fun provideCandidateDetailCacheDao(
        database: AnagramDatabase,
    ): CandidateDetailCacheDao = database.candidateDetailCacheDao()

    @Provides
    @Singleton
    fun provideCandidateDetailRemoteDataSource(): CandidateDetailRemoteDataSource = JishoCandidateDetailRemoteDataSource()

    @Provides
    @Singleton
    fun provideSeedEntryLoader(
        @ApplicationContext context: Context,
    ): SeedEntryLoader = AssetSeedEntryLoader(context)

    @Provides
    @Singleton
    fun provideCandidateDetailLoader(
        @ApplicationContext context: Context,
        candidateDetailCacheDao: CandidateDetailCacheDao,
        candidateDetailRemoteDataSource: CandidateDetailRemoteDataSource,
    ): CandidateDetailLoader = AssetCandidateDetailLoader(
        context = context,
        candidateDetailCacheDao = candidateDetailCacheDao,
        candidateDetailRemoteDataSource = candidateDetailRemoteDataSource,
    )

    @Provides
    @Singleton
    fun provideAdditionalSeedEntryLoader(
        @ApplicationContext context: Context,
    ): AdditionalSeedEntryLoader = AssetAdditionalSeedEntryLoader(context)

    @Provides
    @Singleton
    fun provideInputHistoryStore(
        @ApplicationContext context: Context,
    ): InputHistoryStore = DataStoreInputHistoryStore(context)

    @Provides
    @Singleton
    fun provideSearchSettingsStore(
        @ApplicationContext context: Context,
    ): SearchSettingsStore = DataStoreSearchSettingsStore(context)

    @Provides
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun providePreloadLogger(): PreloadLogger = PreloadLogger { message ->
        Log.i("AnagramPreload", message)
    }
}
