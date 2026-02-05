package com.contactsplus.app.activities

import android.content.Intent
import android.os.Bundle

class SplashActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}