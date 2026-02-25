package com.klecer.gottado.widget

import android.content.Intent
import android.util.Log
import android.widget.RemoteViewsService

class GottaDoRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        Log.d("GottaDoWidget", "onGetViewFactory called, intent=$intent extras=${intent.extras}")
        return GottaDoRemoteViewsFactory(applicationContext, intent)
    }
}
