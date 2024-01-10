package au.com.dmg.fusionadaptor.screens

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import au.com.dmg.fusion.Message
import au.com.dmg.fusion.data.MessageCategory
import au.com.dmg.fusion.data.PaymentType
import au.com.dmg.fusion.data.UnitOfMeasure
import au.com.dmg.fusion.request.SaleToPOIRequest
import au.com.dmg.fusion.request.paymentrequest.SaleItem
import au.com.dmg.fusionadaptor.RequestBuilder
import au.com.dmg.fusionadaptor.databinding.RequestActivityBinding
import au.com.dmg.fusionadaptor.datastore.Configuration
import au.com.dmg.fusionadaptor.hio.DatameshMapping
import au.com.dmg.fusionadaptor.model.RequestData
import au.com.dmg.fusionadaptor.utils.DatameshUtils
import au.com.dmg.fusionadaptor.utils.DatameshUtils.Companion.logError
import au.com.dmg.fusionadaptor.utils.DatameshUtils.Companion.logInfo
import au.com.dmg.fusionadaptor.utils.Logger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID

class RequestActivity: AppCompatActivity()  {
    private lateinit var binding: RequestActivityBinding
    private val TAG = "RequestActivity"
    var currentServiceID = ""
    var productCode =""
    private var tipAmount:BigDecimal?=null
    @JvmField
    var requestData = RequestData()
    var saleToPOIRequest: SaleToPOIRequest? = null
    private var isPosOnTerminal = false

    var reqIntent: Intent? = null
    private val requestBuilder = RequestBuilder


    //TODO Add close button
    private fun buildPaymentRequest(requestData: RequestData): SaleToPOIRequest {
        return requestBuilder.buildPaymentRequest(requestData)
    }
    private fun buildRefundRequest(requestData: RequestData): SaleToPOIRequest {
        return requestBuilder.buildRefundRequest(requestData)
    }

    //TODO add internet connection status enable/disable buttons?
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RequestActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        lifecycleScope.launch {
            isPosOnTerminal = Configuration.getOnTerminal(applicationContext).first()
            val config = Configuration.getConfiguration(applicationContext).first()
            val isDev = Configuration.getUseTestEnvironment(applicationContext).first()
            val enableTipping = Configuration.getEnableTip(applicationContext).first()
            DatameshMapping.initRequestBuilder(config, isDev, isPosOnTerminal, enableTipping)
        }

        currentServiceID = UUID.randomUUID().toString()

        // CHECKING IF ON-DEVICE OR OFF-DEVICE
        if(isPosOnTerminal){
            reqIntent = Intent(Message.INTENT_ACTION_SALETOPOI_REQUEST)
            // name of this app, that gets treated as the POS label by the terminal.
            reqIntent!!.putExtra(Message.INTENT_EXTRA_APPLICATION_NAME, DatameshUtils.appName)
            // version of of this POS app.
            reqIntent!!.putExtra(Message.INTENT_EXTRA_APPLICATION_VERSION, DatameshUtils.appVersion)
            reqIntent!!.flags = 0
        }else{
            reqIntent = Intent(this, TransactionProgressActivity::class.java)
        }

        binding.btnLogin.setOnClickListener {
            Logger.logEvent("RequestActivity", "Login button clicked")
            saleToPOIRequest = RequestBuilder.buildLoginRequest(currentServiceID)
            startTransactionProgressActivity(saleToPOIRequest!!)
        }

        binding.btnPurchase.setOnClickListener {
            Logger.logEvent("RequestActivity", "Purchase button clicked")
            doTransaction(PaymentType.Normal)
        }

        binding.btnRefund.setOnClickListener {
            Logger.logEvent("RequestActivity", "Refund button clicked")
            doTransaction(PaymentType.Refund)
        }
        binding.btnCancel.setOnClickListener {
            Logger.logEvent("RequestActivity", "Cancel button clicked")
            finish()
        }
    }

    private fun doTransaction(pt: PaymentType) {
        productCode = binding.inputProductCode.text.toString()
        tipAmount.let{
            if(binding.inputTipAmount.text.toString().isEmpty()){
                BigDecimal(0)
            }else{
                BigDecimal(binding.inputTipAmount.text.toString())
            }
        }
        val newTipAmount =
            if(isPosOnTerminal){
                if (tipAmount?.compareTo(BigDecimal.ZERO)!! > 0) tipAmount else null
            }else{
                tipAmount
            }


        val productCode = binding.inputProductCode.text.toString()

        requestData.apply {
            serviceID = currentServiceID
            messageCategory = MessageCategory.Payment
            paymentType = pt
            requestedAmount =
                if(binding.inputItemAmount.text.toString().isEmpty()){
                    BigDecimal(0)
                }else{
                    BigDecimal(binding.inputItemAmount.text.toString())
                }

            this.tipAmount = newTipAmount
            saleItem = listOf(
                SaleItem.Builder()
                    .itemID(1)
                    .productCode(productCode)
                    .productLabel(productCode)
                    .quantity(BigDecimal(2))
                    .unitPrice(BigDecimal(5))
                    .itemAmount(BigDecimal(10.0))
                    .unitOfMeasure(UnitOfMeasure.Other)
                    .build()
            )
        }


        when(pt){
            PaymentType.Normal -> saleToPOIRequest = buildPaymentRequest(requestData)
            PaymentType.Refund -> saleToPOIRequest = buildRefundRequest(requestData)
            else -> {
                //TODO
                saleToPOIRequest = null
                logInfo(TAG, "other payment PaymentType not supported yet")
            }
        }

        startTransactionProgressActivity(saleToPOIRequest)
    }

    private fun startTransactionProgressActivity(saleToPOIRequest: SaleToPOIRequest?) {
        val message = Message(saleToPOIRequest)
        logInfo(TAG, "SaleToPOIRequest : ${message.toJson()} ")

        reqIntent!!.putExtra(Message.INTENT_EXTRA_MESSAGE, message.toJson())
        startForResult.launch(reqIntent)
    }

    private val startForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        logInfo(TAG, "Received Result")
        if (result.resultCode == 1) {
            logInfo(TAG, "JSON:  ${result.data.toString()}")
        }
        logInfo(TAG, "Returned to RequestActivity")

        if (result.data != null && result.data!!.hasExtra(Message.INTENT_EXTRA_MESSAGE)) {
            val message = try {
                Message.fromJson(result.data!!.getStringExtra(Message.INTENT_EXTRA_MESSAGE))
            } catch (e: Exception) {
                logError(TAG, "Error reading Terminal intent response.")
                return@registerForActivityResult
            }
            logInfo(TAG, "SaleToPOIResponse: $message ")


        }else{
            logError(TAG, "Error reading response from Satellite.")
        }
    }
}