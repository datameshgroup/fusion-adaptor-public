package au.com.dmg.fusionadaptor.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.lifecycle.lifecycleScope
import au.com.dmg.fusion.SaleToPOI
import au.com.dmg.fusion.client.FusionClient
import au.com.dmg.fusion.data.MessageCategory
import au.com.dmg.fusion.data.SaleCapability
import au.com.dmg.fusion.data.TerminalEnvironment
import au.com.dmg.fusion.exception.FusionException
import au.com.dmg.fusion.request.SaleTerminalData
import au.com.dmg.fusion.request.loginrequest.LoginRequest
import au.com.dmg.fusion.request.loginrequest.SaleSoftware
import au.com.dmg.fusion.response.SaleToPOIResponse
import au.com.dmg.fusion.util.PairingData
import au.com.dmg.fusionadaptor.R
import au.com.dmg.fusionadaptor.constants.Timeout
import au.com.dmg.fusionadaptor.databinding.PairingActivityBinding
import au.com.dmg.fusionadaptor.datastore.Configuration
import au.com.dmg.fusionadaptor.utils.ConnectionHelper
import au.com.dmg.fusionadaptor.utils.DatameshUtils.Companion.logError
import au.com.dmg.fusionadaptor.utils.DatameshUtils.Companion.logInfo
import au.com.dmg.fusionadaptor.utils.Logger
import au.com.dmg.fusionadaptor.utils.FusionMessageHandler
import au.com.dmg.fusionadaptor.utils.FusionMessageResponse
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID


class PairingActivity: AppCompatActivity() {
    private lateinit var binding: PairingActivityBinding
    
    private val TAG = "PairingActivity"

    var waitForConnection = true

    var receivedPOIID: String? = null
    //SaleID, unique to the POS instance. Autogenerate this once per POS instance.
    var saleId = ""
    var s:String = ""
    //PairingPOIID. This will be populated on the pairing response
    var p:String = ""
    //KEK. Autogenerate this once per POS instance.
    var k:String = ""
    lateinit var fusionClient: FusionClient
    private var useTestEnvironment: Boolean = false
    private lateinit var config: Configuration.ConfigurationData

    enum class ConnectionStatus {
        NOT_CONNECTED,
        OK,
        NO_NETWORK_CONNECTION,
        ERROR
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PairingActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)


