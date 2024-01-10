package au.com.dmg.fusionadaptor.constants

class Timeout {
    companion object {
        //Timeouts associated to Pairing
        const val PairingConnection = 30000L
        const val PairingLogin = 20000L
        const val PairingRetryDelay = 10000L
        const val PairingNextBtnDisplay = 10000L

        //Timeouts associated to Transaction processing
        const val TransactionConnection = 30000L
        const val TransactionLogin = 10000L
        const val TransactionPayment = 60000L
        const val TransactionErrorHandling = 90000L
        const val TransactionRetryDelay = 10000L
    }
}