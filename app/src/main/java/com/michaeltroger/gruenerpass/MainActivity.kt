package com.michaeltroger.gruenerpass

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.michaeltroger.gruenerpass.db.Certificate
import com.michaeltroger.gruenerpass.deeplinking.DeeplinkActivity

private const val INTERACTION_TIMEOUT_MS = 5 * 60 * 1000L

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val vm by viewModels<MainViewModel> { MainViewModelFactory(application)}
    private var timeoutHandler: Handler? = null
    private var interactionTimeoutRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            vm.setPendingFile(intent)
        }
        timeoutHandler =  Handler(Looper.getMainLooper());
        interactionTimeoutRunnable = Runnable {
            vm.onInteractionTimeout()
        }

        startHandler()
    }

    override fun onDestroy() {
        vm.deletePendingFileIfExists()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        vm.setPendingFile(intent)
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

private fun MainViewModel.setPendingFile(intent: Intent?) {
    if (intent == null) return
    @Suppress("DEPRECATION")
    (intent.getParcelableExtra(DeeplinkActivity.KEY_EXTRA_PENDING_FILE) as Certificate?)?.let {
        setPendingFile(it)
    }
}
