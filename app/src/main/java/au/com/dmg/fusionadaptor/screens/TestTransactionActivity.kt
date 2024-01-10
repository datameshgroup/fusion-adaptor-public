package au.com.dmg.fusionadaptor.screens

import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import au.com.dmg.fusion.MessageHeader
import au.com.dmg.fusion.SaleToPOI
import au.com.dmg.fusion.client.FusionClient
import au.com.dmg.fusion.data.ErrorCondition
import au.com.dmg.fusion.data.MessageCategory
import au.com.dmg.fusion.data.PaymentInstrumentType
import au.com.dmg.fusion.data.PaymentType
import au.com.dmg.fusion.data.SaleCapability
import au.com.dmg.fusion.data.TerminalEnvironment
import au.com.dmg.fusion.data.UnitOfMeasure
import au.com.dmg.fusion.exception.FusionException
import au.com.dmg.fusion.request.SaleTerminalData
import au.com.dmg.fusion.request.SaleToPOIRequest
import au.com.dmg.fusion.request.aborttransactionrequest.AbortTransactionRequest
import au.com.dmg.fusion.request.loginrequest.LoginRequest
import au.com.dmg.fusion.request.loginrequest.SaleSoftware
import au.com.dmg.fusion.request.paymentrequest.AmountsReq
import au.com.dmg.fusion.request.paymentrequest.PaymentData
import au.com.dmg.fusion.request.paymentrequest.PaymentInstrumentData
import au.com.dmg.fusion.request.paymentrequest.PaymentRequest
import au.com.dmg.fusion.request.paymentrequest.PaymentTransaction
import au.com.dmg.fusion.request.paymentrequest.SaleData
import au.com.dmg.fusion.request.paymentrequest.SaleItem
import au.com.dmg.fusion.request.paymentrequest.SaleTransactionID
import au.com.dmg.fusion.request.transactionstatusrequest.MessageReference
import au.com.dmg.fusion.request.transactionstatusrequest.TransactionStatusRequest
import au.com.dmg.fusion.response.Response
import au.com.dmg.fusion.response.SaleToPOIResponse
import au.com.dmg.fusion.response.TransactionStatusResponse
import au.com.dmg.fusion.response.paymentresponse.PaymentResponse
import au.com.dmg.fusion.util.MessageHeaderUtil
import au.com.dmg.fusionadaptor.R
import au.com.dmg.fusionadaptor.constants.Timeout
import au.com.dmg.fusionadaptor.databinding.TestTransactionActivityBinding
import au.com.dmg.fusionadaptor.datastore.Configuration
import au.com.dmg.fusionadaptor.utils.DatameshUtils.Companion.logInfo
import au.com.dmg.fusionadaptor.utils.Logger
import au.com.dmg.fusionadaptor.utils.FusionMessageHandler
import au.com.dmg.fusionadaptor.utils.FusionMessageResponse
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.naming.ConfigurationException

//import javax.naming.ConfigurationException

//TODO: Fix - Timer is not starting if there's no connection
//TODO: Add timer for building messages
class TestTransactionActivity : AppCompatActivity() {
    private lateinit var binding: TestTransactionActivityBinding
    private lateinit var executorService: ExecutorService
    private val TAG = "TestTransactionActivity"
    var sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss") //
    private var pressedTime: Long = 0
    var providerIdentification: String? = null
    var applicationName: String? = null
    var softwareVersion: String? = null
    var certificationCode: String? = null
    var saleID: String? = null
    var poiID: String? = null
    var kek: String? = null
    var useTestEnvironment = true

    //Timer settings; Update as needed.
    var loginTimeout = Timeout.TransactionLogin
    var paymentTimeout = Timeout.TransactionPayment
    var errorHandlingTimeout = Timeout.TransactionErrorHandling
    var prevSecond // reference for counting second passed
            : Long = 0
    var waitingForResponse = false
    var secondsRemaining = 0
    var currentTransaction = MessageCategory.Login
    lateinit var currentServiceID: String
    lateinit var fusionClient: FusionClient
    var abortReason = ""

    fun initUI() {
        binding.editTextJsonLogs.setTextIsSelectable(true)
        binding.textViewReceipt.movementMethod = ScrollingMovementMethod()
    }

