package au.com.dmg.fusionadaptor

import au.com.dmg.fusion.MessageHeader
import au.com.dmg.fusion.data.MessageCategory
import au.com.dmg.fusion.data.MessageClass
import au.com.dmg.fusion.data.MessageType
import au.com.dmg.fusion.data.PaymentType
import au.com.dmg.fusion.data.SaleCapability
import au.com.dmg.fusion.data.TerminalEnvironment
import au.com.dmg.fusion.request.Request
import au.com.dmg.fusion.request.SaleTerminalData
import au.com.dmg.fusion.request.SaleToPOIRequest
import au.com.dmg.fusion.request.loginrequest.LoginRequest
import au.com.dmg.fusion.request.loginrequest.SaleSoftware
import au.com.dmg.fusion.request.paymentrequest.AmountsReq
import au.com.dmg.fusion.request.paymentrequest.PaymentData
import au.com.dmg.fusion.request.paymentrequest.PaymentRequest
import au.com.dmg.fusion.request.paymentrequest.PaymentTransaction
import au.com.dmg.fusion.request.paymentrequest.SaleData
import au.com.dmg.fusion.request.paymentrequest.SaleTransactionID
import au.com.dmg.fusion.request.transactionstatusrequest.MessageReference
import au.com.dmg.fusion.request.transactionstatusrequest.TransactionStatusRequest
import au.com.dmg.fusion.securitytrailer.SecurityTrailer
import au.com.dmg.fusion.util.SecurityTrailerUtil.generateSecurityTrailer
import au.com.dmg.fusionadaptor.datastore.Configuration
import au.com.dmg.fusionadaptor.model.RequestData
import au.com.dmg.fusionadaptor.utils.DatameshUtils
import au.com.dmg.fusionadaptor.utils.Logger
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.UUID


