package com.kensukeyoshida.onomatopoeiadetector.data

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import com.kensukeyoshida.onomatopoeiadetector.R
import com.kensukeyoshida.onomatopoeiadetector.model.HistoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/** 永続化の失敗をユーザー向け文言に対応付ける例外。 */
class PersistenceException(
    @StringRes val messageRes: Int,
    cause: Throwable
) : Exception(cause)

/**
 * 判定履歴の永続化。最大 100 件を新しい順に保持する。
 */
class HistoryRepository(context: Context) {

    private val dao = HistoryDatabase.get(context).historyDao()

    suspend fun add(inputText: String, score: Int): Unit = withContext(Dispatchers.IO) {
        try {
            dao.insert(
                HistoryRecord(
                    id = UUID.randomUUID().toString(),
                    inputText = inputText,
                    score = score,
                    date = System.currentTimeMillis()
                )
            )
            dao.prune()
        } catch (error: Exception) {
            Log.e(TAG, "insert failed", error)
            throw PersistenceException(R.string.error_persistence_save, error)
        }
    }

    suspend fun fetch(): List<HistoryItem> = withContext(Dispatchers.IO) {
        try {
            dao.fetch().map { HistoryItem(it.id, it.inputText, it.score, it.date) }
        } catch (error: Exception) {
            Log.e(TAG, "fetch failed", error)
            throw PersistenceException(R.string.error_persistence_load, error)
        }
    }

    suspend fun delete(item: HistoryItem): Unit = withContext(Dispatchers.IO) {
        try {
            dao.deleteById(item.id)
        } catch (error: Exception) {
            Log.e(TAG, "delete failed", error)
            throw PersistenceException(R.string.error_persistence_delete, error)
        }
    }

    suspend fun deleteAll(): Unit = withContext(Dispatchers.IO) {
        try {
            dao.deleteAll()
        } catch (error: Exception) {
            Log.e(TAG, "deleteAll failed", error)
            throw PersistenceException(R.string.error_persistence_delete, error)
        }
    }

    private companion object {
        const val TAG = "OnoHistory"
    }
}
