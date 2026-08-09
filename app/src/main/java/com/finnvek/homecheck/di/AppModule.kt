package com.finnvek.homecheck.di

import android.content.Context
import androidx.room.Room
import com.finnvek.homecheck.billing.BillingManager
import com.finnvek.homecheck.billing.PlayBillingManager
import com.finnvek.homecheck.data.local.HomeCheckDatabase
import dagger.Binds
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
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): HomeCheckDatabase = Room.databaseBuilder(context, HomeCheckDatabase::class.java, "homecheck.db").build()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BillingModule {
    @Binds
    @Singleton
    abstract fun bindBillingManager(implementation: PlayBillingManager): BillingManager
}
