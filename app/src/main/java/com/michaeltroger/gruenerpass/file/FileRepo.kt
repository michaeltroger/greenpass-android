package com.michaeltroger.gruenerpass.file

import android.content.Context
import com.michaeltroger.gruenerpass.coroutines.dispatcher.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

interface FileRepo {
    fun deleteFile(fileName: String)
}

class FileRepoImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : FileRepo {

    @OptIn(DelicateCoroutinesApi::class)
    override fun deleteFile(fileName: String) {
        GlobalScope.launch(dispatcher) {
            if (getFile(fileName).exists()) {
                getFile(fileName).delete()
            }
        }
    }

    private fun getFile(fileName: String): File {
        return File(context.filesDir, fileName)
    }
}
