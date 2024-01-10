package au.com.dmg.fusionadaptor

import au.com.dmg.fusion.MessageHeader
import au.com.dmg.fusion.data.ErrorCondition
import au.com.dmg.fusion.data.MessageCategory
import au.com.dmg.fusion.data.MessageClass
import au.com.dmg.fusion.data.MessageType
import au.com.dmg.fusion.request.SaleToPOIRequest
import au.com.dmg.fusion.request.paymentrequest.POIData
import au.com.dmg.fusion.request.paymentrequest.POITransactionID
import au.com.dmg.fusion.request.paymentrequest.SaleTransactionID
import au.com.dmg.fusion.response.Response
import au.com.dmg.fusion.response.ResponseResult
import au.com.dmg.fusion.response.SaleToPOIResponse
import au.com.dmg.fusion.response.paymentresponse.PaymentResponse
import au.com.dmg.fusion.response.paymentresponse.PaymentResponseSaleData
import java.time.Instant

class ResponseBuilder {
    companion object{
        fun generateFailedTransactionResponse(errorCondition: ErrorCondition,
                                              additionalResponse: String, request: SaleToPOIRequest): SaleToPOIResponse {
            val txTime = Instant.ofEpochMilli(System.currentTimeMillis())
            val paymentResponse = PaymentResponse.Builder()
                        .response(
                            Response.Builder()
                                .errorCondition(errorCondition)
                                .result(ResponseResult.Failure)
                                .additionalResponse(additionalResponse)
                                .build()
                        )
                        .saleData(
                            PaymentResponseSaleData(
                                SaleTransactionID.Builder()
                                    .transactionID(
                                        request.paymentRequest?.saleData?.saleTransactionID?.transactionID ?: ""
                                    )
                                    .timestamp(
                                        request.paymentRequest?.saleData?.saleTransactionID?.timestamp?: txTime
                                    )
                                    .build()
                            )
                        )
                        .POIData(
                        POIData.Builder()
                            .POITransactionID(
                                POITransactionID(request.paymentRequest?.saleData?.saleTransactionID?.transactionID ?: "",
                                                txTime)
                            )
                            .POIReconciliationID("")
                            .build()
                    ).build()
            val messageHeader =   MessageHeader.Builder()
                .messageClass(MessageClass.Service)
                .messageCategory(MessageCategory.Payment)
                .messageType(MessageType.Response)
                .serviceID(request.messageHeader.serviceID)
                .saleID(request.messageHeader.saleID)
                .POIID(request.messageHeader.poiID)
                .build()

            return SaleToPOIResponse.Builder()
                .messageHeader(messageHeader)
                .response(paymentResponse)
                .build()
        }
    }
}