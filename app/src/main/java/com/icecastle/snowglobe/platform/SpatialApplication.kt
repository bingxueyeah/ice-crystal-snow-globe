package com.icecastle.snowglobe.platform

import android.app.Application
import com.pico.spatial.ui.foundation.dsl.launch
import com.icecastle.snowglobe.mainApp

class SpatialApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        launch(::mainApp)
    }
}
