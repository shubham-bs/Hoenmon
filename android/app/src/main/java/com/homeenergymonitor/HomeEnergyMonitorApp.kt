package com.homeenergymonitor

import android.app.Application
import com.homeenergymonitor.di.AppContainer

class HomeEnergyMonitorApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
