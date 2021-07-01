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
            supportFragmentManager.commit {
                val bundle = bundleOf(BUNDLE_KEY_URI to getUriFromIntent())
                setReorderingAllowed(true)
                add<MainFragment>(R.id.fragment_container_view, args = bundle)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        getUriFromIntent()?.let {
            myViewModel.updatedUri.tryEmit(it)
        }
    }

    private fun getUriFromIntent(): Uri? {
        return when {
            intent.data != null -> {
                intent.data
            }
            intent.action == Intent.ACTION_SEND -> {
                if (intent.extras?.containsKey(Intent.EXTRA_STREAM) == true) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
                } else {
                    null
                }
            }
            else -> {
                null
            }
        }
    }

    companion object {
        const val BUNDLE_KEY_URI = "uri"
    }
}

class MainViewModel: ViewModel() {
    val updatedUri = MutableSharedFlow<Uri>(extraBufferCapacity = 1)
}