    private fun initFusionClient() {
        lifecycleScope.launch {
            val config = Configuration.getConfiguration(applicationContext).first()

            providerIdentification = config.providerIdentification
            applicationName = config.applicationName
            softwareVersion = config.softwareVersion
            certificationCode = config.certificationCode
            saleID = config.saleId
            poiID = config.poiId
            kek = config.kek
            fusionClient = FusionClient(useTestEnvironment) //need to override this in production
            fusionClient.setSettings(
                saleID,
                poiID,
                kek
            ) // replace with the Sale ID provided by DataMesh
        }
        }

    override fun onResume() {
        super.onResume()
        initFusionClient()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = TestTransactionActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        
        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        initFusionClient()
        initUI()
        binding.btnLogin.setOnClickListener(View.OnClickListener { v: View? ->
            Logger.logEvent("TestTransactionActivity", "Login button clicked")
            executorService = Executors.newSingleThreadExecutor()
            executorService.submit(Runnable { doLogin() })
        })
        binding.btnPurchase.setOnClickListener(View.OnClickListener { v: View? ->
            Logger.logEvent("TestTransactionActivity", "Purchase button clicked")
            executorService = Executors.newSingleThreadExecutor()
            executorService.submit(Runnable { doPayment() })
        })
        binding.btnRefund.setOnClickListener(View.OnClickListener { v: View? ->
            Logger.logEvent("TestTransactionActivity", "Refund button clicked")
            executorService = Executors.newSingleThreadExecutor()
            executorService.submit(Runnable { doRefund() })
        })
    }

    /* SaleToPOI Listener */
    private fun listen() {
        try {
            //Update timer text
            prevSecond = computeSecondsRemaining(prevSecond)
            var saleToPOI: SaleToPOI? = null
            saleToPOI = fusionClient.readMessage()
            if (saleToPOI == null) return
            log("Response Received: ${prettyPrintJson(saleToPOI)}")
            val fmh = FusionMessageHandler()
            var fmr: FusionMessageResponse? = null
            if (saleToPOI is SaleToPOIRequest) {
                fmr = fmh.handle((saleToPOI as SaleToPOIRequest?)!!)

                /* DISPLAY SaleToPOIRequest*/
                val finalFmr: FusionMessageResponse = fmr
                runOnUiThread { binding.textViewUiDetails.text = finalFmr.displayMessage }

                //Reset timer when a message is received (Not applicable to transaction status AKA error handling)
                if (currentTransaction == MessageCategory.Payment) {
                    secondsRemaining = (paymentTimeout / 1000).toInt() //Converting to seconds
                }
                waitingForResponse = true
            }
            if (saleToPOI is SaleToPOIResponse) {
                Logger.logFusionResponse("TestTransactionActivity", "listen", saleToPOI.toJson())
                logInfo(TAG, "LISTEN SaleToPOIResponse")
                fmr = fmh.handle((saleToPOI as SaleToPOIResponse?)!!)

                //Ignore response if it's not the current transaction
                if (fmr.messageCategory != currentTransaction && fmr.messageCategory != MessageCategory.Event) {
                    log("Ignoring Response above... waiting for " + currentTransaction + ", received " + fmr.messageCategory)
                    return
                }
                when (fmr.messageCategory) {
                    MessageCategory.Event -> {
                        val spr = fmr.saleToPOI as SaleToPOIResponse?
                        val eventNotification = spr!!.eventNotification
                        log("Ignoring Event below... ${prettyPrintJson(spr)} Event Details: ${eventNotification!!.eventDetails}")
                    }

                    MessageCategory.Login -> displayLoginResponseMessage(fmr)
                    MessageCategory.Payment -> displayPaymentResponseMessage(fmr)
                    MessageCategory.TransactionStatus -> handleTransactionResponseMessage(fmr)
                    else -> {
                        //TODO Unknown MessageCategory
                    }
                }
            }
        } catch (e: FusionException) {
            Logger.logException("TestTransactionActivity", "listen", e)
//            e.printStackTrace();
            // Should not loop if there's an error. e.g. Socket Disconnection
            endLog(String.format("Stopped listening to message. Reason:\n %s", e), true)
            if (currentTransaction != MessageCategory.TransactionStatus) {
                logInfo(TAG, "CURRENT SERVICE ID: " + currentServiceID)
                executorService!!.shutdownNow()
                executorService = Executors.newSingleThreadExecutor()
                executorService.submit(Runnable {
                    checkTransactionStatus(
                        currentServiceID,
                        "Websocket connection interrupted"
                    )
                })
            }
        }
    }

