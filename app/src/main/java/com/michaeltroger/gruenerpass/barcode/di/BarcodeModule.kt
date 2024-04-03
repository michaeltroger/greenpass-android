package com.michaeltroger.gruenerpass.barcode.di;


import com.michaeltroger.gruenerpass.barcode.BarcodeRenderer
import com.michaeltroger.gruenerpass.barcode.BarcodeRendererImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent

@Module
@InstallIn(FragmentComponent::class)
abstract class BarcodeModule {

    @Binds
    abstract fun bindBarcodeRenderer(
        barcodeRendererImpl: BarcodeRendererImpl
    ): BarcodeRenderer
}
