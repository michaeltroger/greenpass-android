package com.michaeltroger.gruenerpass

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext

@OptIn(ExperimentalCoroutinesApi::class)
class InstantExecutionExtension(private val dispatcher: TestDispatcher = StandardTestDispatcher()) :
    BeforeTestExecutionCallback, AfterTestExecutionCallback {

    override fun beforeTestExecution(context: ExtensionContext?) {
        Dispatchers.setMain(dispatcher)
    }

    override fun afterTestExecution(context: ExtensionContext?) {
        Dispatchers.resetMain()
    }
}