    private fun displayLoginResponseMessage(fmr: FusionMessageResponse) {
        endTransactionUi()
        runOnUiThread { binding.textViewUiHeader.text = fmr.displayMessage }
        waitingForResponse = false
    }

    private fun displayPaymentResponseMessage(fmr: FusionMessageResponse) {
        endTransactionUi()
        val paymentResponse = (fmr.saleToPOI as SaleToPOIResponse?)!!.paymentResponse
        val paymentType = paymentResponse!!.paymentResult!!.paymentType
        val paymentResult = paymentResponse.paymentResult
        runOnUiThread {
            if (paymentType == PaymentType.Normal) {
                binding.textViewUiHeader.text = fmr.displayMessage
                binding.responseAuthorizeAmountValue.setText(paymentResult!!.amountsResp!!.authorizedAmount.toString())
                binding.responseTipAmountValue.setText(paymentResult.amountsResp!!.tipAmount.toString())
                binding.responseSurchargeAmountValue.setText(paymentResult.amountsResp!!.surchargeAmount.toString())
                binding.responseMaskedPan.setText(paymentResult.paymentInstrumentData!!.cardData.maskedPAN)
                binding.responsePaymentBrand.setText(paymentResult.paymentInstrumentData!!.cardData.paymentBrand.toString())
                binding.responsePaymentBrand.setText(paymentResult.paymentInstrumentData!!.cardData.entryMode.toString())
                binding.responseServiceId.setText(fmr.saleToPOI!!.messageHeader.serviceID)
                // Receipt
                val paymentReceipt = paymentResponse.paymentReceipt!![0]
                val outputXHTML = paymentReceipt.receiptContentAsHtml
                binding.textViewReceipt.text = HtmlCompat.fromHtml(outputXHTML, 0)
            } else if (paymentType == PaymentType.Refund) {
                binding.textViewUiHeader.text = fmr.displayMessage
                binding.responseAuthorizeAmountValue.setText(paymentResult!!.amountsResp!!.authorizedAmount.toString())
                binding.responseTipAmountValue.setText("0.00")
                binding.responseSurchargeAmountValue.setText(paymentResult.amountsResp!!.surchargeAmount.toString())
                binding.responseMaskedPan.setText(paymentResult.paymentInstrumentData!!.cardData.maskedPAN)
                binding.responsePaymentBrand.setText(paymentResult.paymentInstrumentData!!.cardData.paymentBrand.toString())
                binding.responseEntryMode.setText(paymentResult.paymentInstrumentData!!.cardData.entryMode.toString())
                binding.responseServiceId.setText(fmr.saleToPOI!!.messageHeader.serviceID)
                // Receipt
                val paymentReceipt = paymentResponse.paymentReceipt!![0]
                val outputXHTML = paymentReceipt.receiptContentAsHtml
                binding.textViewReceipt.text = HtmlCompat.fromHtml(outputXHTML, 0)
            }
        }
        waitingForResponse = false
    }

    //Currently only called from transactionstatus response
    private fun displayPaymentResponseMessage(pr: PaymentResponse, mh: MessageHeader) {
        endTransactionUi()
        runOnUiThread {
            binding.textViewUiHeader.text =
                "PAYMENT " + pr.response.result.toString().uppercase(Locale.getDefault())
            val paymentResult = pr.paymentResult
            binding.responseAuthorizeAmountValue.setText(paymentResult!!.amountsResp!!.authorizedAmount.toString())
            binding.responseTipAmountValue.setText(paymentResult.amountsResp!!.tipAmount.toString())
            binding.responseSurchargeAmountValue.setText(paymentResult.amountsResp!!.surchargeAmount.toString())
            binding.responseMaskedPan.setText(paymentResult.paymentInstrumentData!!.cardData.maskedPAN)
            binding.responsePaymentBrand.setText(paymentResult.paymentInstrumentData!!.cardData.paymentBrand.toString())
            binding.responseEntryMode.setText(paymentResult.paymentInstrumentData!!.cardData.entryMode.toString())
            binding.responseServiceId.setText(mh.serviceID)

            //Receipt
            val paymentReceipt = pr.paymentReceipt!![0]
            val outputXHTML = paymentReceipt.receiptContentAsHtml
            binding.textViewReceipt.text = HtmlCompat.fromHtml(outputXHTML, 0)
        }
        waitingForResponse = false
    }

