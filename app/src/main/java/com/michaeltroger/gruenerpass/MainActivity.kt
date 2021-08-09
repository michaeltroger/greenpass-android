package com.michaeltroger.gruenerpass

import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val vm by viewModels<MainViewModel> { MainViewModelFactory(applicationContext)}

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