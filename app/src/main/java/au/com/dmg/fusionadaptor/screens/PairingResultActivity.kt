package au.com.dmg.fusionadaptor.screens

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import au.com.dmg.fusionadaptor.R
import au.com.dmg.fusionadaptor.databinding.PairingResultActivityBinding
import au.com.dmg.fusionadaptor.utils.Logger

class PairingResultActivity: AppCompatActivity() {
    private lateinit var binding: PairingResultActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PairingResultActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        Logger.logIntentsReceived("PairingResultActivity", "onCreate", intent)
        val isSuccessful = intent.getBooleanExtra("isSuccessful", false)

        if(isSuccessful){
            val receivedPOI = intent.getStringExtra("receivedPOI")
            binding.lblPairingResult.text = getString(R.string.pairing_successful)
            binding.trHeader.setBackgroundColor(Color.parseColor("#4CAF50"))

            binding.txtErrorCondition.apply {
                text = "Paired to POI:"
                setTextColor(getColor(R.color.adaptorGreen))
            }
            binding.txtAdditionalResponse.text = receivedPOI

        }else{
            val errorCondition = intent.getStringExtra("errorCondition")
            val additionalResponse = intent.getStringExtra("additionalResponse")

            binding.lblPairingResult.text = getString(R.string.pairing_failed)
            binding.trHeader.setBackgroundColor(Color.parseColor("#E91E63"))

            binding.txtErrorCondition.apply {
                text = errorCondition
                setTextColor(getColor(R.color.adaptorRed))
            }
            binding.txtAdditionalResponse.text = additionalResponse
        }

        binding.btnClose.setOnClickListener{
            Logger.logEvent("PairingResultActivity", "Close button clicked")
            finish()
        }



    }
}