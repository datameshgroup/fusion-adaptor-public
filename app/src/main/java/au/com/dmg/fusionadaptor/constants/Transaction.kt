package au.com.dmg.fusionadaptor.constants

class Transaction {
    companion object {
        val SALE = "SALE"
        val NEGATIVE_SALE = "NEGATIVE_SALE"
        val REFUND = "REFUND"
        val ADJUST_TIPS = "ADJUST_TIPS"
        val VOID_TRANSACTION = "VOID_TRANSACTION"
        val QUERY_TRANSACTION = "QUERY_TRANSACTION"
        val BATCH_CLOSE = "BATCH_CLOSE"
    }
}