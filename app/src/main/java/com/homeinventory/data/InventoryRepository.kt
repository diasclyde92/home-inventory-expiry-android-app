package com.homeinventory.data
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class InventoryRepository(private val dao: InventoryDao, private val context: Context) {
 val items = dao.observeItems(); val categories = dao.observeCategories()
 fun item(id: Long) = dao.observeItem(id)

 suspend fun save(item: ItemEntity, uris: List<String>): Long {
  val existingPhotos = if (item.id != 0L) dao.getPhotosByItemId(item.id).map { it.uri } else emptyList()
  val id = if (item.id == 0L) dao.insertItem(item) else { dao.updateItem(item.copy(updatedAt = System.currentTimeMillis())); item.id }
  val distinctUris = uris.distinct()
  dao.clearPhotos(id)
  dao.insertPhotos(distinctUris.map { PhotoEntity(itemId = id, uri = it) })

  val removedPhotos = existingPhotos - distinctUris.toSet()
  cleanupUnusedFiles(removedPhotos)
  return id
 }

 suspend fun delete(item: ItemEntity) {
  val photosToDelete = dao.getPhotosByItemId(item.id).map { it.uri }
  dao.deleteItem(item)
  cleanupUnusedFiles(photosToDelete)
 }

 suspend fun addCategory(name: String) = dao.addCategory(CategoryEntity(name = name.trim()))
 suspend fun rename(category: CategoryEntity, name: String) = dao.updateCategory(category.copy(name = name.trim()))
 suspend fun deleteCategory(category: CategoryEntity, replacement: Long) { dao.moveItems(category.id, replacement); dao.deleteCategory(category) }
 suspend fun used(id: Long) = dao.itemCount(id)

 private suspend fun cleanupUnusedFiles(uris: List<String>) = withContext(Dispatchers.IO) {
  uris.forEach { uriString ->
   try {
    val remainingUses = dao.photoUriCount(uriString)
    if (remainingUses == 0) {
     val parsed = Uri.parse(uriString)
     if (parsed.scheme == "file") {
      parsed.path?.let { File(it).takeIf { f -> f.exists() }?.delete() }
     } else if (parsed.scheme == "content" && parsed.authority == "${context.packageName}.files") {
      val relativePath = parsed.path?.removePrefix("/images/")?.removePrefix("/")
      val targetFile = if (!relativePath.isNullOrBlank()) {
       File(File(context.filesDir, "images"), relativePath.substringAfterLast('/'))
      } else null
      if (targetFile != null && targetFile.exists()) {
       targetFile.delete()
      }
     }
    }
   } catch (_: Exception) {}
  }
 }
}
