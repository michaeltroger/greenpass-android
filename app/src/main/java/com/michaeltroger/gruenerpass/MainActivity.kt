package com.michaeltroger.gruenerpass

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val vm by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            intent?.getUri()?.let(vm::setUri)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.getUri()?.let(vm::setUri)
    }
}

private fun Intent.getUri(): Uri? {
    return when {
        data != null -> {
            data
        }
        action == Intent.ACTION_SEND -> {
            if (extras?.containsKey(Intent.EXTRA_STREAM) == true) {
                getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
            } else {
                null
            }
        }
        else -> {
            null
        }
    }
}