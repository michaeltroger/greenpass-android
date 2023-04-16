package com.michaeltroger.gruenerpass

import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

private const val INTERACTION_TIMEOUT_MS = 5 * 60 * 1000L

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val vm by viewModels<MainViewModel> { MainViewModelFactory(application)}
    private var timeoutHandler: Handler? = null
    private var interactionTimeoutRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            intent?.getUri()?.let(vm::setUri)
        }
        timeoutHandler =  Handler(Looper.getMainLooper());
        interactionTimeoutRunnable =  Runnable {
            vm.onInteractionTimeout()
        }

        startHandler()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.getUri()?.let(vm::setUri)
    }

    /**
     * To remove focus from any EditText when click outside
     */
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v: View? = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        resetHandler()
    }

    private fun resetHandler() {
        interactionTimeoutRunnable?.let { runnable ->
            timeoutHandler?.removeCallbacks(runnable)
            startHandler()
        }
    }

    private fun startHandler() {
        interactionTimeoutRunnable?.let { runnable ->
            timeoutHandler?.postDelayed(runnable, INTERACTION_TIMEOUT_MS)
        }
    }
}

private fun Intent.getUri(): Uri? {
    return when {
        data != null -> {
            data
        }
        action == Intent.ACTION_SEND -> {
            if (extras?.containsKey(Intent.EXTRA_STREAM) == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
                }
            } else {
                null
            }
        }
        else -> {
            null
        }
    }
}