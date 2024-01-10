package au.com.dmg.fusionadaptor.utils

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import au.com.dmg.fusionadaptor.BuildConfig
import au.com.dmg.fusionadaptor.utils.DatameshUtils.Companion.logInfo
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.crashlytics.ktx.setCustomKeys
import com.google.firebase.ktx.Firebase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


class Logger {

    companion object {
        private var isCrashlyticsInitialized = false
        private var isAnalyticsInitialized = false
        private var userID = null

        private fun initializeCrashlytics() {
            if(!isCrashlyticsInitialized) {
                val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
                val current = LocalDateTime.now().format(formatter)
                initiateNewSession(current) //To do: to be updated
            }
        }

        fun initializeAnalytics() {
            if (!isAnalyticsInitialized) {
                Firebase.analytics.setAnalyticsCollectionEnabled(BuildConfig.DEBUG)

                isAnalyticsInitialized = true
            }
        }

        @SuppressLint("MissingPermission")
        private fun getSerial(): String {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //Build.getSerial()
                "123456" //To do: To be updated
            } else {
                Build.SERIAL
            }
        }

        private fun initaliaseKeys(serialNumber: String){
            val crashlytics = Firebase.crashlytics
            crashlytics.setCustomKeys {
                key("Device", Build.DEVICE)
                key("Model", Build.MODEL)
                key("Product", Build.PRODUCT)
                key("Display", Build.DISPLAY)
                key("Hardware",Build.HARDWARE)
                key("Manufacturer", Build.HARDWARE)
                key("User", Build.USER)
                key("Host", Build.HOST)
                key("Version", Build.VERSION.SDK_INT)
                key("VersionRelease", Build.VERSION.RELEASE)
                key("SerialNumber", serialNumber)
            }
        }

        private fun logNonFatal(message: String){
            if(!isCrashlyticsInitialized){
                initializeCrashlytics()
            }
            Firebase.crashlytics.log(message)
            Firebase.crashlytics.recordException(Exception("NonFatal | $message"))
            logInfo("Logger", "logNonFatal | $message")
        }

        fun logAnalyticsEvent(eventName: String, params: Bundle? = null) {
            if (!isAnalyticsInitialized) {
                initializeAnalytics()
            }

            // Limit the length of the parameter value
            params?.let {
                for (key in it.keySet()) {
                    val value = it.get(key)
                    if (value is String && value.length > 100) {
                        it.putString(key, value.substring(0, 100))
                    }
                }
            }

            Firebase.analytics.logEvent(eventName, params)
            logInfo("Logger", "logCustomEvent | $eventName")
        }

        private fun logIntents(prefixString: String, message: String){
            val maxMessageSize = 10000
            logInfo("Logger", "logIntents | message = $message, messageLength = ${message.length}" )
            if(message.length > maxMessageSize){
                val messageList: List<String> = message.chunked(maxMessageSize)
                logInfo("Logger", "logIntents | messageList.size = ${messageList.size}" )
                for (i in messageList.indices){
                    logNonFatal("$prefixString | $i | " + messageList[i])
                }
            }
            else {
                logNonFatal("$prefixString | $message")
            }

//
//            // Log a custom event with parameters
//            val eventParams = Bundle().apply {
//                putString("Logger", message)
//            }
//            logAnalyticsEvent("event_intent_response", eventParams)
        }

        fun initiateNewSession(userID: String){
            Firebase.crashlytics.setUserId(userID)
            initaliaseKeys(getSerial())
            isCrashlyticsInitialized = true
        }

        fun log(sourceFile: String, methodName: String, message: String){
            logNonFatal("$sourceFile | $methodName() | $message")
        }

        fun logEvent(sourceFile: String, eventDescription: String){
            logNonFatal("$sourceFile | Event | $eventDescription")
        }

        fun logFusionRequest(sourceFile: String, methodName: String, fusionMethod: String, request: String?, serviceID: String?){
            logNonFatal("$sourceFile | $methodName() | Fusion Request | $fusionMethod($request,$serviceID)")
        }

        fun logFusionResponse(sourceFile: String, methodName: String, response: String?){
            logNonFatal("$sourceFile | $methodName() | Fusion Response | $response")
        }

        fun logIntentsReceived(sourceFile: String, methodName: String, intent: Intent){
            val stringBuilder = StringBuilder()
            if(intent.extras != null) {
                val bundle: Bundle = intent.extras!!
                for (key in bundle.keySet()) {
                    stringBuilder.append("[" + key + "=" + bundle.get(key) + "],")
                }
            }
            var prefixString = "$sourceFile | $methodName() | Intents |  Received"
            logIntents(prefixString, stringBuilder.toString())
        }

        fun logIntentsResultReceived(sourceFile: String, resultCode: Int?, intent: Intent?){
            val stringBuilder = StringBuilder()
            if(intent?.extras != null) {
                val bundle: Bundle = intent.extras!!
                for (key in bundle.keySet()) {
                    stringBuilder.append("[" + key + "=" + bundle.get(key) + "],")
                }
            }
            var prefixString = "$sourceFile | Intents |  Received"
            if(resultCode != null){
                prefixString +=  " | Result=$resultCode"
            }
            logIntents(prefixString, stringBuilder.toString())
        }

        fun logIntentsSet(sourceFile: String, methodName: String, resultCode: Int?, intent: Intent){
            val stringBuilder = StringBuilder()
            if(intent.extras != null) {
                val bundle: Bundle = intent.extras!!
                for (key in bundle.keySet()) {
                    stringBuilder.append("[" + key + "=" + bundle.get(key) + "],")
                }
            }
            var prefixString : String = "$sourceFile | Intents | Set"
            if(resultCode != null){
                prefixString +=  " | Result=$resultCode"
            }
            logIntents(prefixString, stringBuilder.toString())
        }

        fun logException(sourceFile: String, methodName: String, exception: Exception){
            if(!isCrashlyticsInitialized){
                initializeCrashlytics()
            }
            val message = "NonFatal Exception | $sourceFile | $methodName() | " + exception.message
            Firebase.crashlytics.log(message)
            Firebase.crashlytics.recordException(Exception(message))
            logInfo("Logger", message)
        }
    }
}