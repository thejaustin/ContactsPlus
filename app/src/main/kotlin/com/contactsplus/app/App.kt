package com.contactsplus.app

import org.fossify.commons.FossifyApp

class App : FossifyApp() {
    override fun onCreate() {
        super.onCreate()
    }

    fun checkAppSideloading() = false
}