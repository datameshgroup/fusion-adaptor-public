package au.com.dmg.fusionadaptor.model

import au.com.dmg.fusion.data.MessageCategory
import au.com.dmg.fusion.data.PaymentType
import au.com.dmg.fusion.request.paymentrequest.SaleItem
import java.io.Serializable
import java.math.BigDecimal

class RequestData : Serializable
{
    var serviceID: String = ""
    var messageCategory: MessageCategory? = null
    var paymentType: PaymentType?  = null
    var requestedAmount: BigDecimal? = null
    var tipAmount: BigDecimal? = null
    var cashBackAmount: BigDecimal? = null
    var productCode: String? = null
    var saleItem: List<SaleItem>? = null
    var operatorID: String? = null

    //errorHandling
    var additionalResponse: String? = null

    //HIO-specific
    var taxAmount: BigDecimal? = null
    var documentData: String? = null
    var saleTransactionID: String? = null
}