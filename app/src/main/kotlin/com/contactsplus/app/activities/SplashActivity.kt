package com.contactsplus.app.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.TextView

class SplashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this)
        tv.text = "Launching..."
        setContentView(tv)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
