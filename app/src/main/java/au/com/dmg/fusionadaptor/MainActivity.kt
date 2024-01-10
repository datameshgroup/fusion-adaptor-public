package au.com.dmg.fusionadaptor

import android.content.Intent
import android.os.Bundle
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import au.com.dmg.fusionadaptor.datastore.Configuration
import au.com.dmg.fusionadaptor.screens.SettingsActivity
import au.com.dmg.fusionadaptor.utils.DatameshUtils
import au.com.dmg.fusionadaptor.utils.DatameshUtils.Companion.appIntVersion
import au.com.dmg.fusionadaptor.utils.DatameshUtils.Companion.appVersion
import au.com.dmg.fusionadaptor.utils.DatameshUtils.Companion.logInfo
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


open class MainActivity: AppCompatActivity(R.layout.main_activity) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DatameshUtils.appName = resources.getString(R.string.app_name)
        appVersion = resources.getString(R.string.application_version)
        appIntVersion =  resources.getInteger(R.integer.application_int_version)

        val deviceVersion = resources.getString(R.string.device_version)

        val isPosOnTerminal = deviceVersion in listOf("pax", "ingenico")

        lifecycleScope.launch {
            Configuration.setOnTerminal(applicationContext, isPosOnTerminal) // Set the new value for onTerminal
            if(isPosOnTerminal){
                val txtVersion = findViewById<TextView>(R.id.txtVersionOnTerminal)
                txtVersion.text = appVersion
                val toggleTipping = findViewById<Switch>(R.id.toggleTippingOnTerminal)
                toggleTipping.isChecked = Configuration.getEnableTip(applicationContext).first()
                toggleTipping.setOnCheckedChangeListener { _, isChecked ->
                    saveEnableTip(isChecked)
                }
                val btnClose = findViewById<FloatingActionButton>(R.id.btnClose)
                btnClose.setOnClickListener {
                    finish()
                }
            }
        }

        logInfo("DMG_POS MainActivity", "onTerminal value: $isPosOnTerminal")


        if(!isPosOnTerminal){
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            finish()
        }

    }
    private fun saveEnableTip(isChecked: Boolean) {
        lifecycleScope.launch {
            try {
                Configuration.setEnableTip(applicationContext, isChecked)
            } catch (e: Exception) {
                // Log the exception
                e.printStackTrace()
            }
        }
    }

    //Keeping below code for future use


//    private fun isSatelliteInstalled(): Boolean {
//        val packageManager: PackageManager = applicationContext.packageManager
//        val satellitePackage = "au.com.dmg.axispay"
//        return try {
//            // Attempt to get application info for the package name
//            val packageInfo = packageManager.getPackageInfoCompat(satellitePackage, 0)
//            // If packageInfo is not null, the package is installed
//            packageInfo != null
//        } catch (e: PackageManager.NameNotFoundException) {
//            // Package not found
//            false
//        }
//    }
//
//    private fun PackageManager.getPackageInfoCompat(packageName: String, flags: Int = 0): PackageInfo =
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
//        } else {
//            @Suppress("DEPRECATION") getPackageInfo(packageName, flags)
//        }
}
