package au.com.dmg.fusionadaptor.utils

import android.util.Log
import com.google.gson.GsonBuilder
import java.util.UUID

class DatameshUtils {
    companion object{
        var appName = ""
        var appVersion = ""
        var appIntVersion = 0

        fun generateRandomUUID(): String {
            return UUID.randomUUID().toString()
        }
        fun logInfo(page: String, message: String){
            val tag = "DMG_FUSION"
            val tag2 = "$page - "
            Log.i(tag,"$tag2 $message")
        }

        fun logError(page: String, message: String){
            val tag = "DMG_FUSION"
            val tag2 = "$page - "
            Log.e(tag,"$tag2 $message")
        }

        fun logWarning(page: String, message: String){
            val tag = "DMG_FUSION"
            val tag2 = "$page - "
            Log.w(tag,"$tag2 $message")
        }

        fun prettyPrintJson(json: Any?): String {
            val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
            return gson.toJson(json)
        }
    }
}