class RequestBuilder {
    companion object {
        private lateinit var config: Configuration.ConfigurationData
        private var isDev: Boolean = true
        private var onTerminal: Boolean = true
        fun init(configuration: Configuration.ConfigurationData, isDev: Boolean, onTerminal: Boolean) {
            this.config = configuration
            this.isDev = isDev
            this.onTerminal = onTerminal
        }

        private fun checkInitialized() {
            if (!::config.isInitialized) {
                var exception = IllegalStateException("RequestBuilder has not been initialized. Call init(configuration: Configuration.ConfigurationData, isDev: Boolean) before using other methods.")
                Logger.logException("ResponseBuilder", "checkInitialized", exception)
                throw exception
            }
        }
        fun buildRefundRequest(
            requestData: RequestData): SaleToPOIRequest {
            checkInitialized()
            return SaleToPOIRequest.Builder()
                .messageHeader(
                    MessageHeader.Builder()
                        .messageClass(MessageClass.Service)
                        .messageCategory(MessageCategory.Payment)
                        .messageType(MessageType.Request)
                        .serviceID(requestData.serviceID)
                        .saleID(this.config.saleId)
                        .build()
                )
                .request(
                    PaymentRequest.Builder()
                        .saleData(
                            SaleData.Builder()
                                .operatorLanguage("en")
                                .apply {
                                    if (requestData.operatorID != null) {
                                        operatorID(requestData.operatorID)
                                    }
                                }
                                .saleTransactionID(
                                    SaleTransactionID.Builder()
                                        .timestamp(Instant.ofEpochMilli(System.currentTimeMillis()))
                                        .transactionID(requestData.operatorID ?: DatameshUtils.generateRandomUUID()) // Document.Header.SaleId
                                        .build()
                                )
                                .build()
                        )
                        .paymentTransaction(
                            PaymentTransaction.Builder()
                                .amountsReq(
                                    AmountsReq.Builder()
                                        .currency("AUD")
                                        .requestedAmount(requestData.requestedAmount) //Total of all sale items
                                        .tipAmount(requestData.tipAmount)
                                        .cashBackAmount(requestData.cashBackAmount?: BigDecimal(0))
                                        .build()
                                )
                                .apply {
                                    if (requestData.saleItem != null) {
                                        addSaleItems(requestData.saleItem)
                                    }
                                }
                                .build()
                        )
                        .paymentData(
                            PaymentData.Builder()
                                .paymentType(PaymentType.Refund)
                                .build()
                        )
                        .build()
                )
                .build()
        }

        fun buildPaymentRequest(
        requestData: RequestData): SaleToPOIRequest {
            checkInitialized()

            return SaleToPOIRequest.Builder()
                .messageHeader(
                    MessageHeader.Builder()
                        .messageClass(MessageClass.Service)
                        .messageCategory(MessageCategory.Payment)
                        .messageType(MessageType.Request)
                        .serviceID(requestData.serviceID)
                        .saleID(this.config.saleId)
                        .build()
                )
                .request(
                    PaymentRequest.Builder()
                        .saleData(
                            SaleData.Builder()
                                .operatorLanguage("en")
                                .apply {
                                    if (requestData.operatorID != null) {
                                        operatorID(requestData.operatorID)
                                    }
                                }
                                .saleTransactionID(
                                    SaleTransactionID.Builder()
                                        .timestamp(Instant.ofEpochMilli(System.currentTimeMillis()))
                                        .transactionID(requestData.operatorID ?: DatameshUtils.generateRandomUUID()) // Document.Header.SaleId
                                        .build()
                                )
                                .build()
                        )
                        .paymentTransaction(
                            PaymentTransaction.Builder()
                                .amountsReq(
                                    AmountsReq.Builder()
                                        .currency("AUD")
                                        .requestedAmount(requestData.requestedAmount) //Total of all sale items
                                        .tipAmount(requestData.tipAmount)
                                        .cashBackAmount(requestData.cashBackAmount?: BigDecimal(0))
                                        .build()
                                )
                                .apply {
                                    if (requestData.saleItem != null) {
                                        addSaleItems(requestData.saleItem)
                                    }
                                }
                                .build()
                        )
                        .paymentData(
                            PaymentData.Builder()
                                .paymentType(PaymentType.Normal)
                                .build()
                        )
                        .build()
                )
                .build()
        }

        fun buildLoginRequest(serviceID: String): SaleToPOIRequest{
            checkInitialized()
            val saleSoftware = SaleSoftware.Builder()
                .providerIdentification(this.config.providerIdentification)
                .applicationName(this.config.applicationName)
                .softwareVersion(this.config.softwareVersion)
                .certificationCode(this.config.certificationCode)
                .build()

            val saleTerminalData = SaleTerminalData.Builder()
                .terminalEnvironment(TerminalEnvironment.Attended)
                .saleCapabilities(
                    listOf(
                        SaleCapability.CashierStatus, SaleCapability.CustomerAssistance,
                        SaleCapability.PrinterReceipt
                    )
                )
                .build()

            val loginRequest = LoginRequest.Builder()
                .dateTime(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(Date()))
                .operatorLanguage("en")
                .saleTerminalData(saleTerminalData)
                .saleSoftware(saleSoftware)
                .build()

            return SaleToPOIRequest.Builder()
                .messageHeader(
                    MessageHeader.Builder()
                        .messageClass(MessageClass.Service)
                        .messageCategory(MessageCategory.Login)
                        .messageType(MessageType.Request)
                        .serviceID(serviceID)
                        .saleID(this.config.saleId)
                        .POIID(this.config.poiId)
                        .build()
                )
                .request(loginRequest)
                .build()
        }

        fun buildTransactionStatusRequest(serviceID: String): SaleToPOIRequest {
            checkInitialized()
            // TODO, this only accommodates Payment for now
            val messageHeader = MessageHeader.Builder()
                .messageClass(MessageClass.Service)
                .messageCategory(MessageCategory.TransactionStatus)
                .messageType(MessageType.Request)
                .saleID(this.config.saleId)//
                .POIID(this.config.poiId)//
                .serviceID(UUID.randomUUID().toString())
                .build()

            val messageReference = MessageReference.Builder()
                .messageCategory(MessageCategory.Payment)
                .serviceID(serviceID)
                .POIID(this.config.poiId)
                .saleID(this.config.saleId)
                .build()

            val transactionStatusRequest = TransactionStatusRequest(messageReference)

            if(onTerminal){
                return SaleToPOIRequest.Builder()
                    .messageHeader(messageHeader)
                    .request(transactionStatusRequest)
                    .build()
            } else{
                val securityTrailer = generateSecurityTrailer(messageHeader, transactionStatusRequest)

                return SaleToPOIRequest.Builder() //
                    .messageHeader(messageHeader) //
                    .request(transactionStatusRequest) //
                    .securityTrailer(securityTrailer) //
                    .build()
            }
        }

        //TODO below should not be needed
        private fun generateSecurityTrailer(
            messageHeader: MessageHeader,
            request: Request
        ): SecurityTrailer? {
            checkInitialized()
            return generateSecurityTrailer(messageHeader, request, this.isDev)
        }
    }

}