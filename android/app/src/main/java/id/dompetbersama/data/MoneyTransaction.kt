package id.dompetbersama.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TxType { INCOME, EXPENSE }

/**
 * PENDING = read from a notification, waiting for the user to confirm it.
 * DISCARDED = the user threw it away; the row stays so the same notification is not added again.
 */
enum class TxStatus { PENDING, CONFIRMED, DISCARDED }

@Entity(
    tableName = "transactions",
    indices = [
        // Nullable: manual entries have no key; SQLite allows many NULLs in a unique index.
        Index(value = ["dedupKey"], unique = true),
        Index(value = ["status", "occurredAt"]),
    ],
)
data class MoneyTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TxType,
    /** Whole rupiah. Never a float. */
    val amount: Long,
    val category: String,
    val note: String = "",
    val source: String = SOURCE_MANUAL,
    val occurredAt: Long,
    val status: TxStatus = TxStatus.CONFIRMED,
    val dedupKey: String? = null,
    /**
     * Set when the money only moved between the user's own wallets (Jago → GoPay). Rows sharing a group are
     * the two sides of one move. Kept out of income and expense totals; [type] still says which side this is.
     */
    val transferGroup: String? = null,
) {
    companion object {
        const val SOURCE_MANUAL = "Manual"
    }
}

// An extension, not a member: Room would otherwise look for an isTransfer column.
val MoneyTransaction.isTransfer: Boolean get() = transferGroup != null