    // Only called when there is no repeated message response on the transaction status
    private fun displayTransactionResponseMessage(
        errorCondition: ErrorCondition,
        additionalResponse: String
    ) {
        endTransactionUi()
        runOnUiThread { binding.textViewUiHeader.text = "$errorCondition - $additionalResponse" }
    }

    private fun handleTransactionResponseMessage(fmr: FusionMessageResponse) {
        // TODO: handle transaction status response for others. currently designed for Payment only.
        var transactionStatusResponse: TransactionStatusResponse? = null
        var responseBody: Response? = null
        if (fmr.isSuccessful!!) {
            transactionStatusResponse =
                (fmr.saleToPOI as SaleToPOIResponse?)!!.transactionStatusResponse
            responseBody = transactionStatusResponse!!.response
            log(String.format("Transaction Status Result: %s ", responseBody.result))
            val paymentResponse =
                transactionStatusResponse.repeatedMessageResponse.repeatedResponseMessageBody.paymentResponse
            val paymentMessageHeader =
                transactionStatusResponse.repeatedMessageResponse.messageHeader
            displayPaymentResponseMessage(paymentResponse, paymentMessageHeader)
        } else if (fmr.errorCondition == ErrorCondition.InProgress) {
            log("Transaction in progress...")
            if (secondsRemaining > 10) {
                errorHandlingTimeout =
                    ((secondsRemaining - 10) * 1000).toLong() //decrement errorHandlingTimeout so it will not reset after waiting
                log(
                    """
    Sending another transaction status request after 10 seconds...
    Remaining seconds until error handling timeout: $secondsRemaining
    """.trimIndent()
                )
                try {
                    TimeUnit.SECONDS.sleep(10)
                    executorService!!.shutdownNow()
                    executorService = Executors.newSingleThreadExecutor()
                    executorService.submit(Runnable {
                        checkTransactionStatus(
                            currentServiceID,
                            ""
                        )
                    })
                } catch (e: InterruptedException) {
                    endLog(e)
                }
            }
        } else {
            transactionStatusResponse =
                (fmr.saleToPOI as SaleToPOIResponse?)!!.transactionStatusResponse
            responseBody = transactionStatusResponse!!.response
            endLog(
                String.format(
                    "Error Condition: %s, Additional Response: %s",
                    responseBody.errorCondition, responseBody.additionalResponse
                ), true
            )
            displayTransactionResponseMessage(
                responseBody.errorCondition,
                responseBody.additionalResponse
            )
        }
    }

    private fun doLogin() {
        try {
            currentServiceID = MessageHeaderUtil.generateServiceID()
            currentTransaction = MessageCategory.Login
            startTransactionUi()
            val loginRequest = buildLoginRequest()
            log(" Sending message to websocket server: ${prettyPrintJson(loginRequest)}")
            Logger.logFusionRequest("TestTransactionActivity","doLogin","sendMessage", loginRequest.toJson(), currentServiceID)
            fusionClient.sendMessage(loginRequest, currentServiceID)

            //Set timeout
            prevSecond = System.currentTimeMillis()
            secondsRemaining = (loginTimeout / 1000).toInt()

            // Loop for Listener
            waitingForResponse = true
            while (waitingForResponse) {
                listen()
                if (secondsRemaining < 1) {
                    endLog("Login Request Timeout...", true)
                    endTransactionUi()
                    break
                }
            }
        } catch (e: ConfigurationException) {
            endLog(e)
        }
    }

    private fun doPayment() {
        try {
            currentServiceID = MessageHeaderUtil.generateServiceID()
            currentTransaction = MessageCategory.Payment
            startTransactionUi()
            clearLog()
            val paymentRequest = buildPaymentRequest()
            log(
                """
    Sending message to websocket server: 
    ${prettyPrintJson(paymentRequest)}
    """.trimIndent()
            )
            Logger.logFusionRequest("TestTransactionActivity","doPayment","sendMessage", paymentRequest.toJson(), currentServiceID)
            fusionClient.sendMessage(paymentRequest, currentServiceID)

            // Set timeout
            prevSecond = System.currentTimeMillis()
            secondsRemaining = (paymentTimeout / 1000).toInt()
            waitingForResponse = true
            while (waitingForResponse) {
                listen()
                if (secondsRemaining < 1) {
                    endLog("Payment Request Timeout...", true)
                    abortReason = "Timeout"
                    checkTransactionStatus(currentServiceID, abortReason)
                    break
                }
            }
        } catch (e: ConfigurationException) {
            Logger.logException("TestTransactionActivity", "doPayment", e)
            endLog(String.format("Exception: %s", e), true)
            abortReason = "Other Exception"
            checkTransactionStatus(currentServiceID, abortReason)
        } catch (e: FusionException) {
            Logger.logException("TestTransactionActivity", "doPayment", e)
            endLog(String.format("FusionException: %s. Resending the Request...", e), true)
            // Continue the timer
            paymentTimeout = secondsRemaining * 1000L
            doPayment()
        }
    }

