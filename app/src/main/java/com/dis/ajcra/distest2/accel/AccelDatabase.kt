package com.dis.ajcra.distest2.accel

import android.arch.persistence.room.*

@Entity
class RideMatch {
    var name: String = ""
    @PrimaryKey
    var time: Long = 0
    var distance: Double = 0.0
}

@Entity
class MovementMatch {
    var name: String = ""
    var confidence: Int = 0
    @PrimaryKey
    var time: Long = 0
}

@Dao
interface AccelDatabaseDao {
    @Query("SELECT * FROM RideMatch ORDER BY time DESC")
    fun listRideMatches(): List<RideMatch>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRideMatch(rideMatch: RideMatch)

    @Query("SELECT * FROM MovementMatch ORDER BY time DESC")
    fun listMovementMatches(): List<MovementMatch>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMovementMatch(rideMatch: MovementMatch)
}

@Database(entities = arrayOf(RideMatch::class, MovementMatch::class), version=1)
abstract class AccelDatabase: RoomDatabase() {
    abstract fun accelDatabaseDao(): AccelDatabaseDao
}