package au.com.dmg.fusionadaptor.screens

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import au.com.dmg.fusionadaptor.databinding.SettingsActivityOldBinding
import au.com.dmg.fusionadaptor.utils.DatameshUtils.Companion.logInfo
import au.com.dmg.fusionadaptor.utils.Logger

class OldSettingActivity: AppCompatActivity()  {

    private lateinit var binding: SettingsActivityOldBinding
    private val TAG = "OldSettingActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingsActivityOldBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.btnViewConfig.setOnClickListener {
            Logger.logEvent("OldSettingActivity", "View config button clicked")
            val intent = Intent(this, ConfigurationActivity::class.java)
            startActivity(intent)
        }

        binding.btnPairTerminal.setOnClickListener {
            //TODO
            Logger.logEvent("OldSettingActivity", "Pair Terminal button clicked")
            logInfo(TAG,"btnPairTerminal clicked")
            val intent = Intent(this, PairingActivity::class.java)
            startActivity(intent)
        }

        binding.btnUnpairTerminal.setOnClickListener {
            //TODO
            Logger.logEvent("OldSettingActivity", "Unpair Terminal button clicked")
            val intent = Intent(this, PairingActivity::class.java)
            startActivity(intent)
        }

        binding.btnExitSettings.setOnClickListener {
            Logger.logEvent("OldSettingActivity", "Exit button clicked")
            finish()
        }
    }
}