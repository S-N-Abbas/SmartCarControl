package com.example.smartcarcontrol.di

import com.example.smartcarcontrol.data.ai.AIRepositoryImpl
import com.example.smartcarcontrol.data.bluetooth.BluetoothRepositoryImpl
import com.example.smartcarcontrol.domain.repository.IAIRepository
import com.example.smartcarcontrol.domain.repository.IBluetoothRepository
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
    abstract fun bindBluetoothRepository(
        impl: BluetoothRepositoryImpl
    ): IBluetoothRepository

    @Binds
    @Singleton
    abstract fun bindAIRepository(
        impl: AIRepositoryImpl
    ): IAIRepository
}