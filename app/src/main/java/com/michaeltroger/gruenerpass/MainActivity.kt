package com.michaeltroger.gruenerpass

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val  myViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val bundle = bundleOf(BUNDLE_KEY_URI to intent.data)
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<MainFragment>(R.id.fragment_container_view, args = bundle)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.data == null) return
        myViewModel.updatedUri.tryEmit(intent.data!!)
    }

    companion object {
        const val BUNDLE_KEY_URI = "uri"
    }
}

class MainViewModel: ViewModel() {
    val updatedUri = MutableSharedFlow<Uri>(extraBufferCapacity = 1)
}