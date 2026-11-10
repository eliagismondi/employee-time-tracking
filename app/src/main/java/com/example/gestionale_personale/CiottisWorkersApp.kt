package com.example.gestionale_personale

import android.app.Application

class CiottisWorkersApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
    }
}
