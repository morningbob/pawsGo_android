package com.bitpunchlab.android.pawsgo.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bitpunchlab.android.pawsgo.modelsRoom.DogRoom
import com.bitpunchlab.android.pawsgo.modelsRoom.MessageRoom
import com.bitpunchlab.android.pawsgo.modelsRoom.UserRoom

@Dao
interface PawsDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(user: UserRoom)

    @Query("SELECT * FROM user_table WHERE :id == userID LIMIT 1")
    fun getUser(id: String) : LiveData<UserRoom>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDogs(vararg dog: DogRoom)

    @Query("SELECT * FROM dog_table WHERE :id == dogID LIMIT 1")
    fun getDog(id: String) : LiveData<DogRoom>

    @Query("SELECT * FROM dog_table WHERE isLost == 1")
    fun getAllLostDogs() : LiveData<List<DogRoom>>

    @Query("SELECT * FROM dog_table WHERE isLost == 0")
    fun getAllFoundDogs() : LiveData<List<DogRoom>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessages(vararg message: MessageRoom)
}