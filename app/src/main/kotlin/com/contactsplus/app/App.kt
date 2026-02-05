package com.contactsplus.app

import org.fossify.commons.FossifyApp

class App : FossifyApp() {
    override fun onCreate() {
        super.onCreate()
    }

    override fun checkAppSideloading(): Boolean {
        // Always return false to disable the "fake version" popup
        return false
    }
}