    private fun doAbort(serviceID: String?, abortReason: String) {
        endTransactionUi()
        runOnUiThread {
            binding.textViewUiHeader.text = "ABORTING TRANSACTION"
            binding.textViewUiDetails.text = ""
        }
        hideProgressCircle(false)
        val abortTransactionPOIRequest = buildAbortRequest(serviceID, abortReason)
        log(
            """
    Sending abort message to websocket server: 
    ${prettyPrintJson(abortTransactionPOIRequest)}
    """.trimIndent()
        )
        fusionClient!!.sendMessage(abortTransactionPOIRequest)
    }

    private fun checkTransactionStatus(serviceID: String?, abortReason: String) {
        try {
            currentTransaction = MessageCategory.TransactionStatus
            startTransactionUi()
            hideCancelBtn(true)
            if (abortReason !== "") {
                doAbort(serviceID, abortReason)
            }
            runOnUiThread { binding.textViewUiHeader.text = "CHECKING TRANSACTION STATUS" }
            val transactionStatusRequest = buildTransactionStatusRequest(serviceID)
            log(
                """
    Sending transaction status request to check status of payment... 
    ${prettyPrintJson(transactionStatusRequest)}
    """.trimIndent()
            )
            Logger.logFusionRequest("TestTransactionActivity","checkTransactionStatus","sendMessage", transactionStatusRequest?.toJson(), null)
            fusionClient.sendMessage(transactionStatusRequest)
            // Set timeout
            prevSecond = System.currentTimeMillis()
            secondsRemaining = (errorHandlingTimeout / 1000).toInt()
            waitingForResponse = true
            while (waitingForResponse) {
                listen()
                if (secondsRemaining < 1) {
                    endTransactionUi()
                    runOnUiThread {
                        binding.textViewUiHeader.text = "Time Out"
                        binding.textViewUiDetails.text = "Please check transaction history on terminal"
                        endLog("Transaction Status Request Timeout...", true)
                    }
                    break
                }
            }
        } catch (e: ConfigurationException) {
            throw RuntimeException(e)
        }
    }

    private fun doRefund() {
        try {
            currentServiceID = MessageHeaderUtil.generateServiceID()
            currentTransaction = MessageCategory.Payment
            startTransactionUi()
            clearLog()
            val refundRequest = buildRefundRequest()
            log(
                """
    Sending message to websocket server: 
    ${prettyPrintJson(refundRequest)}
    """.trimIndent()
            )
            fusionClient.sendMessage(refundRequest, currentServiceID)

            // Set timeout
            prevSecond = System.currentTimeMillis()
            secondsRemaining = (paymentTimeout / 1000).toInt()
            waitingForResponse = true
            while (waitingForResponse) {
                listen()
                if (secondsRemaining < 1) {
                    abortReason = "Timeout"
                    endLog("Refund Request Timeout...", true)
                    checkTransactionStatus(currentServiceID, abortReason)
                    break
                }
            }
        } catch (e: ConfigurationException) {
            Logger.logException("TestTransactionActivity", "doRefund", e)
            endLog(String.format("Exception: %s", e), true)
            abortReason = "Other Exception"
            checkTransactionStatus(currentServiceID, abortReason)
        } catch (e: FusionException) {
            Logger.logException("TestTransactionActivity", "doRefund", e)
            endLog(String.format("FusionException: %s. Resending the Request...", e), true)
            // Continue the timer
            paymentTimeout = secondsRemaining * 1000L
            doPayment()
        }
    }

