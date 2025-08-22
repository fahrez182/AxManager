package com.frb.engine

import android.app.Application

lateinit var application: Engine
    private set

open class Engine: Application() {
    override fun onCreate() {
        super.onCreate()
        application = this
    }
}