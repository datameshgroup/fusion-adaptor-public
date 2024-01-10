package au.com.dmg.fusionadaptor.utils

import android.content.Context
import android.util.Base64OutputStream
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.StringWriter
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.Properties
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult


internal object APIUtils {
    /**
     * Process int value that uses Payment Gateway
     *
     * @param value
     * @return
     */
    val TAG = "APIUtils"
    fun parseAPIAmount(value: String): BigDecimal? {
        return try {
            val integerPart = value.substring(0, value.length - 2)
            val decimalPart = value.substring(value.length - 2, value.length)
//            BigDecimal("$integerPart.$decimalPart".toDouble())
            BigDecimal("$integerPart.$decimalPart")
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    /**
     * Formats decimal number to communication specified API format.
     *
     * @param bigDecimal
     * @return
     */
    fun serializeAPIAmount(bigDecimal: BigDecimal): String? {
        val df = DecimalFormat("0.00", DecimalFormatSymbols(Locale.US))
        val result = df.format(bigDecimal.toDouble())
        return try {
            result.replace("\\.".toRegex(), "")
        } catch (nfe: NumberFormatException) {
            "000"
        }
    }

    /**
     * Parse initialize action input params to key - value structure.
     *
     * @param serializedPropeties
     * @return
     */
    fun parseInitializeParameters(serializedPropeties: String?): Properties? {
        val properties = Properties()
        try {
            if (!serializedPropeties.isNullOrEmpty()) {
                val dbFactory = DocumentBuilderFactory.newInstance()
                val dBuilder = dbFactory.newDocumentBuilder()
                val document =
                    dBuilder.parse(ByteArrayInputStream(serializedPropeties.toByteArray()))
                document.normalize()

                // Obtaining all params
                val params = document.getElementsByTagName("Param")
                for (i in 0 until params.length) {
                    val param = params.item(i) as Element
                    val key = param.getAttribute("Key")
                    val value = param.textContent
                    properties[key] = value
                }
            }
        } catch (e: Exception) {
        }
        return properties
    }

    fun formatReceipt(context: Context, content: String, printerColumns: String, onTerminal: Boolean): String? {
        var receipt = ""
        try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val document = builder.newDocument()
            val receiptNode = document.createElement("Receipt")
            receiptNode.setAttribute("numCols", printerColumns)
            document.appendChild(receiptNode)

            // DMG LOGO
            val pngInputStream = context.assets.open("dmg_receipt_logo.png")
            val dmgLogoBase64 = pngInputStream?.let { encodePngToBase64(it) }
            if (dmgLogoBase64 != null) {
                addReceiptLineToXMLDocument(
                    document, printerColumns, receiptNode,
                    "IMAGE", "NORMAL",
                    dmgLogoBase64
                )
            }
            // Go through the contents
            if (content != null) {
                val rawLines = content.replace(Regex("[\n\t]"), "").split(Regex("<br/>|<p/>|</p>"))
                for (line in rawLines) {
                    val printLine = line.replace(Regex("<[^>]*>"), "")
                    //Add space for paragraph starters
                    if (line.lowercase(Locale.ROOT).startsWith("<p") && !onTerminal) {
                        addReceiptLineToXMLDocument(
                            document, printerColumns, receiptNode,
                            "TEXT", "NORMAL",
                            ""
                        )
                    }
                    //NORMAL / BOLD / DOUBLE_HEIGHT / DOUBLE_WIDTH/ UNDERLINE
                    if (line.lowercase(Locale.ROOT).endsWith("</b>")) {
                        addReceiptLineToXMLDocument(
                            document, printerColumns, receiptNode,
                            "TEXT", "BOLD",
                            printLine
                        )
                    }else{
                        addReceiptLineToXMLDocument(
                            document, printerColumns, receiptNode,
                            "TEXT", "NORMAL",
                            printLine
                        )
                    }

                    val transformerFactory = TransformerFactory.newInstance()
                    val transformer = transformerFactory.newTransformer()
                    val source = DOMSource(document)
                    val stringWriter = StringWriter()
                    val streamResult = StreamResult(stringWriter)

                    transformer.transform(source, streamResult)
                    receipt = stringWriter.toString()
                }
                addReceiptLineToXMLDocument(
                    document, printerColumns, receiptNode,
                    "CUT_PAPER", "NORMAL",
                    ""
                )
            }
        } catch (te: TransformerException) {
            Logger.logException("APIUtils", "formatReceipt", te)
            te.printStackTrace()
        } catch (pce: ParserConfigurationException) {
            Logger.logException("APIUtils", "formatReceipt", pce)
            pce.printStackTrace()
        } catch (e: Exception) {
            Logger.logException("APIUtils", "formatReceipt", e)
            e.printStackTrace()
        }
        return receipt
    }

    private fun addReceiptLineToXMLDocument(
        document: Document, printerColumns: String, rootElement: Element,
        type: String, format: String, value: String
    ) {
        // ReceiptLine
        val receiptLine = document.createElement("ReceiptLine")
        receiptLine.setAttribute("type", type)
        rootElement.appendChild(receiptLine)

        // Formats
        val formatsNode = document.createElement("Formats")
        receiptLine.appendChild(formatsNode)
        val formatNode = document.createElement("Format")
        formatNode.setAttribute("from", "0")
        formatNode.setAttribute("to", printerColumns)
        formatNode.textContent = format
        formatsNode.appendChild(formatNode)

        // Text
        val textNode = document.createElement("Text")
        textNode.textContent = value
        receiptLine.appendChild(textNode)
    }

    private fun encodePngToBase64(svgInputStream: InputStream): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val base64OutputStream = Base64OutputStream(byteArrayOutputStream, 2) // Use 2 for NO_WRAP
        val buffer = ByteArray(1024)
        var bytesRead: Int

        try {
            bytesRead = svgInputStream.read(buffer)
            while (bytesRead != -1) {
                base64OutputStream.write(buffer, 0, bytesRead)
                bytesRead = svgInputStream.read(buffer)
            }
            svgInputStream.close()
            base64OutputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return byteArrayOutputStream.toString()
    }

}