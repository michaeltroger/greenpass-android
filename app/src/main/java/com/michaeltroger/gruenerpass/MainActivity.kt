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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.michaeltroger.gruenerpass.extensions.getUri
import com.michaeltroger.gruenerpass.settings.PreferenceUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

private const val INTERACTION_TIMEOUT_MS = 5 * 60 * 1000L

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val vm by viewModels<MainViewModel>()
    private val timeoutHandler: Handler = Handler(Looper.getMainLooper())
    private lateinit var interactionTimeoutRunnable: Runnable
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            vm.setPendingFile(intent)
        }
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHost.navController
        setupActionBarWithNavController(navController = navController)

        updateSettings()

        interactionTimeoutRunnable = Runnable {
            vm.onInteractionTimeout()
            if (navController.currentDestination?.id != R.id.mainFragment) {
                navController.popBackStack(R.id.mainFragment, false)
            }
        }
        startTimeoutHandler()
    }

    private fun updateSettings() {
        val preferenceUtil = PreferenceUtil(this)
        lifecycleScope.launch {
            preferenceUtil.updateScreenBrightness(this@MainActivity)
            preferenceUtil.updateShowOnLockedScreen(this@MainActivity)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        vm.deletePendingFileIfExists()
        timeoutHandler.removeCallbacks(interactionTimeoutRunnable)
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
        timeoutHandler.removeCallbacks(interactionTimeoutRunnable)
        startTimeoutHandler()
    }

    private fun startTimeoutHandler() {
        timeoutHandler.postDelayed(interactionTimeoutRunnable, INTERACTION_TIMEOUT_MS)
    }

    private fun MainViewModel.setPendingFile(intent: Intent?) {
        intent?.getUri()?.let {
            setPendingFile(it)
        }
    }

}
