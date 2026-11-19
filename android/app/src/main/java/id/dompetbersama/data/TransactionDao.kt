package id.dompetbersama.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE status = 'CONFIRMED' ORDER BY occurredAt DESC")
    fun observeConfirmed(): Flow<List<MoneyTransaction>>

    @Query("SELECT * FROM transactions WHERE status = 'PENDING' ORDER BY occurredAt DESC")
    fun observePending(): Flow<List<MoneyTransaction>>

    @Query("SELECT * FROM transactions WHERE status = 'CONFIRMED' ORDER BY occurredAt ASC")
    suspend fun allConfirmed(): List<MoneyTransaction>

    /** Any status, discarded rows included: a repost of a discarded notification stays discarded. */
    @Query(
        """SELECT COUNT(*) FROM transactions
           WHERE source = :source AND type = :type AND amount = :amount AND note = :note
             AND occurredAt BETWEEN :from AND :to""",
    )
    suspend fun countSimilar(source: String, type: String, amount: Long, note: String, from: Long, to: Long): Int

    /**
     * The other side of a move between own wallets: opposite direction, same amount, another app,
     * still waiting for confirmation and not already paired. Closest in time wins.
     */
    @Query(
        """SELECT * FROM transactions
           WHERE status = 'PENDING' AND transferGroup IS NULL AND id != :id
             AND type = :type AND amount = :amount AND source != :source
             AND occurredAt BETWEEN :from AND :to
           ORDER BY ABS(occurredAt - :at) LIMIT 1""",
    )
    suspend fun findTransferCounterpart(id: Long, type: String, amount: Long, source: String, at: Long, from: Long, to: Long): MoneyTransaction?

    /** Every row, discarded ones included: they stop the same notification from coming back after a restore. */
    @Query("SELECT * FROM transactions ORDER BY occurredAt ASC")
    suspend fun everything(): List<MoneyTransaction>

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(items: List<MoneyTransaction>)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun byId(id: Long): MoneyTransaction?

    /** Status only: a whole-row update from a stale copy would undo a pairing made in the meantime. */
    @Query("UPDATE transactions SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: String)

    @Query("UPDATE transactions SET transferGroup = :group WHERE id IN (:ids)")
    suspend fun setTransferGroup(ids: List<Long>, group: String)

    @Query("UPDATE transactions SET status = 'CONFIRMED' WHERE transferGroup = :group AND status = 'PENDING'")
    suspend fun confirmTransfer(group: String)

    @Query("UPDATE transactions SET transferGroup = NULL WHERE transferGroup = :group")
    suspend fun clearTransfer(group: String)

    /** Returns -1 when the dedupKey already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tx: MoneyTransaction): Long

    @Update
    suspend fun update(tx: MoneyTransaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)
}
