@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.homeinventory.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.homeinventory.InventoryViewModel
import com.homeinventory.data.*
import com.homeinventory.domain.*
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable fun InventoryApp(vm: InventoryViewModel) {
 val nav = rememberNavController(); val items by vm.items.collectAsStateWithLifecycle(); val categories by vm.categories.collectAsStateWithLifecycle()
 NavHost(nav, "dashboard") {
  composable("dashboard") { Dashboard(items, categories, { nav.navigate("inventory?status=$it") }, { nav.navigate("edit/0") }, { nav.navigate("inventory") }, {nav.navigate("categories")}, {nav.navigate("details/$it")}) }
  composable("inventory?status={status}", arguments=listOf(navArgument("status"){type=NavType.StringType; defaultValue="ALL"})) { entry -> InventoryScreen(items,categories,entry.arguments?.getString("status") ?: "ALL", {nav.popBackStack()}, {nav.navigate("details/$it")}, {nav.navigate("edit/0")}, {nav.navigate("categories")}) }
  composable("categories") { CategoriesScreen(categories, vm, {nav.popBackStack()}) }
  composable("details/{id}", arguments=listOf(navArgument("id"){type=NavType.LongType})) { e -> val item by vm.item(e.arguments!!.getLong("id")).collectAsStateWithLifecycle(null); item?.let { DetailsScreen(it,categories,{nav.popBackStack()},{nav.navigate("edit/${it.item.id}")},{vm.delete(it.item){nav.popBackStack()}}) } }
  composable("edit/{id}", arguments=listOf(navArgument("id"){type=NavType.LongType})) { e -> val id=e.arguments!!.getLong("id"); val existing by vm.item(id).collectAsStateWithLifecycle(null); EditScreen(existing,categories,{nav.popBackStack()},{item,photos-> vm.save(item,photos){nav.popBackStack()}},{name,done->vm.addCategory(name,done)}) }
 }
}
@Composable fun Top(title:String, back:()->Unit) = TopAppBar(title={Text(title)}, navigationIcon={IconButton(back){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back")}})
@Composable fun Dashboard(items:List<ItemWithPhotos>, categories:List<CategoryEntity>, open:(String)->Unit, add:()->Unit, all:()->Unit, manage:()->Unit, details:(Long)->Unit) {
    val soon = items.count { expiryStatus(it.item.expiryDate) == ExpiryStatus.SOON }
    val expired = items.count { expiryStatus(it.item.expiryDate) == ExpiryStatus.EXPIRED }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Home Inventory") }, actions = { IconButton(onClick = all) { Icon(Icons.Default.Inventory2, "Inventory") }; IconButton(onClick = manage) { Icon(Icons.Default.Category, "Categories") } }) },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = add, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add item") }) }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("At a glance", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { Stat("Expiring in 7 days", soon, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f)) { open("SOON") }; Stat("Expired", expired, MaterialTheme.colorScheme.error, Modifier.weight(1f)) { open("EXPIRED") } } }
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Recently added", style = MaterialTheme.typography.titleLarge); TextButton(onClick = all) { Text("View all") } } }
            if (items.isEmpty()) item { Empty("No items yet. Add your first item.") } else items(items.take(5), key = { it.item.id }) { ItemCard(it, categories) { details(it.item.id) } }
        }
    }
}
@Composable private fun Stat(label:String,count:Int,color:androidx.compose.ui.graphics.Color, modifier: Modifier, click:()->Unit) { Card(modifier.clickable(onClick=click),colors=CardDefaults.cardColors(containerColor=color.copy(alpha=.15f))){Column(Modifier.padding(16.dp)){Text("$count",style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Bold,color=color);Text(label)}} }
@Composable fun InventoryScreen(items:List<ItemWithPhotos>, categories:List<CategoryEntity>, initial:String,back:()->Unit,details:(Long)->Unit,add:()->Unit,manage:()->Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf(initial) }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    val filtered = items.filter { (query.isBlank() || it.item.name.contains(query, true)) && (categoryId == null || it.item.categoryId == categoryId) && (status == "ALL" || expiryStatus(it.item.expiryDate).name == status) }
    Scaffold(topBar = { Top("Inventory", back) }, floatingActionButton = { FloatingActionButton(onClick = add) { Icon(Icons.Default.Add, "Add") } }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Search items") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
            Spacer(Modifier.height(8.dp)); FilterRow(status, { status = it }, categoryId, { categoryId = it }, categories, manage); Spacer(Modifier.height(8.dp))
            if (filtered.isEmpty()) Empty("No matching inventory items.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(filtered, key = { it.item.id }) { ItemCard(it, categories) { details(it.item.id) } } }
        }
    }
}
@Composable fun FilterRow(status:String,setStatus:(String)->Unit,category:Long?,setCategory:(Long?)->Unit,cats:List<CategoryEntity>,manage:()->Unit) { var exp by remember {mutableStateOf(false)}; Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Box{FilterChip(status!="ALL",{exp=true},{Text(if(status=="ALL")"Expiry" else status.replace('_',' '))});DropdownMenu(exp,{exp=false}){listOf("ALL","GOOD","SOON","EXPIRED","NO_EXPIRY").forEach{DropdownMenuItem({Text(it.replace('_',' '))},{setStatus(it);exp=false})}}}; var ce by remember{mutableStateOf(false)};Box{FilterChip(category!=null,{ce=true},{Text(cats.firstOrNull{it.id==category}?.name ?: "Category")});DropdownMenu(ce,{ce=false}){DropdownMenuItem({Text("All categories")},{setCategory(null);ce=false});cats.forEach{c->DropdownMenuItem({Text(c.name)},{setCategory(c.id);ce=false})};HorizontalDivider();DropdownMenuItem({Text("Manage categories")},manage)}}} }
@Composable fun ItemCard(row:ItemWithPhotos,cats:List<CategoryEntity>,click:()->Unit) { val i=row.item; val state=expiryStatus(i.expiryDate); Card(Modifier.fillMaxWidth().clickable(onClick=click)){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){if(row.photos.isNotEmpty())AsyncImage(row.photos.first().uri,"",Modifier.size(54.dp).clip(RoundedCornerShape(10.dp)),contentScale=ContentScale.Crop) else Icon(Icons.Default.Inventory2,null,Modifier.size(44.dp));Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(i.name,style=MaterialTheme.typography.titleMedium);Text(cats.firstOrNull{it.id==i.categoryId}?.name ?: "Uncategorized");Text(i.primaryLocation,style=MaterialTheme.typography.bodySmall)}; Status(state) } }
}
@Composable fun Status(s:ExpiryStatus){val c=when(s){ExpiryStatus.EXPIRED->MaterialTheme.colorScheme.error;ExpiryStatus.SOON->MaterialTheme.colorScheme.tertiary;ExpiryStatus.GOOD->MaterialTheme.colorScheme.primary;ExpiryStatus.NO_EXPIRY->MaterialTheme.colorScheme.outline};AssistChip({}, {Text(when(s){ExpiryStatus.NO_EXPIRY->"No expiry";ExpiryStatus.SOON->"Soon";ExpiryStatus.EXPIRED->"Expired";ExpiryStatus.GOOD->"Good"})},colors=AssistChipDefaults.assistChipColors(labelColor=c))}
@Composable fun Empty(text: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
