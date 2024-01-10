package au.com.dmg.fusionadaptor.utils

import au.com.dmg.fusion.SaleToPOI
import au.com.dmg.fusion.data.ErrorCondition
import au.com.dmg.fusion.data.MessageCategory
import au.com.dmg.fusion.data.MessageType

class FusionMessageResponse {
    @JvmField
    var isSuccessful: Boolean? = null
    var messageType: MessageType? = null

    @JvmField
    var messageCategory: MessageCategory? = null

    @JvmField
    var saleToPOI: SaleToPOI? = null

    @JvmField
    var displayMessage: String? = null

    @JvmField
    var errorCondition: ErrorCondition? = null

    @JvmField
    var additionalResponse: String? = null

    fun setMessage(
        isSuccessful: Boolean,
        messageType: MessageType?,
        messageCategory: MessageCategory?,
        saleToPOI: SaleToPOI?,
        displayMsg: String?
    ) {
        this.isSuccessful = isSuccessful
        this.messageType = messageType
        this.messageCategory = messageCategory
        this.saleToPOI = saleToPOI
        displayMessage = displayMsg
        errorCondition = null
        additionalResponse = null
    }

    fun setMessage(
        messageType: MessageType?,
        messageCategory: MessageCategory?,
        saleToPOI: SaleToPOI?
    ) {
        isSuccessful = true
        this.messageType = messageType
        this.messageCategory = messageCategory
        this.saleToPOI = saleToPOI
        displayMessage = ""
        errorCondition = null
        additionalResponse = null
    }

    fun setMessage(isSuccessful: Boolean, displayMsg: String?) {
        this.isSuccessful = isSuccessful
        messageType = null
        messageCategory = null
        saleToPOI = null
        displayMessage = displayMsg
        errorCondition = null
        additionalResponse = null
    }

    fun setMessage(
        isSuccessful: Boolean,
        messageType: MessageType?,
        messageCategory: MessageCategory?,
        saleToPOI: SaleToPOI?,
        displayMsg: String?,
        err: ErrorCondition?,
        additionalResponse: String?
    ) {
        this.isSuccessful = isSuccessful
        this.messageType = messageType
        this.messageCategory = messageCategory
        this.saleToPOI = saleToPOI
        this.displayMessage = displayMsg
        errorCondition = err
        this.additionalResponse = additionalResponse
    }
    fun setMessage(
        isSuccessful: Boolean,
        displayMessage: String?,
        err: ErrorCondition?,
        additionalResponse: String?
    ) {
        this.isSuccessful = isSuccessful
        this.displayMessage = displayMessage
        errorCondition = err
        this.additionalResponse = additionalResponse
    }
}
