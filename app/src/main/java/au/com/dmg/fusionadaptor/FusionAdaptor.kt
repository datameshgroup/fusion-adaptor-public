package au.com.dmg.fusionadaptor

import android.app.Application
import au.com.dmg.fusionadaptor.datastore.Configuration
import com.google.firebase.analytics.FirebaseAnalytics

class FusionAdaptor : Application() {
    companion object {
        lateinit var instance: FusionAdaptor
            private set
    }
    override fun onCreate() {
        super.onCreate()
        instance = this

        Configuration.initialize(this)
//        FirebaseAnalytics.getInstance(this)
        FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(true)
    }
}