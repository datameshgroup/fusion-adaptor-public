package au.com.dmg.fusionadaptor.screens

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import au.com.dmg.fusion.Message
import au.com.dmg.fusion.SaleToPOI
import au.com.dmg.fusion.client.FusionClient
import au.com.dmg.fusion.data.ErrorCondition
import au.com.dmg.fusion.data.MessageCategory
import au.com.dmg.fusion.data.PaymentType
import au.com.dmg.fusion.exception.FusionException
import au.com.dmg.fusion.request.SaleToPOIRequest
import au.com.dmg.fusion.request.aborttransactionrequest.AbortTransactionRequest
import au.com.dmg.fusion.request.loginrequest.LoginRequest
import au.com.dmg.fusion.request.paymentrequest.PaymentRequest
import au.com.dmg.fusion.request.transactionstatusrequest.MessageReference
import au.com.dmg.fusion.response.SaleToPOIResponse
import au.com.dmg.fusionadaptor.R
import au.com.dmg.fusionadaptor.RequestBuilder
import au.com.dmg.fusionadaptor.ResponseBuilder
import au.com.dmg.fusionadaptor.constants.Timeout
import au.com.dmg.fusionadaptor.databinding.TransactionProgressActivityBinding
import au.com.dmg.fusionadaptor.datastore.Configuration
import au.com.dmg.fusionadaptor.utils.DatameshUtils.Companion.logError
import au.com.dmg.fusionadaptor.utils.DatameshUtils.Companion.logInfo
import au.com.dmg.fusionadaptor.utils.FusionMessageHandler
import au.com.dmg.fusionadaptor.utils.FusionMessageResponse
import au.com.dmg.fusionadaptor.utils.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull


class TransactionProgressActivity : AppCompatActivity() {
    private lateinit var binding: TransactionProgressActivityBinding
    private lateinit var fusionClient: FusionClient
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val TAG = "TransactionProgressActivity"

    // Constants

    private var timerJob: Job? = null

    // Placeholders
    private var currentTransaction: MessageCategory? = null
    private var headerDisplay = "PROCESSING TRANSACTION"
    private var paymentRequest: PaymentRequest? = null
    private var refundRequest: PaymentRequest? = null
    private var loginRequest: LoginRequest? = null
    private var saleToPOIRequest: SaleToPOIRequest? = null
    private var saleToPOIResponse: SaleToPOIResponse? = null //Important for POS response!!
    private var _result = RESULT_OK
    private var currentServiceID = ""

    var waitForConnection = true
    var inErrorHandling = false
    var showResultPage = true

    enum class ConnectionStatus {
        NOT_CONNECTED,
        OK,
        ERROR,
        NO_PAIRING_DATA,
        NO_NETWORK_CONNECTION
    }

