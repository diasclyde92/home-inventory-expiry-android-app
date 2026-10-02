@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.homeinventory.ui

import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.homeinventory.InventoryViewModel
import com.homeinventory.data.*
import com.homeinventory.domain.datesValid
import java.io.File
import java.time.LocalDate

@Composable fun DetailsScreen(row:ItemWithPhotos,cats:List<CategoryEntity>,back:()->Unit,edit:()->Unit,delete:()->Unit){var confirm by remember{mutableStateOf(false)};Scaffold(topBar={TopAppBar(title={Text("Item details")},navigationIcon={IconButton(back){Icon(Icons.Default.ArrowBack,"Back")}},actions={IconButton(edit){Icon(Icons.Default.Edit,"Edit")};IconButton({confirm=true}){Icon(Icons.Default.Delete,"Delete")}})}){p->val i=row.item;Column(Modifier.padding(p).padding(20.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){Text(i.name,style=MaterialTheme.typography.headlineMedium);Text(cats.firstOrNull{it.id==i.categoryId}?.name?:"Uncategorized");Status(com.homeinventory.domain.expiryStatus(i.expiryDate));Info("Manufactured",i.manufacturedDate);Info("Expiry",i.expiryDate);Info("Primary location",i.primaryLocation);if(!i.alternateLocation.isNullOrBlank())Info("Alternate location",i.alternateLocation);if(!i.description.isNullOrBlank())Info("Notes",i.description);Text("Photos",style=MaterialTheme.typography.titleMedium);if(row.photos.isEmpty())Text("No photos added.") else LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){items(row.photos){photo->AsyncImage(photo.uri,"Attached photo",Modifier.size(160.dp).clip(MaterialTheme.shapes.medium),contentScale=ContentScale.Crop)}};Text("Added ${java.text.DateFormat.getDateInstance().format(i.createdAt)}",style=MaterialTheme.typography.bodySmall)} };if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text("Delete item?")},text={Text("This permanently removes this item from your inventory.")},dismissButton={TextButton({confirm=false}){Text("Cancel")}},confirmButton={Button(delete){Text("Delete")}})}
@Composable private fun Info(label:String,value:String?){if(!value.isNullOrBlank())Column{Text(label,style=MaterialTheme.typography.labelMedium);Text(value)}}

@Composable fun CategoriesScreen(cats: List<CategoryEntity>, vm: InventoryViewModel, back: () -> Unit) {
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }
    var deleting by remember { mutableStateOf<CategoryEntity?>(null) }
    Scaffold(topBar = { Top("Categories", back) }, floatingActionButton = { FloatingActionButton({ adding = true }) { Icon(Icons.Default.Add, "Add") } }) { padding ->
        androidx.compose.foundation.lazy.LazyColumn(Modifier.padding(padding)) {
            items(cats, key = { it.id }) { category ->
                ListItem(headlineContent = { Text(category.name) }, trailingContent = { Row { IconButton({ editing = category }) { Icon(Icons.Default.Edit, "Rename") }; IconButton({ deleting = category }) { Icon(Icons.Default.Delete, "Delete") } } })
                HorizontalDivider()
            }
        }
    }
    if (adding) CategoryNameDialog("Add category", "") { vm.addCategory(it); adding = false }
    editing?.let { category -> CategoryNameDialog("Rename category", category.name) { vm.rename(category, it); editing = null } }
    deleting?.let { category -> DeleteCategoryDialog(category, cats, vm) { deleting = null } }
}
@Composable private fun CategoryNameDialog(title:String,initial:String,ok:(String)->Unit){var name by remember{mutableStateOf(initial)};var show by remember{mutableStateOf(true)};if(show) AlertDialog(onDismissRequest={show=false},title={Text(title)},text={OutlinedTextField(name,{name=it},label={Text("Category name")})},dismissButton={TextButton({show=false}){Text("Cancel")}},confirmButton={Button({ok(name);show=false},enabled=name.isNotBlank()){Text("Save")}})}
@Composable private fun DeleteCategoryDialog(c:CategoryEntity,cats:List<CategoryEntity>,vm:InventoryViewModel,dismiss:()->Unit){var replacement by remember{mutableStateOf(cats.firstOrNull{it.id!=c.id}?.id)};AlertDialog(onDismissRequest=dismiss,title={Text("Delete ${c.name}?")},text={Column{Text("Items will be moved safely to another category.");cats.filter{it.id!=c.id}.forEach{x->Row(Modifier.fillMaxWidth().clickable{replacement=x.id},verticalAlignment=Alignment.CenterVertically){RadioButton(replacement==x.id,{replacement=x.id});Text(x.name)}}}},dismissButton={TextButton(dismiss){Text("Cancel")}},confirmButton={Button({replacement?.let{vm.deleteCategory(c,it);dismiss()}},enabled=replacement!=null){Text("Move & delete")}})}

