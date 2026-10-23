package app.steamgarage.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDao {
    @Query("SELECT * FROM cars WHERE deleted = 0 ORDER BY name COLLATE NOCASE")
    fun observeAlive(): Flow<List<CarEntity>>

    @Query("SELECT * FROM cars WHERE id = :id")
    suspend fun get(id: String): CarEntity?

    @Query("SELECT * FROM cars")
    suspend fun all(): List<CarEntity>

    @Upsert
    suspend fun upsert(entity: CarEntity)

    @Upsert
    suspend fun upsertAll(entities: List<CarEntity>)

    @Query("SELECT * FROM cars WHERE dirty = 1 AND garageId = :garageId")
    suspend fun dirty(garageId: String): List<CarEntity>

    @Query("UPDATE cars SET dirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markClean(id: String, updatedAt: Long)

    @Query("DELETE FROM cars WHERE garageId = :garageId")
    suspend fun deleteForGarage(garageId: String)

    @Query("UPDATE cars SET garageId = NULL, dirty = 0 WHERE garageId = :garageId")
    suspend fun detachGarage(garageId: String)
}

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE deleted = 0 ORDER BY name COLLATE NOCASE")
    fun observeAlive(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun get(id: String): ItemEntity?

    @Query("SELECT * FROM items WHERE carId = :carId")
    suspend fun byCar(carId: String): List<ItemEntity>

    @Query("SELECT * FROM items")
    suspend fun all(): List<ItemEntity>

    @Upsert
    suspend fun upsert(entity: ItemEntity)

    @Upsert
    suspend fun upsertAll(entities: List<ItemEntity>)

    @Query("SELECT * FROM items WHERE dirty = 1 AND garageId = :garageId")
    suspend fun dirty(garageId: String): List<ItemEntity>

    @Query("UPDATE items SET dirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markClean(id: String, updatedAt: Long)

    @Query("DELETE FROM items WHERE garageId = :garageId")
    suspend fun deleteForGarage(garageId: String)

    @Query("UPDATE items SET garageId = NULL, dirty = 0 WHERE garageId = :garageId")
    suspend fun detachGarage(garageId: String)
}

@Dao
interface ServiceDao {
    @Query("SELECT * FROM services WHERE deleted = 0 ORDER BY date DESC, mileage DESC")
    fun observeAlive(): Flow<List<ServiceEntity>>

    @Query("SELECT * FROM services WHERE id = :id")
    suspend fun get(id: String): ServiceEntity?

    @Query("SELECT * FROM services WHERE carId = :carId")
    suspend fun byCar(carId: String): List<ServiceEntity>

    @Query("SELECT * FROM services WHERE itemId = :itemId")
    suspend fun byItem(itemId: String): List<ServiceEntity>

    @Query("SELECT * FROM services")
    suspend fun all(): List<ServiceEntity>

    @Upsert
    suspend fun upsert(entity: ServiceEntity)

    @Upsert
    suspend fun upsertAll(entities: List<ServiceEntity>)

    @Query("SELECT * FROM services WHERE dirty = 1 AND garageId = :garageId")
    suspend fun dirty(garageId: String): List<ServiceEntity>

    @Query("UPDATE services SET dirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markClean(id: String, updatedAt: Long)

    @Query("DELETE FROM services WHERE garageId = :garageId")
    suspend fun deleteForGarage(garageId: String)

    @Query("UPDATE services SET garageId = NULL, dirty = 0 WHERE garageId = :garageId")
    suspend fun detachGarage(garageId: String)
}

@Dao
interface MileageDao {
    @Query("SELECT * FROM mileage WHERE deleted = 0 ORDER BY date DESC, km DESC")
    fun observeAlive(): Flow<List<MileageEntity>>

    @Query("SELECT * FROM mileage WHERE carId = :carId")
    suspend fun byCar(carId: String): List<MileageEntity>

    @Query("SELECT * FROM mileage WHERE serviceId = :serviceId")
    suspend fun byService(serviceId: String): List<MileageEntity>

    @Query("SELECT * FROM mileage")
    suspend fun all(): List<MileageEntity>

    @Upsert
    suspend fun upsert(entity: MileageEntity)

    @Upsert
    suspend fun upsertAll(entities: List<MileageEntity>)

    @Query("SELECT * FROM mileage WHERE dirty = 1 AND garageId = :garageId")
    suspend fun dirty(garageId: String): List<MileageEntity>

    @Query("UPDATE mileage SET dirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markClean(id: String, updatedAt: Long)

    @Query("DELETE FROM mileage WHERE garageId = :garageId")
    suspend fun deleteForGarage(garageId: String)

    @Query("UPDATE mileage SET garageId = NULL, dirty = 0 WHERE garageId = :garageId")
    suspend fun detachGarage(garageId: String)
}

@Dao
interface GarageDao {
    @Query("SELECT * FROM garage LIMIT 1")
    fun observe(): Flow<GarageEntity?>

    @Query("SELECT * FROM garage LIMIT 1")
    suspend fun get(): GarageEntity?

    @Upsert
    suspend fun upsert(entity: GarageEntity)

    @Query("DELETE FROM garage")
    suspend fun clear()
}

@Dao
interface NotifyDao {
    @Query("SELECT * FROM notify_state")
    suspend fun itemStates(): List<NotifyStateEntity>

    @Query("SELECT * FROM notify_state WHERE itemId = :itemId")
    suspend fun itemState(itemId: String): NotifyStateEntity?

    @Upsert
    suspend fun upsertItemState(entity: NotifyStateEntity)

    @Query("SELECT * FROM car_notify_state")
    suspend fun carStates(): List<CarNotifyStateEntity>

    @Upsert
    suspend fun upsertCarState(entity: CarNotifyStateEntity)
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TemplateEntity>>

    @Upsert
    suspend fun upsert(entity: TemplateEntity)

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun delete(id: String)
}
