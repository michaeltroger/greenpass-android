package com.michaeltroger.gruenerpass.extensions

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build

fun Context.getPackageInfo(): PackageInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0)
    }

fun Context.getSignature(): Signature? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        packageInfo.signingInfo?.signingCertificateHistory?.firstOrNull()
    } else {
        @Suppress("DEPRECATION")
        val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
        @Suppress("DEPRECATION")
        packageInfo.signatures?.firstOrNull()
    }
}

fun Context.getInstallerPackageName(): String? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        packageManager.getInstallSourceInfo(packageName).installingPackageName
    } else {
        @Suppress("DEPRECATION")
        packageManager.getInstallerPackageName(packageName)
    }