    private suspend fun connectionUI(): ConnectionStatus {
        var connectionStatus: ConnectionStatus = ConnectionStatus.NOT_CONNECTED
        try {
            withTimeout(Timeout.TransactionConnection) {
                while (waitForConnection) {
                    try {

                        val isConnected = withContext(Dispatchers.Default) {
                            fusionClient.connect()
                            fusionClient.isConnected
                        }

                        withContext(Dispatchers.Main) {
                            if (isConnected) {
                                initTransactionUI()
                            } else {
                                // Connection not yet established, update connecting UI
                                updateConnectingUI()
                            }
                        }

                        // Exit the loop if connected
                        if (isConnected) {
                            return@withTimeout
                        }

                    } catch (e: FusionException) {
                        // Handle FusionException
                        Logger.logException("TransactionProgressActivity", "connectionUI", e)
//                        logError(TAG, "connectionUI : ${e.message}. Will retry.")
                        withContext(Dispatchers.Main) {
                            updateConnectingUI()
                        }
                        delay(Timeout.TransactionRetryDelay)
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            connectionStatus = ConnectionStatus.ERROR
            // Handle timeout
            Logger.logException("TransactionProgressActivity", "connectionUI", e)
//            logError(TAG, "TimeoutCancellationException: ${e.message}")
        }
        if(fusionClient.isConnected){
            connectionStatus = ConnectionStatus.OK
        }

        return connectionStatus
    }


    private fun updateConnectingUI() {
        val connectingText = getString(R.string.connecting_to_datamesh)

        hideProgressCircle(false)
        binding.textViewUiDetails.visibility = View.INVISIBLE
        //binding.textTimer.visibility = View.INVISIBLE
        binding.btnCancel.setOnClickListener {
            Logger.logEvent("TransactionProgressActivity", "Cancel button clicked")
            lifecycleScope.launch(Dispatchers.Main) {
                startActivityFailure(
                    ErrorCondition.Aborted,
                    "TRANSACTION TIMEOUT",
                    "Please check transaction history on terminal"
                )
            }
        }

        binding.textViewUiHeader.text = "$connectingText..."
    }

    override fun onBackPressed() {
    // super.onBackPressed();
    // Not calling **super**, disables back button in current screen.
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = TransactionProgressActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val poiID : String = Configuration.getPoiId(applicationContext).first()
                if(poiID.isNotEmpty()) {
                    Logger.initiateNewSession(poiID)
                }
            } catch (e:Exception) {
              Logger.logException("TransactionProgressActivity","onCreate", e)
            }
            handleIntent()
            processFusionRequest()
        }
    }

    private suspend fun processFusionRequest() {
        when (connectToFusionClient()) {
            ConnectionStatus.OK -> processTransaction()
            ConnectionStatus.NO_PAIRING_DATA -> handlePOSNotPairedFailure()
            ConnectionStatus.NO_NETWORK_CONNECTION -> handleNoNetworkConnection()
            else -> handleConnectionFailure()
        }
    }
    private fun handleConnectionFailure() {
        handleFailure(ErrorCondition.UnreachableHost, "CONNECTION FAILURE", "Error during connecting to Datamesh Host")
    }

    private fun handlePOSNotPairedFailure(){
        handleFailure(ErrorCondition.UnreachableHost, "POS NOT PAIRED WITH ANY TERMINAL", "Please pair POS with terminal")
    }

    private fun handleNoNetworkConnection(){
        handleFailure(ErrorCondition.UnreachableHost, "NETWORK CONNECTION ERROR", "Please check your network connectivity")
    }

    private fun handleFailure(errorCondition: ErrorCondition, displayMessage: String, additionalResponse: String){
        saleToPOIResponse = buildFailedPaymentResponse(errorCondition, displayMessage)
        // Handle UI update for connection failure
        lifecycleScope.launch(Dispatchers.Main) {
            startActivityFailure(
                errorCondition,
                displayMessage,
                additionalResponse
            )
        }
    }

    private suspend fun connectToFusionClient(): ConnectionStatus {
        return try {
            val connectionResult = CompletableDeferred<ConnectionStatus>()
            lifecycleScope.launch(Dispatchers.Default) {
                val useTestEnvironment = Configuration.getUseTestEnvironment(applicationContext).first()
                val pairingData = Configuration.getPairingData(applicationContext).first()
                if(pairingData.saleId.isNotEmpty() && pairingData.poiId.isNotEmpty() && pairingData.kek.isNotEmpty()) {
                    showResultPage = Configuration.getShowResultScreen(applicationContext).first()
                    fusionClient = FusionClient(useTestEnvironment)
                    fusionClient.setSettings(pairingData.saleId, pairingData.poiId, pairingData.kek)

                    connectionResult.complete(connectionUI())
                } else {
                    connectionResult.complete(ConnectionStatus.NO_PAIRING_DATA)
                }
            }
            connectionResult.await()
        } catch (e: Exception) {
            Logger.logException("TransactionProgressActivity","connectToFusionClient",e)
            ConnectionStatus.ERROR
        }
    }

    private suspend fun handleIntent() {
        val messageJson = intent.getStringExtra("SaleToPOIJson")

        if (messageJson.isNullOrBlank()) {
            handleInvalidRequest()
            return
        }

        saleToPOIRequest = Message.fromJson(messageJson).request
        currentTransaction = saleToPOIRequest?.messageHeader?.messageCategory
        currentServiceID = saleToPOIRequest?.messageHeader?.serviceID.orEmpty()
    }

    private suspend fun handleInvalidRequest() {
        logError(TAG, "SaleToPOIJson received is null or empty")
        startActivityFailure(ErrorCondition.MessageFormat, "Invalid Request Received by Datamesh")
    }

    private fun processTransaction() {
        lifecycleScope.launch(Dispatchers.Default) {
            currentTransaction?.let { transaction ->
                when (transaction) {
                    MessageCategory.Login -> processLogin()
                    MessageCategory.Payment -> processPaymentType()
                    MessageCategory.TransactionStatus -> processTransactionRequest(saleToPOIRequest!!)
                    else -> handleUnknownTransactionCategory()
                }
            }
        }
    }

    private suspend fun processLogin() {
        headerDisplay = "LOGGING IN"
        loginRequest = saleToPOIRequest?.loginRequest
        withContext(Dispatchers.Main) {
            binding.textViewUiHeader.text = headerDisplay
        }

        logInfo(TAG, "Sending Login Request: ${loginRequest?.toJson()}")

        try{
            val fmr = withContext(Dispatchers.Default) {
                try {
                    Logger.logFusionRequest("TransactionProgressActivity","processLogin", "sendMessage", loginRequest?.toJson(), null)
                    fusionClient.sendMessage(loginRequest)
                } catch (fe: FusionException) {
                    // Connection is closed. reconnect
                    logInfo(TAG, "Login Request Error $fe.")
                }
                getResponse(Timeout.TransactionLogin)
            }

            Logger.logFusionResponse("TransactionProgressActivity","processLogin", fmr?.toString())

            fmr?.let {
                handleResponseMessage(it)
            }
        } catch (e: TimeoutCancellationException) {
            logError(TAG, "Login Request: ${e.message}.")
        } catch (e: Exception) {
            logError(TAG, "processLogin: ${e.message}")
        }
    }

    private suspend fun processPaymentType() {
        when (saleToPOIRequest?.paymentRequest?.paymentData?.paymentType) {
            PaymentType.Normal -> {
                paymentRequest = saleToPOIRequest?.paymentRequest
                headerDisplay = "PROCESSING PAYMENT"
                withContext(Dispatchers.Main) {
                    binding.textViewUiHeader.text = headerDisplay
                    binding.btnCancel.setOnClickListener {
                        lifecycleScope.launch {
                            Logger.logEvent("TransactionProgressActivity", "Cancel button clicked")
//                            logInfo(TAG, "Tapped cancel on Payment Processing.")
                            sendAbortRequest(currentServiceID, "Cancelled on POS")
                        }
                    }
                }
                processPayment(paymentRequest!!)
            }

            PaymentType.Refund -> {
                refundRequest = saleToPOIRequest?.paymentRequest
                headerDisplay = "PROCESSING REFUND"
                withContext(Dispatchers.Main) {
                    binding.textViewUiHeader.text = headerDisplay
                    binding.btnCancel.setOnClickListener {
                        lifecycleScope.launch {
                            Logger.logEvent("TransactionProgressActivity", "Cancel button clicked")
                            logInfo(TAG, "Tapped cancel on Refund Processing.")
                            sendAbortRequest(currentServiceID, "Cancelled on POS")
                        }
                    }
                }
                processPayment(refundRequest!!)
            }

            else -> {
                logError(TAG, "PaymentType Unhandled")
                startActivityFailure(ErrorCondition.MessageFormat, "PaymentType unhandled")
            }
        }
    }

    private suspend fun handleUnknownTransactionCategory() {
        logError(TAG, "MessageCategory unhandled")
        startActivityFailure(ErrorCondition.MessageFormat, "MessageCategory unhandled")
    }

    private fun buildAbortRequest(
        referenceServiceID: String?,
        abortReason: String
    ): AbortTransactionRequest {
        val messageReference = MessageReference.Builder()
            .messageCategory(MessageCategory.Abort)
            .serviceID(referenceServiceID).build()
        return AbortTransactionRequest(messageReference, abortReason)
    }

    private fun buildFailedPaymentResponse(errorCondition: ErrorCondition,
                                            additionalResponse: String
    ): SaleToPOIResponse?{
       return saleToPOIRequest?.let {
           ResponseBuilder.generateFailedTransactionResponse(errorCondition, additionalResponse,
               it
           )
       }
    }

    private suspend fun startActivityFailure(
        errorCondition: ErrorCondition,
        additionalResponse: String
    ) {
        startActivityFailure(errorCondition, "TRANSACTION FAILED", additionalResponse)
    }

    private suspend fun startActivityFailure(
        errorCondition: ErrorCondition,
        displayMessage: String?,
        additionalResponse: String
    ) {
        withContext(Dispatchers.Default){
            val stubFmr = FusionMessageResponse()
            stubFmr.setMessage(false, displayMessage, errorCondition, additionalResponse)
            handleResponseMessage(stubFmr)
        }
    }

    private fun dpToPixels(dp: Int): Int {
        return (dp * Resources.getSystem().displayMetrics.density).toInt()
    }
    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    private suspend fun handleResponseMessage(
        fmr: FusionMessageResponse?
    ) {
        withContext(Dispatchers.Main) {
            if (fmr?.isSuccessful == true) {
                _result = RESULT_OK

                binding.textViewUiHeader.apply {
                    visibility = View.VISIBLE
                    text = fmr.displayMessage
                }

                //UI update to put the result header in the middle
                val marginTopInDp = 100
                val marginTopInPixels = dpToPixels(marginTopInDp)
                binding.trHeader.apply {
                    setBackgroundColor(getColor(R.color.adaptorHeaderGreen))
                    updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        topMargin = marginTopInPixels
                    }
                }


                binding.textViewUiDetails.visibility = View.INVISIBLE
                binding.textViewUiExtraDetails.visibility = View.GONE
            } else {
                _result = RESULT_CANCELED

                binding.textViewUiHeader.apply {
                    visibility = View.VISIBLE
                    text = fmr?.displayMessage ?: "Unknown Error" //TODO
                }
                binding.textViewUiDetails.apply {
                    setTextColor(getColor(R.color.adaptorRed))
                    visibility = View.VISIBLE
                    text = fmr?.errorCondition.toString()
                }
                binding.textViewUiExtraDetails.apply {
                    visibility = View.VISIBLE
                    text = fmr?.additionalResponse
                }

                //UI update to put the result header in the middle
                val marginTopInDp = 100
                val marginTopInPixels = dpToPixels(marginTopInDp)
                binding.trHeader.apply {
                    setBackgroundColor(getColor(R.color.adaptorRed))
                    updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        topMargin = marginTopInPixels
                    }
                }


            }
        }
        displayResponse()
    }

    private suspend fun displayResponse() {
        if(showResultPage){
            withContext(Dispatchers.Main) {
                hideProgressCircle(true)
                binding.textViewUiHeader.setTextColor(Color.parseColor("#ffffff"));
                binding.btnCancel.visibility = View.GONE
                //binding.textTimer.text = "0"
                //binding.textTimer.visibility = View.INVISIBLE

                binding.btnOK.apply {
                    Logger.logEvent("TransactionProgressActivity", "OK button clicked")
                    visibility = View.VISIBLE
                    setOnClickListener {
                        val resultIntent = Intent(intent.action)
                        if(saleToPOIResponse != null){
                            val message = Message(saleToPOIResponse)
                            logInfo(TAG, "finalUI SaleToPOIRequest : ${message.toJson()} ")
                            resultIntent.putExtra("SaleToPOIJson",message.toJson())
                        }
                        setResult(_result, resultIntent)
                        finish()
                    }
                }

            }
        }
        else{
            val resultIntent = Intent(intent.action)
            if(saleToPOIResponse != null){
                val message = Message(saleToPOIResponse)
                logInfo(TAG, "finalUI (not displayed) SaleToPOIRequest : ${message.toJson()} ")
                resultIntent.putExtra("SaleToPOIJson",message.toJson())
            }
            setResult(_result, resultIntent)
            finish()
        }
    }

    private fun initTransactionUI() {
        hideProgressCircle(false)
        binding.apply {
            textViewUiHeader.visibility = View.VISIBLE
            textViewUiHeader.text = headerDisplay
            textViewUiDetails.visibility = View.VISIBLE
            textViewUiDetails.text = getString(R.string.waiting_for_poi_response)
        }
        when(currentTransaction){
            MessageCategory.TransactionStatus -> {
                binding.btnCancel.setOnClickListener {
                    Logger.logEvent("TransactionProgressActivity", "Cancel button clicked")

                    lifecycleScope.launch(Dispatchers.Default) {
                        startActivityFailure(
                            ErrorCondition.Aborted,
                            "TRANSACTION INTERRUPTED", //TODO
                            "Please check transaction history on terminal"
                        )
                    }
                }
            }
            else -> {
                //TODO
            }
        }
    }

    private fun hideProgressCircle(doHide: Boolean) {
//        withContext(Dispatchers.Main)  {
        if (doHide) {
            binding.progressAnimation.visibility = View.INVISIBLE

        } else {
            binding.progressAnimation.apply {
                visibility = View.VISIBLE

                val uiModeManager =
                    context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
                val mode = uiModeManager.nightMode
                if (mode == UiModeManager.MODE_NIGHT_YES) {
                    setAnimation("processing.json")
                } else if (mode == UiModeManager.MODE_NIGHT_NO) {
                    setAnimation("processing_lite.json")
                }
                loop(true)
                playAnimation()
            }
        }
//        }
    }

    private suspend fun sendAbortRequest(serviceID: String?, abortReason: String) {

        lifecycleScope.launch(Dispatchers.Main) {
            binding.textViewUiHeader.text = getString(R.string.aborting_transaction_header)
            binding.textViewUiDetails.text = "Please wait"
            binding.btnCancel.visibility = View.INVISIBLE
            //binding.textTimer.visibility = View.INVISIBLE
        }

        withContext(Dispatchers.Default) {
            try {
                val abortTransactionPOIRequest = buildAbortRequest(serviceID, abortReason)
                Logger.logFusionRequest("TransactionProgressActivity","sendAbortRequest","sendMessage", abortTransactionPOIRequest.toJson(), null)
                fusionClient.sendMessage(abortTransactionPOIRequest)
            } catch (e: FusionException) {
                Logger.logException("TransactionProgressActivity","sendAbortRequest",e)
            }
        }
    }


    private suspend fun processPayment(paymentRequest: PaymentRequest) {
        logInfo(TAG, "Sending Payment Request: ${paymentRequest.toJson()}")
        var fmr: FusionMessageResponse? = null

        try {
            withContext(Dispatchers.Default) {
                try {
                    Logger.logFusionRequest("TransactionProgressActivity","processPayment", "sendMessage", paymentRequest.toJson(), currentServiceID)
                    fusionClient.sendMessage(paymentRequest, currentServiceID)
                } catch (fe: FusionException) {
                    // Connection is closed. reconnect
                    Logger.logException("TransactionProgressActivity","processPayment",fe)
                }

                // Loop until fmr is not null
                while (fmr == null) {
                    timerJob = GlobalScope.startTimer(Timeout.TransactionPayment)
                    fmr = getResponse(Timeout.TransactionPayment)
                    timerJob?.cancelAndJoin()
                }
            }

            fmr?.let {
                handleResponseMessage(it)
            }
        } catch (e: TimeoutCancellationException) {
            Logger.logException("TransactionProgressActivity","processPayment",e)
            doErrorHandling(currentServiceID, "Payment Timeout")
        } catch (e: FusionException) {
            Logger.logException("TransactionProgressActivity","processPayment",e)
            doErrorHandling(currentServiceID, "Network Disconnection")
        } catch (e: Exception) {
            Logger.logException("TransactionProgressActivity","processPayment",e)
        } finally {
            timerJob?.cancelAndJoin()
        }
    }


    private suspend fun doErrorHandling(serviceID: String, abortReason: String) {
        inErrorHandling = true

        withContext(Dispatchers.Default){
            currentTransaction = MessageCategory.Abort
            if (abortReason !== "") {
                sendAbortRequest(serviceID, abortReason)
            }
            val transactionStatusRequest = RequestBuilder.buildTransactionStatusRequest(serviceID)
            processTransactionRequest(transactionStatusRequest)
        }

    }


    private suspend fun processTransactionRequest(transactionStatusRequest: SaleToPOIRequest) {
        currentTransaction = MessageCategory.TransactionStatus
        withContext(Dispatchers.Main) {
            headerDisplay = "CHECKING TRANSACTION STATUS"
            binding.textViewUiHeader.text = headerDisplay
            //binding.textTimer.visibility = View.VISIBLE
            binding.btnCancel.visibility = View.VISIBLE
            binding.btnCancel.setOnClickListener {
                Logger.logEvent("TransactionProgressActivity", "Cancel button clicked")
                lifecycleScope.launch(Dispatchers.Main) {
                    startActivityFailure(
                        ErrorCondition.Aborted,
                        "TRANSACTION TIMEOUT",
                        "Please check transaction history on terminal"
                    )
                }

            }

        }

        var remainingTime = Timeout.TransactionErrorHandling
        var finalFmr: FusionMessageResponse? = null
        timerJob = GlobalScope.startTimer(Timeout.TransactionErrorHandling)
        while (finalFmr == null) {
            try {
                var fmr: FusionMessageResponse?
                withTimeout(remainingTime) {
                    withContext(Dispatchers.Default) {
                        try {
                            Logger.logFusionRequest("TransactionProgressActivity","processTransactionRequest","sendMessage", transactionStatusRequest.toJson(), null)
                            fusionClient.sendMessage(transactionStatusRequest)
                        } catch (fe: FusionException) {
                            Logger.logException("TransactionProgressActivity","processTransactionRequest",fe)
                        }
                        fmr = getResponse(remainingTime)
                    }

                    if (fmr != null) {
                        logInfo(TAG, "Received Transaction Status Response.")
                        if (fmr?.errorCondition == ErrorCondition.InProgress) {
                            logInfo(TAG, "Received InProgress Response. $remainingTime")
                        } else {
                            finalFmr = fmr
                            handleResponseMessage(finalFmr)
                        }
                    }
                }

            } catch (e: TimeoutCancellationException) {
                Logger.logException("TransactionProgressActivity", "processTransactionRequest", e)
                timerJob?.cancelAndJoin()
                startActivityFailure(
                    ErrorCondition.Aborted,
                    "TRANSACTION TIMEOUT",
                    "Please check transaction history on terminal"
                )
                break
            } catch (e: Exception) {
                Logger.logException("TransactionProgressActivity", "processTransactionRequest", e)
                // Handle other exceptions
                timerJob?.cancelAndJoin()
                startActivityFailure(
                    ErrorCondition.Aborted,
                    "TRANSACTION TIMEOUT",
                    "Please check transaction history on terminal"
                )
                break
            }
            delay(Timeout.TransactionRetryDelay)
            remainingTime -= Timeout.TransactionRetryDelay
        }
    }

    private suspend fun getResponse(timeout: Long): FusionMessageResponse? {
        var fusionMessageResponse: FusionMessageResponse?=null
        logInfo(TAG, "Waiting for $currentTransaction Response")

        try {
            withTimeout(timeout) {
                while (isActive) { // Continue until a valid response is received or an exception occurs
                    // Use withTimeoutOrNull for the suspending function
                    val saleToPOI: SaleToPOI = withTimeoutOrNull(timeout) {
                        try {
                            fusionClient.readMessage()
                        } catch (e: FusionException) {
                            logInfo(TAG, "Stopped listening to message. Reason:\n $e")
                            throw e
//                            null
                        }
                    } ?: continue

                    val fmh = FusionMessageHandler()
                    var fmr: FusionMessageResponse?

                    when (saleToPOI) {
                        is SaleToPOIRequest -> {
                            Logger.logFusionResponse("TransactionProgressActivity", "getResponse", "SaleToPOIRequest | " + saleToPOI.toJson())
                            if(!inErrorHandling){
                                fmr = fmh.handle((saleToPOI as SaleToPOIRequest?)!!)
                                /* DISPLAY SaleToPOIRequest*/
                                val finalFmr: FusionMessageResponse = fmr
                                withContext(Dispatchers.Main) {
                                    binding.textViewUiDetails.text = finalFmr.displayMessage
                                }
                                //TODO withTimeout
//                                continue
                                //Return a null fmr to restart payment timer after a display request
                                return@withTimeout null
                                break
                            }
                        }

                        is SaleToPOIResponse -> {
                            Logger.logFusionResponse("TransactionProgressActivity", "getResponse", "SaleToPOIResponse | " + saleToPOI.toJson())

                            fmr = fmh.handle((saleToPOI as SaleToPOIResponse?)!!)

                            // Ignore response if it's not the current transaction
                            if (fmr.messageCategory != currentTransaction && fmr.messageCategory != MessageCategory.Event) {
                                logInfo(TAG, "Ignoring Response above... waiting for " + currentTransaction + ", received " + fmr.messageCategory)
                                continue // Reset the loop to read the next message
                            }

                            if (fmr.messageCategory == MessageCategory.Event) {
                                val spr = fmr.saleToPOI as SaleToPOIResponse?
                                val eventNotification = spr!!.eventNotification
                                logInfo(TAG,"Ignoring Event: ${spr.toJson()} \n Event Details: ${eventNotification!!.eventDetails}"
                                )
                                continue // Reset the loop to read the next message
                            }
                            saleToPOIResponse = saleToPOI
                            fusionMessageResponse = fmr
                            break // Exit the loop and return the response
                        }
                    }

                }
            }
        } catch (e: TimeoutCancellationException) {
            Logger.logException("TransactionProgressActivity", "getResponse", e)
            throw e
        } catch(e: FusionException){
            Logger.logException("TransactionProgressActivity", "getResponse", e)
            if(currentTransaction!=MessageCategory.TransactionStatus){
                throw e
            }
        }
        logInfo(TAG, "Stopped waiting for $currentTransaction Response")
        return fusionMessageResponse
    }

    private fun CoroutineScope.startTimer(timeout: Long): Job {
        val startTime = System.currentTimeMillis()
        return launch {
            try {
                logInfo(TAG, "timer start")
                while (isActive) {
                    // Check for cancellation before delay
                    if (!isActive) break

                    delay(1000) // Print remaining time every second

                    val elapsedTime = (System.currentTimeMillis() - startTime)
                    val secondsRemaining = (timeout - elapsedTime) / 1000
                    withContext(Dispatchers.Main) {
                        //binding.textTimer.text = secondsRemaining.toString()
                    }

                    if (secondsRemaining <= 0) {
                        // If secondsRemaining is less than or equal to 0, cancel the timer
                        cancel()
                    }
                }
            } finally {
                logInfo(TAG, "timer done")
            }
        }
    }

}