        lifecycleScope.launch {
            try {
                useTestEnvironment = Configuration.getUseTestEnvironment(applicationContext).first()
                saleId = Configuration.getSaleId(applicationContext).first()
                config = Configuration.getConfiguration(applicationContext).first()

                fusionClient = FusionClient(useTestEnvironment)

                s = saleId.takeIf { it?.isNotBlank() == true } ?: UUID.randomUUID().toString()
                p = UUID.randomUUID().toString() //PairingPOIID
                k = PairingData.CreateKEK();

                logInfo(TAG,"Generated SaleID:$s")
                logInfo(TAG,"Generated POIID:$p")
                logInfo(TAG,"Generated KEK:$k")

                fusionClient.setSettings(s,p,k)

                generateQRCode()
            } catch (e: Exception) {
                Logger.logException("PairingActivity", "onCreate", e)
                // Handle exceptions (e.g., logging, displaying an error message)
                logError(TAG, e.toString())
                Toast.makeText(applicationContext, "Initialise POS first!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.progressCircle.isInvisible = true
        binding.txtPairingStatus.isInvisible = true

        binding.btnEnterManually.setOnClickListener {
            Logger.logEvent("PairingActivity", "Enter Manually clicked")
            val intent = Intent(this, ConfigurationActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.btnCancel.setOnClickListener{
            Logger.logEvent("PairingActivity", "OK Button clicked")
            finish()
        }

        binding.btnNext.setOnClickListener {
            Logger.logEvent("PairingActivity", "Login Button clicked")
            processLogin()
        }

        initialiseDiplayNexButton()
    }

    private fun processLogin() {
        lifecycleScope.launch(Dispatchers.Default) {
            val connectionStatus = connectToFusionClient()
            if (connectionStatus == ConnectionStatus.OK) {
                val loginRequest = buildInitialLoginRequest()
                withContext(Dispatchers.Main) {
                    binding.progressCircle.visibility = View.GONE
                    binding.txtPairingStatus.visibility = View.VISIBLE
                    binding.txtPairingStatus.text = "Logging in..."
                }

                logInfo(TAG, "Sending Login Request: ${loginRequest?.toJson()}")

                try{
                    val fmr = withContext(Dispatchers.Default) {
                        try {
                            val serviceID = UUID.randomUUID().toString()
                            Logger.logFusionRequest("PairingActivity","processLogin","sendMessage", loginRequest?.toJson(),serviceID)
                            fusionClient.sendMessage(loginRequest, serviceID)
                        } catch (fe: FusionException) {
                            // Connection is closed. reconnect
                            logInfo(TAG, "Login Request Error $fe.")
                        }
                        getResponse(Timeout.PairingLogin)
                    }

                    Logger.logFusionResponse("PairingActivity","processLogin", fmr?.toString())

                    fmr?.let {
                        handleLoginResponse(it)
                    }
                } catch (e: TimeoutCancellationException) {
                    Logger.logException("PairingActivity", "processLogin", e)
                    logError(TAG, "Login Request: ${e.message}.")
                    startActivityFailure("Pairing Failed", "Initial Login Timeout Failure")
                } catch (e: FusionException) {
                    Logger.logException("PairingActivity", "processLogin", e)
                    logError(TAG, "processLogin: ${e.message}")
                    startActivityFailure("Pairing Failed", "Initial Login Failure")
                } catch (e: Exception) {
                    Logger.logException("PairingActivity", "processLogin", e)
                    logError(TAG, "processLogin: ${e.message}")
                    startActivityFailure("Pairing Failed", "Initial Login Failure")
                }
            } else if(connectionStatus == ConnectionStatus.NO_NETWORK_CONNECTION){
                withContext(Dispatchers.Main) {
                    binding.progressCircle.visibility = View.GONE
                    binding.txtPairingStatus.visibility = View.VISIBLE
                    binding.txtPairingStatus.text = "No network connection"
                }
            } else {
                startActivityFailure("Pairing Failed", "Connection Failure.")
            }
        }
    }
    private suspend fun getResponse(timeout: Long): FusionMessageResponse? {
        var fusionMessageResponse: FusionMessageResponse?=null

        try {
            withTimeout(timeout) {
                while (isActive) {
                    val saleToPOI: SaleToPOI = withTimeoutOrNull(timeout) {
                        try {
                            fusionClient.readMessage()
                        } catch (e: FusionException) {
                            Logger.logException("PairingActivity", "getResponse", e)
                            logInfo(TAG, "Stopped listening to message. Reason:\n $e")
                            throw e
//                            null
                        }
                    } ?: continue

                    logInfo(TAG, "Message Received: $saleToPOI")

                    val fmh = FusionMessageHandler()
                    var fmr: FusionMessageResponse?

                    when (saleToPOI) {
                        is SaleToPOIResponse -> {
                            Logger.logFusionResponse("PairingActivity", "getResponse", saleToPOI.toJson())
                            logInfo(TAG, "Response received")
                            fmr = fmh.handle((saleToPOI as SaleToPOIResponse?)!!)

                            // Ignore response if it's not the current transaction
                            if (fmr.messageCategory != MessageCategory.Login) {
                                logInfo(TAG, "Ignoring Response above... waiting for Login Response received " + fmr.messageCategory)
                                continue // Reset the loop to read the next message
                            }

                            if (fmr.messageCategory == MessageCategory.Event) {
                                val spr = fmr.saleToPOI as SaleToPOIResponse?
                                val eventNotification = spr!!.eventNotification
                                logInfo(TAG,"Ignoring Event: ${spr.toJson()} \n Event Details: ${eventNotification!!.eventDetails}"
                                )
                                continue // Reset the loop to read the next message
                            }
                            fusionMessageResponse = fmr
                            break // Exit the loop and return the response
                        }
                    }

                }
            }
        } catch (e: TimeoutCancellationException) {
            Logger.logException("PairingActivity", "getResponse", e)
            throw e
        } catch(e: FusionException){
            Logger.logException("PairingActivity", "getResponse", e)
            throw e
        }

        Logger.log("PairingActivity", "getResponse", "returns $fusionMessageResponse")
        return fusionMessageResponse
    }

    private suspend fun connectToFusionClient(): ConnectionStatus {
        return try {
            if(ConnectionHelper.isNetworkConnected(applicationContext)) {
                val connectionResult = CompletableDeferred<ConnectionStatus>()
                connectionResult.complete(tryConnect())
                connectionResult.await()
            } else
                ConnectionStatus.NO_NETWORK_CONNECTION
        } catch (e: Exception) {
            Logger.logException("PairingActivity", "connectToFusionClient", e)
            ConnectionStatus.ERROR
        }
    }

    private suspend fun tryConnect(): ConnectionStatus {
        var connectionStatus: ConnectionStatus = ConnectionStatus.NOT_CONNECTED
        try {
            withContext(Dispatchers.Main) {
                binding.progressCircle.visibility = View.VISIBLE
                binding.txtPairingStatus.visibility = View.GONE
            }
            withTimeout(Timeout.PairingConnection) {
                while (waitForConnection) {
                    try {
                        val isConnected = withContext(Dispatchers.Default) {
                            fusionClient.connect()
                            fusionClient.isConnected
                        }
                        Logger.log("PairingActivity","tryConnect", "isConnected = $isConnected")

                        // Exit the loop if connected
                        if (isConnected) {
                            logInfo(TAG, "Connected.")
                            return@withTimeout
                        }
                    } catch (e: FusionException) {
                        Logger.logException("PairingActivity","tryConnect",e)
                        // Handle FusionException
                        logError(TAG, "connectionUI : ${e.message}. Will retry.")
                        withContext(Dispatchers.Main) {
                        }
                        delay(Timeout.PairingRetryDelay)
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            connectionStatus = ConnectionStatus.ERROR
            // Handle timeout
            Logger.logException("PairingActivity","tryConnect", e)
            logError(TAG, "TimeoutCancellationException: ${e.message}")
        }

        if(fusionClient.isConnected){
            connectionStatus = ConnectionStatus.OK
        }

        return connectionStatus
    }

    private fun buildInitialLoginRequest():LoginRequest? {
        val saleSoftware = SaleSoftware.Builder()
            .providerIdentification(config.providerIdentification)
            .applicationName(config.applicationName)
            .softwareVersion(config.softwareVersion)
            .certificationCode(config.certificationCode)
            .build()

        val saleTerminalData = SaleTerminalData.Builder()
            .terminalEnvironment(TerminalEnvironment.Attended)
            .saleCapabilities(
                listOf(
                    SaleCapability.CashierStatus,
                    SaleCapability.CustomerAssistance,
                    SaleCapability.PrinterReceipt
                )
            ) //
            .build()

        return LoginRequest.Builder()
            .dateTime(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(Date()))
            .operatorLanguage("en")
            .pairing(true)         //New for QR Pairing
            .saleTerminalData(saleTerminalData)
            .saleSoftware(saleSoftware)
            .build()
    }


    private fun handleLoginResponse(fmr: FusionMessageResponse) {
        runOnUiThread { binding.txtPairingStatus.text = fmr.displayMessage }
        receivedPOIID = fmr.saleToPOI?.messageHeader?.poiID.toString()

        val loginResponse = (fmr.saleToPOI as SaleToPOIResponse?)!!.loginResponse
        loginResponse?.response

        var errorCondition = "Unknown"
        var additionalResponse = "Invalid Response <NULL>"

        val intent = Intent(this, PairingResultActivity::class.java)
        if(fmr.isSuccessful==true){
            intent.apply {
                putExtra("isSuccessful", fmr.isSuccessful)
                putExtra("receivedPOI",receivedPOIID)
                putExtra("errorCondition","")
                putExtra("additionalResponse","")
            }

            val updatedPairingData = Configuration.PairingData(
                saleId = s,
                poiId = receivedPOIID.toString(),
                kek = k
            )

            lifecycleScope.launch {
                try {
                    Configuration.updatePairingData(applicationContext, updatedPairingData)
                    Logger.log("PairingActivity","handleLoginResponse","POS($s) paired with POID($receivedPOIID).");
                    logInfo(TAG, "Pairing data updated")
                } catch (e: Exception) {
                    Logger.logException("PairingActivity","handleLoginResponse",e);
                    logError(TAG, "Error updating pairing data. $e")
                }
            }

        }else{
            intent.apply {
                putExtra("isSuccessful", fmr.isSuccessful)
                putExtra("receivedPOI","")
                putExtra("errorCondition",errorCondition)
                putExtra("additionalResponse",additionalResponse)
            }
        }

        startActivity(intent)
        finish()
    }

    fun generateQRCode() {
        logInfo(TAG, "Generating QR Code...")

        val c = config.certificationCode //CertificationCode
        val n = config.posName //POS display name with at most 30 characters.

        val newPairingData = createPairingData(
            s,
            p,
            k,
            n,
            c,
        )

        val moshi = Moshi.Builder().build()
        val jsonAdapter = moshi.adapter(PairingData::class.java)
        val json = jsonAdapter.toJson(newPairingData)
        logInfo(TAG, json)
        Logger.log("PairingActivity","generateQRCode", "pairingData = $json")
        val qrCodeValue = genQRCode(json)

        binding.ivQrCode.setImageBitmap(qrCodeValue)
    }

    fun genQRCode(input: String): Bitmap? {
        val writer = QRCodeWriter()
        try {
            val bitMatrix: BitMatrix = writer.encode(input, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            return bmp
        } catch (e: WriterException) {
            Logger.logException("PairingActivity", "genQRCode", e)
            logError(TAG, "genQRCode ${e.printStackTrace()}")
            startActivityFailure("QR Code Generation", "Failed to generate QR")
            return null
        }
    }

    private fun createPairingData(saleID: String?, pairingPOIID: String?, kek: String?, posName: String?, certificationCode: String?, version: Int = 1): PairingData? {
        if (certificationCode == null) {
            logError(TAG, "Invalid pairing request. certificationCode is null empty")
            return null
        }

        return PairingData.Builder()
            .saleID(saleID)
            .pairingPOIID(pairingPOIID)
            .kek(kek)
            .certificationCode(certificationCode)
            .posName(posName)
            .version(version)
            .build()
    }

    fun startActivityFailure(errorCondition:String, additionalResponse:String){
        var intent = Intent(this, PairingResultActivity::class.java).apply {
            putExtra("isSuccessful", false)
            putExtra("errorCondition",errorCondition)
            putExtra("additionalResponse",additionalResponse)
        }
        startActivity(intent)
        finish()
    }

    private fun initialiseDiplayNexButton() {
        lifecycleScope.launch {
            delay(Timeout.PairingNextBtnDisplay)
            //set btnNext to primary button
            binding.btnNext.setBackgroundResource(R.drawable.button_primary_background)
            binding.btnNext.setTextColor(ContextCompat.getColor(applicationContext, R.color.dmgWhite))

            binding.btnNext.isEnabled = true;
            binding.btnNext.isClickable = true;
        }
    }

}