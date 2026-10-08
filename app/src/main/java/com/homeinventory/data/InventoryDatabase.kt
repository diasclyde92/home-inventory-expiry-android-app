package com.homeinventory.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "categories") data class CategoryEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String)
@Entity(tableName = "items", foreignKeys = [ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.RESTRICT)], indices = [Index("categoryId")])
data class ItemEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val categoryId: Long, val manufacturedDate: String? = null, val expiryDate: String? = null, val primaryLocation: String, val alternateLocation: String? = null, val description: String? = null, val createdAt: Long = System.currentTimeMillis(), val updatedAt: Long = System.currentTimeMillis())
@Entity(tableName = "photos", foreignKeys = [ForeignKey(entity = ItemEntity::class, parentColumns = ["id"], childColumns = ["itemId"], onDelete = ForeignKey.CASCADE)], indices = [Index("itemId")]) data class PhotoEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val itemId: Long, val uri: String)
data class ItemWithPhotos(@Embedded val item: ItemEntity, @Relation(parentColumn = "id", entityColumn = "itemId") val photos: List<PhotoEntity>)

@Dao interface InventoryDao {
 @Transaction @Query("SELECT * FROM items ORDER BY createdAt DESC") fun observeItems(): Flow<List<ItemWithPhotos>>
 @Transaction @Query("SELECT * FROM items WHERE id=:id") fun observeItem(id: Long): Flow<ItemWithPhotos?>
 @Query("SELECT * FROM categories ORDER BY name") fun observeCategories(): Flow<List<CategoryEntity>>
 @Insert suspend fun addCategory(category: CategoryEntity): Long
 @Update suspend fun updateCategory(category: CategoryEntity)
 @Delete suspend fun deleteCategory(category: CategoryEntity)
 @Query("SELECT COUNT(*) FROM items WHERE categoryId=:id") suspend fun itemCount(id: Long): Int
 @Query("UPDATE items SET categoryId=:replacement WHERE categoryId=:old") suspend fun moveItems(old: Long, replacement: Long)
 @Insert suspend fun insertItem(item: ItemEntity): Long
 @Update suspend fun updateItem(item: ItemEntity)
 @Delete suspend fun deleteItem(item: ItemEntity)
 @Insert suspend fun insertPhotos(photos: List<PhotoEntity>)
 @Query("DELETE FROM photos WHERE itemId=:itemId") suspend fun clearPhotos(itemId: Long)
 @Query("SELECT * FROM photos WHERE itemId=:itemId") suspend fun getPhotosByItemId(itemId: Long): List<PhotoEntity>
 @Query("SELECT COUNT(*) FROM photos WHERE uri=:uri") suspend fun photoUriCount(uri: String): Int
 @Query("SELECT * FROM photos") suspend fun getAllPhotos(): List<PhotoEntity>
 @Query("UPDATE photos SET uri=:newUri WHERE id=:id") suspend fun updatePhotoUri(id: Long, newUri: String)
}
@Database(entities = [CategoryEntity::class, ItemEntity::class, PhotoEntity::class], version = 1) abstract class InventoryDatabase: RoomDatabase() { abstract fun dao(): InventoryDao
 companion object {
  @Volatile private var INSTANCE: InventoryDatabase? = null

  fun getInstance(context: Context): InventoryDatabase =
   INSTANCE ?: synchronized(this) {
    INSTANCE ?: Room.databaseBuilder(
     context.applicationContext,
     InventoryDatabase::class.java,
     "inventory.db"
    ).build().also { INSTANCE = it }
   }
 }
}

