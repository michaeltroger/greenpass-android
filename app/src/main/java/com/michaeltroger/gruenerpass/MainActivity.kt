package com.michaeltroger.gruenerpass

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.michaeltroger.gruenerpass.certificates.dialogs.CertificateErrors
import com.michaeltroger.gruenerpass.extensions.getUri
import com.michaeltroger.gruenerpass.navigation.GetAutoRedirectDestinationUseCase
import com.michaeltroger.gruenerpass.navigation.GetStartDestinationUseCase
import com.michaeltroger.gruenerpass.settings.PreferenceUtil
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

private const val INTERACTION_TIMEOUT_MS = 5 * 60 * 1000L
private const val PDF_MIME_TYPE = "application/pdf"

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main), AddFile {

    private val vm by viewModels<MainViewModel>()

    @Inject
    lateinit var preferenceUtil: PreferenceUtil
    @Inject
    lateinit var certificateErrors: CertificateErrors
    @Inject
    lateinit var getStartDestinationUseCase: GetStartDestinationUseCase

    private val timeoutHandler: Handler = Handler(Looper.getMainLooper())
    private lateinit var interactionTimeoutRunnable: Runnable

    private var navController: NavController? = null
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
                vm.getAutoRedirectDestination(navController!!).collect {
                    handleTargetDestination(it)
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.viewEvent.collect {
                    handleEvent(it)
                }
            }
        }
    }

    private fun handleEvent(it: ViewEvent) {
        when (it) {
            ViewEvent.ShowParsingFileError -> {
                certificateErrors.showFileErrorSnackbar(window.decorView.rootView)
            }
        }
    }

    private fun handleTargetDestination(navDestination: GetAutoRedirectDestinationUseCase.Result) {
        when (navDestination) {
            GetAutoRedirectDestinationUseCase.Result.NavigateBack -> {
                navController?.popBackStack()
            }

            is GetAutoRedirectDestinationUseCase.Result.NavigateTo -> {
                navController?.navigate(navDestination.navDirections)
            }

            GetAutoRedirectDestinationUseCase.Result.NothingTodo -> {
                // nothing to do
            }
        }
    }

    private suspend fun setUpNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val graphInflater = navHostFragment.navController.navInflater
        val navGraph = graphInflater.inflate(R.navigation.nav_graph)
        navController = navHostFragment.navController

        navGraph.setStartDestination(getStartDestinationUseCase())
        navController!!.graph = navGraph

        setupActionBarWithNavController(
            navController = navController!!,
            configuration = appBarConfiguration.build()
        )
    }

    private fun updateSettings() {
        lifecycleScope.launch {
            preferenceUtil.updateScreenBrightness(this@MainActivity)
            preferenceUtil.updateShowOnLockedScreen(this@MainActivity)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController?.navigateUp() == true || super.onSupportNavigateUp()
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
