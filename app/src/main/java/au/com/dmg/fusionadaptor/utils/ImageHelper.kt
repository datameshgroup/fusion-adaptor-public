package au.com.dmg.fusionadaptor.utils
import android.os.Build
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

class ImageHelper {

    fun getImageBase64(imagePath: String): String? {
        try {
            // Use the class loader to load the resource
            val inputStream: InputStream? = this::class.java.classLoader?.getResourceAsStream(imagePath)

            if (inputStream != null) {
                // Get the Bitmap from the InputStream
                val byteArrayOutputStream = ByteArrayOutputStream()
                inputStream.use { it.copyTo(byteArrayOutputStream) }
                val byteArray = byteArrayOutputStream.toByteArray()

                // Encode the byte array to Base64
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    java.util.Base64.getEncoder().encodeToString(byteArray)
                } else {
                    TODO("VERSION.SDK_INT < O")
                }
            }

        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }
}
