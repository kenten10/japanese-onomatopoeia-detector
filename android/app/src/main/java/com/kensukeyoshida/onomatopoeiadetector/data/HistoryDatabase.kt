package com.kensukeyoshida.onomatopoeiadetector.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "history")
data class HistoryRecord(
    @PrimaryKey val id: String,
    val inputText: String,
    val score: Int,
    val date: Long
)

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY date DESC LIMIT 100")
    suspend fun fetch(): List<HistoryRecord>

    @Insert
    suspend fun insert(record: HistoryRecord)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM history")
    suspend fun deleteAll()

    /** 最新 100 件だけを残す（iOS 版 `pruneIfNeeded` と同じ上限）。 */
    @Query("DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY date DESC LIMIT 100)")
    suspend fun prune()
}

@Database(entities = [HistoryRecord::class], version = 1, exportSchema = false)
abstract class HistoryDatabase : RoomDatabase() {

    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var instance: HistoryDatabase? = null

        fun get(context: Context): HistoryDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    HistoryDatabase::class.java,
                    "onomatopoeia-history"
                ).build().also { instance = it }
            }
    }
}
