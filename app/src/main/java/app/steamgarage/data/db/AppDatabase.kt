package app.steamgarage.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Локальная база — главный источник данных. Схема экспортируется в app/schemas;
 * при изменении схемы повышается версия и пишется миграция (данные пользователя не стираются).
 */
@Database(
    entities = [
        CarEntity::class,
        ItemEntity::class,
        ServiceEntity::class,
        MileageEntity::class,
        GarageEntity::class,
        NotifyStateEntity::class,
        CarNotifyStateEntity::class,
        TemplateEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cars(): CarDao
    abstract fun items(): ItemDao
    abstract fun services(): ServiceDao
    abstract fun mileage(): MileageDao
    abstract fun garage(): GarageDao
    abstract fun notify(): NotifyDao
    abstract fun templates(): TemplateDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "steamgarage.db").build()
    }
}
