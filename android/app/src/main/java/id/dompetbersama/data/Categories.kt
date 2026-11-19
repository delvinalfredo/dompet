package id.dompetbersama.data

object Categories {
    val expense = listOf(
        "Makan", "Transport", "Belanja", "Tagihan", "Pulsa & internet",
        "Kesehatan", "Hiburan", "Transfer keluar", "Lainnya",
    )
    val income = listOf("Gaji", "Transfer masuk", "Bonus", "Penjualan", "Refund", "Lainnya")

    fun forType(type: TxType): List<String> = if (type == TxType.INCOME) income else expense

    // First matching rule wins, so specific merchants come before broad ones ("gofood" before "gojek").
    private val expenseRules = listOf(
        "Makan" to listOf("gofood", "grabfood", "shopeefood", "resto", "makan", "kopi", "coffee"),
        "Transport" to listOf("goride", "gocar", "grabbike", "grabcar", "grab", "gojek", "maxim", "pertamina", "bensin", "parkir", "krl", "mrt"),
        "Tagihan" to listOf("pln", "listrik", "pdam", "bpjs", "indihome", "tagihan"),
        "Pulsa & internet" to listOf("pulsa", "paket data", "kuota", "telkomsel", "indosat"),
        "Belanja" to listOf("shopee", "tokopedia", "lazada", "blibli", "indomaret", "alfamart", "qris"),
        "Transfer keluar" to listOf("transfer ke", "kirim uang", "transferred", "sent rp"),
    )
    private val incomeRules = listOf(
        "Gaji" to listOf("gaji", "payroll", "salary"),
        "Refund" to listOf("refund", "pengembalian dana"),
        "Bonus" to listOf("cashback", "bonus"),
    )

    /** [lowerText] must already be lowercase. */
    fun guess(lowerText: String, type: TxType): String {
        val rules = if (type == TxType.INCOME) incomeRules else expenseRules
        val hit = rules.firstOrNull { (_, words) -> words.any { lowerText.contains(it) } }?.first
        return hit ?: if (type == TxType.INCOME) "Transfer masuk" else "Lainnya"
    }
}
