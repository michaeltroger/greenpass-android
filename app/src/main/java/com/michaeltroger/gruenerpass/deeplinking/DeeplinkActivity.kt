package com.michaeltroger.gruenerpass.deeplinking

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.michaeltroger.gruenerpass.MainActivity
import com.michaeltroger.gruenerpass.extensions.getUri
import com.michaeltroger.gruenerpass.locator.Locator
import kotlinx.coroutines.launch

class DeeplinkActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            intent.getUri()?.let { uri ->
                Locator.fileRepo(applicationContext).copyToApp(uri).also {
                    startActivity(Intent(baseContext, MainActivity::class.java).apply {
                        putExtra(KEY_EXTRA_PENDING_FILE, it)
                    })
                }
            }

            finishAndRemoveTask()
        }
    }

    companion object {
        const val KEY_EXTRA_PENDING_FILE = "pendingFile"
    }
}
