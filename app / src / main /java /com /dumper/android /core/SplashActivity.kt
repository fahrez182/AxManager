package com.dumper.android.core

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topjohnwu.superuser.Shell
import kotlin.system.exitProcess

@SuppressLint("CustomSplashScreen")
class SplashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shell.getShell {
            if (it.isRoot) {
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                MaterialAlertDialogBuilder(this@SplashActivity)
                    .setTitle("Error")
                    .setMessage("You need to be root to use this app")
                    .setPositiveButton("Exit") { _, _ -> exitProcess(0) }
                    .show()
            }
        }
    }
}
