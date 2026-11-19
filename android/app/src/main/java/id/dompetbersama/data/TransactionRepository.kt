package id.dompetbersama.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/** [inTransaction] runs a block as one database transaction (Room's withTransaction), so a restore is all or nothing. */
class TransactionRepository(
    private val dao: TransactionDao,
    private val inTransaction: suspend (suspend () -> Unit) -> Unit,
) {
    val confirmed = dao.observeConfirmed()
    val pending = dao.observePending()

    // Check-then-insert and pairing must not interleave with each other or with the user's edits.
    private val lock = Mutex()

    /**
     * [transferChanged]: the user flipped the "pindah dana" switch. Otherwise the group stored in the database
     * wins, because a notification may have paired this row after the form was opened.
     */
    suspend fun save(tx: MoneyTransaction, transferChanged: Boolean = false) = lock.withLock {
        if (tx.id == 0L) {
            dao.insert(tx)
            return@withLock
        }
        val stored = dao.byId(tx.id)?.transferGroup
        val group = when {
            !transferChanged -> stored
            tx.transferGroup == null -> {
                // Switched off: the partner becomes ordinary income or expense again instead of silently vanishing.
                stored?.let { dao.clearTransfer(it) }
                null
            }
            else -> stored ?: tx.transferGroup
        }
        dao.update(tx.copy(transferGroup = group))
    }

    /**
     * False when the notification was already recorded: either the exact same notification (dedupKey),
     * or an app re-posting the same text and amount with a fresh timestamp within [DUPLICATE_WINDOW_MS].
     * Trade-off: two real payments with identical text inside that window keep only one; the user adds the other.
     */
    suspend fun addFromNotification(tx: MoneyTransaction): Boolean = lock.withLock {
        val similar = dao.countSimilar(
            source = tx.source,
            type = tx.type.name,
            amount = tx.amount,
            note = tx.note,
            from = tx.occurredAt - DUPLICATE_WINDOW_MS,
            to = tx.occurredAt + DUPLICATE_WINDOW_MS,
        )
        if (similar != 0) return@withLock false
        val id = dao.insert(tx)
        if (id == -1L) return@withLock false
        pairWithCounterpart(tx.copy(id = id))
        true
    }

    /** Top up GoPay from Jago arrives as two notifications; link them so the move is not counted twice. */
    private suspend fun pairWithCounterpart(tx: MoneyTransaction) {
        val other = dao.findTransferCounterpart(
            id = tx.id,
            type = (if (tx.type == TxType.INCOME) TxType.EXPENSE else TxType.INCOME).name,
            amount = tx.amount,
            source = tx.source,
            at = tx.occurredAt,
            from = tx.occurredAt - TRANSFER_WINDOW_MS,
            to = tx.occurredAt + TRANSFER_WINDOW_MS,
        ) ?: return
        dao.setTransferGroup(listOf(tx.id, other.id), UUID.randomUUID().toString())
    }

    suspend fun confirmTransfer(group: String) = lock.withLock { dao.confirmTransfer(group) }

    /** "Bukan, pisahkan": both rows go back to ordinary pending income and expense. */
    suspend fun splitTransfer(group: String) = lock.withLock { dao.clearTransfer(group) }

    suspend fun confirm(tx: MoneyTransaction) = lock.withLock { dao.setStatus(tx.id, TxStatus.CONFIRMED.name) }

    /** Notification rows are kept as DISCARDED so a repost does not bring them back. */
    suspend fun discard(tx: MoneyTransaction) = lock.withLock {
        // Without this side the move is gone; the partner counts as ordinary income or expense again.
        dao.byId(tx.id)?.transferGroup?.let { dao.clearTransfer(it) }
        if (tx.dedupKey == null) dao.delete(tx.id) else dao.setStatus(tx.id, TxStatus.DISCARDED.name)
    }

    suspend fun confirmedForExport(): List<MoneyTransaction> = dao.allConfirmed()

    suspend fun everythingForBackup(): List<MoneyTransaction> = dao.everything()

    /** Restore: the phone's rows are replaced by [items]. Ids are reassigned. */
    suspend fun replaceAll(items: List<MoneyTransaction>) = lock.withLock {
        inTransaction {
            dao.deleteAll()
            dao.insertAll(items.map { it.copy(id = 0) })
        }
    }

    private companion object {
        const val DUPLICATE_WINDOW_MS = 2 * 60_000L

        // Bank and e-wallet notifications for one top up usually land within a minute of each other.
        const val TRANSFER_WINDOW_MS = 10 * 60_000L
    }
}
