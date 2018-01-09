package com.dis.ajcra.distest2

import android.arch.persistence.room.*

/**
 * Created by ajcra on 1/3/2018.
 */

@Entity
class CloudFileInfo {
    @PrimaryKey
    var key: String = ""
    var fileURI: String? = null
    var lastAccessed: Long = 0
    var lastUpdated: Long = 0

    @Ignore
    override fun toString(): String {
        var desc = "Key: " + key + "\n"
        if (fileURI != null) {
            desc += "FileURI: " + fileURI + "\n"
        }
        desc += "LastAccessed: " + lastAccessed.toString() + "\n"
        desc += "LastUpdated: " + lastUpdated.toString() + "\n"
        return desc
    }
}

@Dao
interface CloudFileInfoDao {
    @Query("SELECT fileURI FROM CloudFileInfo ORDER BY lastAccessed")
    fun getFileNamesOldestToNewest(): List<String>

    @Query("SELECT * FROM CloudFileInfo ORDER BY lastAccessed")
    fun getCloudFileInfosOldestToNewest(): List<CloudFileInfo>

    @Query("SELECT * FROM CloudFileInfo ORDER BY lastAccessed DESC")
    fun getCloudFileInfosNewestToOldest(): List<CloudFileInfo>

    @Query("SELECT * FROM CloudFileInfo WHERE key = :arg0")
    fun getCloudFileInfo(key: String): CloudFileInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addCloudFileInfo(cfi: CloudFileInfo)

    @Delete
    fun delete(cloudFileInfo: CloudFileInfo)

    @Query("DELETE FROM CloudFileInfo")
    fun deleteAll()
}

@Database(entities = arrayOf(CloudFileInfo::class), version=2)
abstract class CloudFileDatabase: RoomDatabase() {
    abstract fun cloudFileInfoDao(): CloudFileInfoDao
}