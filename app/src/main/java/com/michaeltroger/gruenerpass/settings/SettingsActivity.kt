package com.michaeltroger.gruenerpass.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.michaeltroger.gruenerpass.R

class SettingsActivity : AppCompatActivity(R.layout.activity_settings) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = getString(R.string.settings)
    }
}
