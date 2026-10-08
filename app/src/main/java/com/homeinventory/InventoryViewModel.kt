package com.homeinventory
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.homeinventory.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class InventoryViewModel(application: Application): AndroidViewModel(application) {
 private val repo = InventoryRepository(InventoryDatabase.getInstance(application).dao(), application)
 val items = repo.items.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
 val categories = repo.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

 init {
  viewModelScope.launch {
   repo.migrateLegacyPhotoUris()
  }
 }
 fun item(id:Long) = repo.item(id)
 fun save(item:ItemEntity, photos:List<String>, done:(Long)->Unit) = viewModelScope.launch { done(repo.save(item,photos)) }
 fun delete(item:ItemEntity, done:()->Unit={}) = viewModelScope.launch { repo.delete(item); done() }
 fun addCategory(name:String, done:(Long)->Unit={}) = viewModelScope.launch { if(name.isNotBlank()) done(repo.addCategory(name)) }
 fun rename(category:CategoryEntity,name:String) = viewModelScope.launch { if(name.isNotBlank()) repo.rename(category,name) }
 fun deleteCategory(category:CategoryEntity,replacement:Long) = viewModelScope.launch { repo.deleteCategory(category,replacement) }
 fun used(id:Long, result:(Int)->Unit) = viewModelScope.launch { result(repo.used(id)) }
}
