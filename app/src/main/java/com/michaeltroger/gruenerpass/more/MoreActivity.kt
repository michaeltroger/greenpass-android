package com.michaeltroger.gruenerpass.more

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.michaeltroger.gruenerpass.R

class MoreActivity : AppCompatActivity(R.layout.activity_more) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = getString(R.string.more)
    }

}