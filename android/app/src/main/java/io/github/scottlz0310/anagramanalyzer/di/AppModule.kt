package io.github.scottlz0310.anagramanalyzer.di

import android.content.Context
import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.scottlz0310.anagramanalyzer.data.datastore.DataStoreInputHistoryStore
import io.github.scottlz0310.anagramanalyzer.data.datastore.DataStoreQuizScoreStore
import io.github.scottlz0310.anagramanalyzer.data.datastore.DataStoreSearchSettingsStore
import io.github.scottlz0310.anagramanalyzer.data.datastore.InputHistoryStore
import io.github.scottlz0310.anagramanalyzer.data.datastore.QuizScoreStore
import io.github.scottlz0310.anagramanalyzer.data.datastore.SearchSettingsStore
import io.github.scottlz0310.anagramanalyzer.data.db.AnagramDao
import io.github.scottlz0310.anagramanalyzer.data.db.AnagramDatabase
import io.github.scottlz0310.anagramanalyzer.data.db.CandidateDetailCacheDao
import io.github.scottlz0310.anagramanalyzer.data.seed.AdditionalSeedEntryLoader
import io.github.scottlz0310.anagramanalyzer.data.seed.AssetAdditionalSeedEntryLoader
import io.github.scottlz0310.anagramanalyzer.data.seed.AssetCandidateDetailLoader
import io.github.scottlz0310.anagramanalyzer.data.seed.AssetSeedEntryLoader
import io.github.scottlz0310.anagramanalyzer.data.seed.CandidateDetailLoader
import io.github.scottlz0310.anagramanalyzer.data.seed.CandidateDetailRemoteDataSource
import io.github.scottlz0310.anagramanalyzer.data.seed.JishoCandidateDetailRemoteDataSource
import io.github.scottlz0310.anagramanalyzer.data.seed.SeedEntryLoader
import io.github.scottlz0310.anagramanalyzer.domain.model.PreloadLogger
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
    @Singleton
    fun provideQuizScoreStore(
        @ApplicationContext context: Context,
    ): QuizScoreStore = DataStoreQuizScoreStore(context)

    @Provides
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun providePreloadLogger(): PreloadLogger = PreloadLogger { message ->
        Log.i("AnagramPreload", message)
    }
}