    @Throws(ConfigurationException::class)
    private fun buildLoginRequest(): LoginRequest {
        val saleSoftware = SaleSoftware.Builder() //
            .providerIdentification(providerIdentification) //
            .applicationName(applicationName) //
            .softwareVersion(softwareVersion) //
            .certificationCode(certificationCode) //
            .build()
        val saleTerminalData = SaleTerminalData.Builder() //
            .terminalEnvironment(TerminalEnvironment.SemiAttended) //
            .saleCapabilities(
                listOf(
                    SaleCapability.CashierStatus, SaleCapability.CustomerAssistance,
                    SaleCapability.PrinterReceipt
                )
            ) //
            .build()
        return LoginRequest.Builder() //
            .dateTime(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(Date())) //
            .saleSoftware(saleSoftware) //
            .saleTerminalData(saleTerminalData) //
            .operatorLanguage("en") //
            .build()
    }

    @Throws(ConfigurationException::class)
    private fun buildPaymentRequest(): PaymentRequest {
        val inputAmount =
            BigDecimal(binding.inputItemAmount.text.toString())
        val inputTip = BigDecimal(binding.inputTipAmount.text.toString())
        //        BigDecimal inputTip = new BigDecimal(inputTipAmount.getText().toString() ? ); // TODO: if null
        val productCode = binding.inputProductCode.text.toString()
        val requestedAmount = inputAmount.add(inputTip)
        val saleTransactionID = SaleTransactionID.Builder() //
            .transactionID(
                "transactionID" + SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                    .format(Date())
            ) ////
            .timestamp(Instant.now()).build()
        val saleData = SaleData.Builder() //
            // .operatorID("")//
            .operatorLanguage("en") //
            .saleTransactionID(saleTransactionID) //
            .build()
        val amountsReq = AmountsReq.Builder() //
            .currency("AUD") //
            .requestedAmount(requestedAmount) //
            .tipAmount(inputTip)
            .build()
        val saleItem = SaleItem.Builder() //
            .itemID(0) //
            .productCode(productCode) //
            .unitOfMeasure(UnitOfMeasure.Other) //
            .quantity(BigDecimal(1)) //
            .unitPrice(BigDecimal(100.00)) //
            .itemAmount(inputAmount) //
            .productLabel("Product Label") //
            .build()
        val paymentInstrumentData =
            PaymentInstrumentData.Builder() //
                .paymentInstrumentType(PaymentInstrumentType.Cash) //
                .build()
        val paymentData = PaymentData.Builder() //
            .paymentType(PaymentType.Normal) //
            .paymentInstrumentData(paymentInstrumentData) //
            .build()
        val paymentTransaction = PaymentTransaction.Builder() //
            .amountsReq(amountsReq) //
            .addSaleItem(saleItem) //
            .build()
        return PaymentRequest.Builder() //
            .paymentTransaction(paymentTransaction) //
            .paymentData(paymentData) //
            .saleData(saleData).build()
    }

