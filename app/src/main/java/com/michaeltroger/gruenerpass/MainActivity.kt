package com.michaeltroger.gruenerpass

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.michaeltroger.gruenerpass.extensions.getUri
import com.michaeltroger.gruenerpass.lock.AppLockedRepo
import com.michaeltroger.gruenerpass.pdfimporter.PdfImporter
import com.michaeltroger.gruenerpass.settings.PreferenceUtil
import com.michaeltroger.gruenerpass.settings.getBooleanFlow
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private const val INTERACTION_TIMEOUT_MS = 5 * 60 * 1000L
private const val PDF_MIME_TYPE = "application/pdf"

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main), AddFile {

    private val vm by viewModels<MainViewModel>()

    @Inject
    lateinit var preferenceUtil: PreferenceUtil

    @Inject
    lateinit var lockedRepo: AppLockedRepo

    @Inject
    lateinit var getStartDestinationUseCase: GetStartDestinationUseCase

    @Inject
    lateinit var pdfImporter: PdfImporter

    @Inject
    lateinit var sharedPrefs: SharedPreferences

    private val showListLayout by lazy {
        sharedPrefs.getBooleanFlow(
            getString(R.string.key_preference_show_list_layout),
            false
        )
    }

    private val timeoutHandler: Handler = Handler(Looper.getMainLooper())
    private lateinit var interactionTimeoutRunnable: Runnable
    private lateinit var navController: NavController
    private val appBarConfiguration = AppBarConfiguration.Builder(
        R.id.certificatesFragment,
        R.id.certificatesListFragment,
        R.id.lockFragment,
    )

    private val documentPick = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        vm.setPendingFile(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            vm.setPendingFile(intent)
        }
        updateSettings()

        interactionTimeoutRunnable = InteractionTimeoutRunnable()
        startTimeoutHandler()

        lifecycleScope.launch {
            setUpNavigation()
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    lockedRepo.isAppLocked(),
                    showListLayout,
                    pdfImporter.hasPendingFile(),
                    navController.currentBackStackEntryFlow,
                    ::autoRedirect
                ).collect {
                    // do nothing
                }
            }
        }
    }

    private suspend fun setUpNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val graphInflater = navHostFragment.navController.navInflater
        val navGraph = graphInflater.inflate(R.navigation.nav_graph)
        navController = navHostFragment.navController

        navGraph.setStartDestination(getStartDestinationUseCase())
        navController.graph = navGraph

        setupActionBarWithNavController(
            navController = navController,
            configuration = appBarConfiguration.build()
        )
    }

    private fun autoRedirect(
        isAppLocked: Boolean,
        showListLayout: Boolean,
        hasPendingFile: Boolean,
        navBackStackEntry: NavBackStackEntry
    ) {
        val currentDestinationId = navBackStackEntry.destination.id
        val certificatesDestination = if (showListLayout) {
            NavGraphDirections.actionGlobalCertificatesListFragment()
        } else {
            NavGraphDirections.actionGlobalCertificatesFragment()
        }
        val destination = when {
            isAppLocked && currentDestinationId != R.id.lockFragment -> {
                NavGraphDirections.actionGlobalLockFragment()
            }
            !isAppLocked && hasPendingFile && currentDestinationId in listOf(
                R.id.moreFragment,
                R.id.settingsFragment,
                R.id.certificateDetailsFragment,
            ) -> {
                certificatesDestination
            }
            !isAppLocked && currentDestinationId == R.id.lockFragment -> {
                certificatesDestination
            }
            !isAppLocked && currentDestinationId == R.id.certificatesFragment && showListLayout-> {
                NavGraphDirections.actionGlobalCertificatesListFragment()
            }
            !isAppLocked && currentDestinationId == R.id.certificatesListFragment && !showListLayout -> {
                NavGraphDirections.actionGlobalCertificatesFragment()
            }
            else -> {
                null // do nothing
            }
        } ?: return
        navController.navigate(destination)
    }

    private fun updateSettings() {
        lifecycleScope.launch {
            preferenceUtil.updateScreenBrightness(this@MainActivity)
            preferenceUtil.updateShowOnLockedScreen(this@MainActivity)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        timeoutHandler.removeCallbacks(interactionTimeoutRunnable)
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        vm.setPendingFile(intent)
    }

    override fun addFile() {
        documentPick.launch(arrayOf(PDF_MIME_TYPE))
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
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

    private inner class InteractionTimeoutRunnable : Runnable {
        override fun run() {
            vm.onInteractionTimeout()
        }
    }
}

interface AddFile {
    fun addFile()
}