@Composable fun EditScreen(existing:ItemWithPhotos?,cats:List<CategoryEntity>,back:()->Unit,save:(ItemEntity,List<String>)->Unit,addCategory:(String,(Long)->Unit)->Unit){val context=LocalContext.current;var name by rememberSaveable(existing?.item?.id){mutableStateOf(existing?.item?.name?:"")};var cat by rememberSaveable(existing?.item?.id){mutableStateOf(existing?.item?.categoryId)};var made by rememberSaveable(existing?.item?.id){mutableStateOf(existing?.item?.manufacturedDate?:"")};var expiry by rememberSaveable(existing?.item?.id){mutableStateOf(existing?.item?.expiryDate?:"")};var location by rememberSaveable(existing?.item?.id){mutableStateOf(existing?.item?.primaryLocation?:"")};var alternate by rememberSaveable(existing?.item?.id){mutableStateOf(existing?.item?.alternateLocation?:"")};var notes by rememberSaveable(existing?.item?.id){mutableStateOf(existing?.item?.description?:"")};var photos by rememberSaveable(existing?.item?.id){mutableStateOf<List<String>>(existing?.photos?.map{it.uri}?:emptyList())};var error by remember{mutableStateOf<String?>(null)};var createCategory by remember{mutableStateOf(false)}
 var pendingCameraUriString by rememberSaveable(existing?.item?.id) { mutableStateOf<String?>(null) }
 val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
  if (uris.isNotEmpty()) {
   val copiedUris = uris.mapNotNull { uri -> copyToInternalStorage(context, uri) }
   photos = (photos + copiedUris).distinct()
  }
 }
 val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
  val uriString = pendingCameraUriString
  if (success && uriString != null) {
   photos = (photos + uriString).distinct()
  } else if (!success && uriString != null) {
   // If cancelled or failed, delete the empty allocated file
   deleteAllocatedCameraFile(context, uriString)
  }
  pendingCameraUriString = null
 }
 Scaffold(topBar={Top(if(existing==null)"Add item" else "Edit item",back)}){p->Column(Modifier.padding(p).padding(16.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){error?.let{Text(it,color=MaterialTheme.colorScheme.error)};OutlinedTextField(name,{name=it},Modifier.fillMaxWidth(),label={Text("Item name *")});var catMenu by remember{mutableStateOf(false)};Box{OutlinedButton({catMenu=true},Modifier.fillMaxWidth()){Text(cats.firstOrNull{it.id==cat}?.name?:"Select category *")};DropdownMenu(catMenu,{catMenu=false}){cats.forEach{c->DropdownMenuItem({Text(c.name)},{cat=c.id;catMenu=false})};DropdownMenuItem({Text("+ Create category")},{createCategory=true;catMenu=false})}};DateField("Manufactured date",made){made=it};DateField("Expiry date",expiry){expiry=it};OutlinedTextField(location,{location=it},Modifier.fillMaxWidth(),label={Text("Primary location *")});OutlinedTextField(alternate,{alternate=it},Modifier.fillMaxWidth(),label={Text("Alternate location")});OutlinedTextField(notes,{notes=it},Modifier.fillMaxWidth(),label={Text("Description / notes")},minLines=3);Text("Photos",style=MaterialTheme.typography.titleMedium);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton({gallery.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))}){Icon(Icons.Default.Photo,null);Text(" Gallery")};OutlinedButton({
  val imagesDir = File(context.filesDir, "images").apply { mkdirs() }
  val newFile = File(imagesDir, "IMG_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(8)}.jpg")
  val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", newFile)
  pendingCameraUriString = uri.toString()
  camera.launch(uri)
 }){Icon(Icons.Default.CameraAlt,null);Text(" Camera")}};if(photos.isEmpty())Text("No photos added.") else LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){items(photos){u->Box{AsyncImage(u,"Photo",Modifier.size(100.dp).clip(MaterialTheme.shapes.small),contentScale=ContentScale.Crop);IconButton({photos=photos-u},Modifier.align(Alignment.TopEnd)){Icon(Icons.Default.Close,"Remove")}}}};Button({if(name.isBlank()||cat==null||location.isBlank())error="Name, category, and primary location are required." else if(!datesValid(made.ifBlank{null},expiry.ifBlank{null}))error="Expiry date cannot be before manufactured date." else save(ItemEntity(id=existing?.item?.id?:0,name=name.trim(),categoryId=cat!!,manufacturedDate=made.ifBlank{null},expiryDate=expiry.ifBlank{null},primaryLocation=location.trim(),alternateLocation=alternate.ifBlank{null},description=notes.ifBlank{null},createdAt=existing?.item?.createdAt?:System.currentTimeMillis()),photos)},Modifier.fillMaxWidth()){Text("Save item")}}};if(createCategory)CategoryNameDialog("New category",""){n->addCategory(n){cat=it};createCategory=false}}
private fun copyToInternalStorage(context: Context, sourceUri: Uri): String? {
 return try {
  val imagesDir = File(context.filesDir, "images").apply { mkdirs() }
  val targetFile = File(imagesDir, "GAL_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(8)}.jpg")
  context.contentResolver.openInputStream(sourceUri)?.use { input ->
   targetFile.outputStream().use { output ->
    input.copyTo(output)
   }
  }
  FileProvider.getUriForFile(context, "${context.packageName}.files", targetFile).toString()
 } catch (_: Exception) {
  null
 }
}
private fun deleteAllocatedCameraFile(context: Context, uriString: String) {
 try {
  val parsed = Uri.parse(uriString)
  if (parsed.authority == "${context.packageName}.files") {
   val relative = parsed.path?.removePrefix("/images/")?.removePrefix("/")
   if (!relative.isNullOrBlank()) {
    File(File(context.filesDir, "images"), relative.substringAfterLast('/')).takeIf { it.exists() }?.delete()
   }
  }
 } catch (_: Exception) {}
}

@Composable private fun DateField(label:String,value:String,on:(String)->Unit){val c=LocalContext.current;OutlinedTextField(value,{on(it)},Modifier.fillMaxWidth(),label={Text("$label (YYYY-MM-DD)")},trailingIcon={IconButton({val d=LocalDate.now();DatePickerDialog(c,{_,y,m,day->on(LocalDate.of(y,m+1,day).toString())},d.year,d.monthValue-1,d.dayOfMonth).show()}){Icon(Icons.Default.DateRange,"Pick date")}},singleLine=true)}