    @Throws(ConfigurationException::class)
    private fun buildTransactionStatusRequest(serviceID: String?): TransactionStatusRequest {
        val messageReference = MessageReference.Builder() //
            .messageCategory(MessageCategory.Payment) //
            .POIID(poiID) //
            .saleID(saleID) //
            .serviceID(serviceID) //
            .build()
        return TransactionStatusRequest(messageReference)
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

    @Throws(ConfigurationException::class)
    private fun buildRefundRequest(): PaymentRequest {
        val inputAmount =
            BigDecimal(binding.inputItemAmount.text.toString())
        val productCode = binding.inputProductCode.text.toString()
        val saleTransactionID = SaleTransactionID.Builder() //
            .transactionID(
                "transactionID" + SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                    .format(Date())
            ) ////
            .timestamp(Instant.now()).build()
        val saleData = SaleData.Builder() //
            .operatorLanguage("en") //
            .saleTransactionID(saleTransactionID) //
            .build()
        val amountsReq = AmountsReq.Builder() //
            .currency("AUD") //
            .requestedAmount(inputAmount) //
            .build()
        val saleItem = SaleItem.Builder() //
            .itemID(0) //
            .productCode(productCode) //
            .unitOfMeasure(UnitOfMeasure.Other) //
            .quantity(BigDecimal(1)) //
            .unitPrice(BigDecimal(100.00)) //
            .itemAmount(inputAmount) //
            .productLabel("Product Label") //
            .build()
        val paymentInstrumentData =
            PaymentInstrumentData.Builder() //
                .paymentInstrumentType(PaymentInstrumentType.Cash) //
                .build()
        val refundData = PaymentData.Builder() //
            .paymentType(PaymentType.Refund) //
            .paymentInstrumentData(paymentInstrumentData) //
            .build()
        val paymentTransaction = PaymentTransaction.Builder() //
            .amountsReq(amountsReq) //
            .addSaleItem(saleItem) //
            .build()
        return PaymentRequest.Builder() //
            .paymentTransaction(paymentTransaction) //
            .paymentData(refundData) //
            .saleData(saleData).build()
    }

    private fun startTransactionUi() {
        runOnUiThread {
            if (currentTransaction == MessageCategory.Payment) {
                binding.btnCancel.setOnClickListener {
                    Logger.logEvent("TestTransactionActivity", "Cancel button clicked")
                    doAbort(
                        currentServiceID,
                        "User Cancelled"
                    )
                }
                binding.btnCancel.visibility = View.VISIBLE
            }
            binding.progressCircle.visibility = View.VISIBLE
            binding.textViewUiHeader.text =
                currentTransaction.toString().uppercase(Locale.getDefault()) + " IN PROGRESS"
            binding.textViewUiDetails.text = "Please wait"
            binding.btnLogin.isEnabled = false
            binding.btnPurchase.isEnabled = false
            binding.btnRefund.isEnabled = false
        }
    }

    private fun endTransactionUi() {
        runOnUiThread {
            binding.progressCircle.visibility = View.INVISIBLE
            binding.btnCancel.visibility = View.INVISIBLE
            binding.textViewUiDetails.text = ""
            binding.textTimer.text = "0"
            binding.btnLogin.isEnabled = true
            binding.btnPurchase.isEnabled = true
            binding.btnRefund.isEnabled = true
        }
    }

    private fun hideCancelBtn(doHide: Boolean) {
        runOnUiThread {
            if (doHide) {
                binding.btnCancel.visibility = View.INVISIBLE
            } else {
                binding.btnCancel.setOnClickListener {
                    Logger.logEvent("TestTransactionActivity", "Cancel button clicked")
                    doAbort(
                        currentServiceID,
                        "User Cancelled"
                    )
                }
                binding.btnCancel.visibility = View.VISIBLE
            }
        }
    }

    private fun hideProgressCircle(doHide: Boolean) {
        runOnUiThread {
            if (doHide) {
                binding.progressCircle.visibility = View.INVISIBLE
            } else {
                binding.progressCircle.visibility = View.VISIBLE
            }
        }
    }

    fun computeSecondsRemaining(start: Long): Long {
        var start = start
        val currentTime = System.currentTimeMillis()
        val sec = (currentTime - start) / 1000
        if (sec == 1L) {
            runOnUiThread { binding.textTimer.text = secondsRemaining--.toString() }
            start = currentTime
        }
        return start
    }

    private fun endLog(ex: Exception) {
        log(ex.message)
        waitingForResponse = false
    }

    private fun endLog(logData: String, stopWaiting: Boolean) {
        runOnUiThread {
            binding.editTextJsonLogs.append(
                """
                        ${sdf.format(Date(System.currentTimeMillis()))}: 
                        $logData
                        
                        
                        """.trimIndent()
            )
        } // 2021.03.24.16.34.26

        if (stopWaiting) {
            waitingForResponse = false
        }
    }

    private fun clearLog() {
        runOnUiThread { binding.editTextJsonLogs.text = "" }
    }

    private fun log(logData: String?) {
        logInfo(TAG, sdf.format(Date(System.currentTimeMillis())) + ": " + logData) // 2021.03.24.16.34.26
        runOnUiThread {
            binding.editTextJsonLogs.append(
                """
    ${sdf.format(Date(System.currentTimeMillis()))}: 
    $logData
    
    
    """.trimIndent()
            )
        }
    }

    fun prettyPrintJson(json: Any?): String {
        val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
        return gson.toJson(json)
    }

    override fun onBackPressed() {
        if (pressedTime + 2000 > System.currentTimeMillis()) {
            super.onBackPressed()
            finish()
        } else {
            Toast.makeText(baseContext, "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
        pressedTime = System.currentTimeMillis()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.back, menu);
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings -> {
                val intent = Intent(this, OldSettingActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.back -> {
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}