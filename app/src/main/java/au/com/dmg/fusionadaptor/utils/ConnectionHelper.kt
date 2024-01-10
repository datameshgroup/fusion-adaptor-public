package au.com.dmg.fusionadaptor.utils

import android.content.Context

class ConnectionHelper{
    companion object {
        fun isNetworkConnected(applicationContext: Context): Boolean {
            return true
        }

        /*
        fun isNetworkConnected(applicationContext: Context): Boolean {
            val connMgr = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                ?: return false
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connMgr.activeNetwork ?: return false
                val capabilities = connMgr.getNetworkCapabilities(network)
                capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            } else {
                val networkInfo = connMgr.activeNetworkInfo
                networkInfo != null && networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_WIFI
            }
        }
        */
    }
}