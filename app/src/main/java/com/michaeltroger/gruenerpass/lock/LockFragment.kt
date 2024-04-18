package com.michaeltroger.gruenerpass.lock

import android.os.Bundle
import android.view.View
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.databinding.FragmentLockBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject


private const val DELAY_AUTHENTICATION_PROMPT_MS = 1000L

@AndroidEntryPoint
class LockFragment : Fragment(R.layout.fragment_lock) {

    private var binding: FragmentLockBinding? = null

    private val vm by viewModels<LockViewModel>()

    @Inject
    lateinit var biometricPromptInfo: BiometricPrompt.PromptInfo

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentLockBinding.bind(view)

        binding!!.authenticate.setOnClickListener {
            authenticate(0L)
        }
    }

    override fun onResume() {
        super.onResume()
        authenticate()
    }

    private fun authenticate(delay: Long = DELAY_AUTHENTICATION_PROMPT_MS) {
        lifecycleScope.launch {
            delay(delay)
            BiometricPrompt(
                this@LockFragment,
                MyAuthenticationCallback()
            ).authenticate(biometricPromptInfo)
        }
    }

    private inner class MyAuthenticationCallback : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            requireActivity().onUserInteraction()
            vm.onAuthenticationSuccess()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            vm.onAuthenticationError()
        }
    }
}
