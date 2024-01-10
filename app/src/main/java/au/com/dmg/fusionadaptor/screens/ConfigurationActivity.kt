package au.com.dmg.fusionadaptor.screens

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import au.com.dmg.fusionadaptor.databinding.ConfigurationActivityBinding
import au.com.dmg.fusionadaptor.datastore.Configuration
import au.com.dmg.fusionadaptor.datastore.Configuration.getConfiguration
import au.com.dmg.fusionadaptor.utils.DatameshUtils.Companion.logError
import au.com.dmg.fusionadaptor.utils.Logger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ConfigurationActivity : AppCompatActivity() {
    private lateinit var binding: ConfigurationActivityBinding
    var previousClass: Class<*>? = null
    private val TAG = "ConfigurationActivity"
    private val  showDatameshValues = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ConfigurationActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        previousClass = intent.javaClass

        getValues()

        binding.btnExit.setOnClickListener {
            Logger.logEvent("ConfigurationActivity", "Exit button clicked")
            finish()
        }

        binding.btnSave.setOnClickListener {
            Logger.logEvent("ConfigurationActivity", "Save button clicked")
            saveSettings()
        }
    }

    private fun getValues() {
        // Retrieve configuration data
        lifecycleScope.launch {
            try {
                val config = getConfiguration(applicationContext).first()
                // Update UI with configuration data
                binding.txtSalesID.setText(config.saleId)
                binding.txtPOIID.setText(config.poiId)
                binding.txtKEK.setText(config.kek)
                binding.txtProviderIdentifiction.text = config.providerIdentification
                binding.txtApplicationName.text = config.applicationName
                binding.txtSoftwareVersion.text = config.softwareVersion
                binding.txtCertificationCode.text = config.certificationCode

                if (showDatameshValues) {
                    binding.layoutDatameshProvidedSettings.visibility = View.VISIBLE
                } else {
                    binding.layoutDatameshProvidedSettings.visibility = View.INVISIBLE
                }
            } catch (e: Exception) {
                logError(TAG, e.toString())
                Toast.makeText(applicationContext, "Error retrieving configuration", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveSettings() {
        try {
            val updatedConfig = Configuration.ConfigurationData(
                saleId = binding.txtSalesID.text.toString(),
                poiId = binding.txtPOIID.text.toString(),
                kek = binding.txtKEK.text.toString(),
                posName = binding.txtPosName.toString(),
                providerIdentification = binding.txtProviderIdentifiction.text.toString(),
                applicationName = binding.txtApplicationName.text.toString(),
                softwareVersion = binding.txtSoftwareVersion.text.toString(),
                certificationCode = binding.txtCertificationCode.text.toString()
            )

            lifecycleScope.launch {
                try {
                    Configuration.updateConfiguration(applicationContext, updatedConfig)
                    Logger.log("ConfigurationActivity","saveSettings", "Save updated settings: SaleID (${updatedConfig.saleId}), POIID (${updatedConfig.poiId}), KEK (${updatedConfig.kek}, POS Name (${updatedConfig.posName}))")
                    Toast.makeText(applicationContext, "Settings Updated", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Logger.logException("ConfigurationActivity","saveSettings", e)
                    // Handle exceptions (e.g., logging, displaying an error message)
                    Toast.makeText(applicationContext, "Error updating configuration", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Logger.logException("ConfigurationActivity","saveSettings", e)
            // Handle exceptions (e.g., logging, displaying an error message)
            Toast.makeText(applicationContext, "Error", Toast.LENGTH_SHORT).show()
        }
    }
}