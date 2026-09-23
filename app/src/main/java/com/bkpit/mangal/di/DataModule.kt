package com.bkpit.mangal.di

import android.content.Context
import com.bkpit.mangal.data.db.AppDatabase
import com.bkpit.mangal.data.repository.ChatRepository
import com.bkpit.mangal.data.repository.ModelDownloadRepository
import com.bkpit.mangal.llm.LlamaEngine
import com.bkpit.mangal.stt.WhisperEngine
import com.bkpit.mangal.tts.TtsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideChatRepository(db: AppDatabase): ChatRepository = ChatRepository(db)

    @Provides
    @Singleton
    fun provideModelDownloadRepository(@ApplicationContext context: Context): ModelDownloadRepository =
        ModelDownloadRepository(context)

    // One shared engine instance each — Phase 5's RAM/thermal throttling
    // depends on there being exactly one loaded model at a time, not one per
    // screen/ViewModel.
    @Provides
    @Singleton
    fun provideLlamaEngine(): LlamaEngine = LlamaEngine()

    @Provides
    @Singleton
    fun provideWhisperEngine(): WhisperEngine = WhisperEngine()

    @Provides
    @Singleton
    fun provideTtsManager(@ApplicationContext context: Context): TtsManager = TtsManager(context)
}
