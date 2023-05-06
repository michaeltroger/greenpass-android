package com.michaeltroger.gruenerpass.extensions

import android.content.Intent
import android.net.Uri

fun Intent.getUri(): Uri? {
    return when {
        data != null -> {
            data
        }
        action == Intent.ACTION_SEND -> {
            if (extras?.containsKey(Intent.EXTRA_STREAM) == true) {
                @Suppress("DEPRECATION")
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
