package com.nextystudio.nextylibrary

import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import coil.compose.AsyncImage
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

private val Ink = Color(0xFF14251F)
private val Green = Color(0xFF16765A)
private val Paper = Color(0xFFF3F7F3)
private val Mint = Color(0xFFDDF1E6)
private val Muted = Color(0xFF74847B)

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val description: String,
    val categoryId: String?,
    val pageCount: Int,
    val coverPath: String?
)

data class BookCategory(val id: String, val name: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NextyLibraryApp() }
    }
}

@Composable
fun NextyLibraryApp() {
    var books by remember { mutableStateOf<List<Book>>(emptyList()) }
    var categories by remember { mutableStateOf<List<BookCategory>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<Book?>(null) }
    var tab by remember { mutableStateOf("Beranda") }
    var reloadKey by remember { mutableIntStateOf(0) }

    suspend fun loadCatalog() {
        loading = true
        error = null
        try {
            val result = withContext(Dispatchers.IO) {
                Pair(fetchCategories(), fetchBooks())
            }
            categories = result.first
            books = result.second
        } catch (e: Exception) {
            error = e.localizedMessage ?: "Tidak dapat memuat katalog."
        } finally {
            loading = false
        }
    }

    LaunchedEffect(reloadKey) { loadCatalog() }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Green,
            onPrimary = Color.White,
            background = Paper,
            surface = Color.White,
            onSurface = Ink
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Paper) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("NEXTY STUDIO", color = Green, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
                        Text("NextyLibrary", color = Ink, fontSize = 27.sp, fontWeight = FontWeight.Black)
                    }
                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(15.dp)).background(Brush.linearGradient(listOf(Green, Color(0xFF75B99A)))), contentAlignment = Alignment.Center) { Text("N", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black) }
                }

                if (selected != null) {
                    BookDetail(book = selected!!, categoryName = categories.firstOrNull { it.id == selected!!.categoryId }?.name, onBack = { selected = null })
                } else {
                    when (tab) {
                        "Beranda" -> {
                            Column(modifier = Modifier.weight(1f).padding(horizontal = 18.dp)) {
                                Spacer(Modifier.height(18.dp))
                                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp)).background(Brush.linearGradient(listOf(Color(0xFF123D32), Color(0xFF218263), Color(0xFF70B99A)))).padding(20.dp)) {
                                    Column {
                                        Text("SELAMAT DATANG DI NEXTY", color = Color(0xFFC6E9D8), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                                        Text("Temukan dunia\ndalam setiap halaman.", color = Color.White, fontSize = 24.sp, lineHeight = 29.sp, fontWeight = FontWeight.ExtraBold)
                                        Spacer(Modifier.height(8.dp))
                                        Text("Bacaan pilihan untuk ide dan inspirasi baru.", color = Color.White.copy(alpha = .88f), fontSize = 12.sp)
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                Text("Jelajahi koleksi pilihan", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(14.dp))
                                OutlinedTextField(
                                    value = search,
                                    onValueChange = { search = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    placeholder = { Text("Cari judul atau penulis...") },
                                    leadingIcon = { Text("⌕", fontSize = 23.sp, color = Green) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, unfocusedBorderColor = Color(0xFFE0E9E2), focusedContainerColor = Color.White, unfocusedContainerColor = Color.White),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = selectedCategory == null,
                                        onClick = { selectedCategory = null },
                                        label = { Text("Semua") }
                                    )
                                    categories.forEach { category ->
                                        FilterChip(
                                            selected = selectedCategory == category.id,
                                            onClick = { selectedCategory = category.id },
                                            label = { Text(category.name) }
                                        )
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Katalog buku", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = Ink, modifier = Modifier.weight(1f))
                                    TextButton(onClick = { reloadKey++ }, enabled = !loading) { Text("Muat ulang") }
                                }
                                when {
                                    loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator()
                                    }
                                    error != null -> Column(
                                        Modifier.fillMaxWidth().weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text("Katalog belum dapat dimuat", fontWeight = FontWeight.Bold)
                                        Text(error!!, color = Color.Gray, fontSize = 12.sp)
                                        Spacer(Modifier.height(10.dp))
                                        Button(onClick = { reloadKey++ }) { Text("Coba lagi") }
                                    }
                                    else -> {
                                        val filtered = books.filter { book ->
                                            val matchesSearch = book.title.contains(search, true) || book.author.contains(search, true)
                                            val matchesCategory = selectedCategory == null || book.categoryId == selectedCategory
                                            matchesSearch && matchesCategory
                                        }
                                        if (filtered.isEmpty()) {
                                            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                                Text(
                                                    if (books.isEmpty()) "Belum ada buku yang diterbitkan." else "Buku tidak ditemukan.",
                                                    color = Color.Gray
                                                )
                                            }
                                        } else {
                                            LazyColumn(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                                contentPadding = PaddingValues(bottom = 16.dp)
                                            ) {
                                                items(filtered, key = { it.id }) { book ->
                                                    BookCard(
                                                        book = book,
                                                        categoryName = categories.firstOrNull { it.id == book.categoryId }?.name,
                                                        onClick = { selected = book }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        "Koleksi" -> PlaceholderPage("Koleksi", "Buku yang sudah kamu buka akan tampil di sini.")
                        else -> AccountScreen()
                    }
                    NavigationBar(containerColor = Color.White) {
                        listOf("Beranda", "Koleksi", "Akun").forEach { item ->
                            NavigationBarItem(
                                selected = tab == item,
                                onClick = { tab = item },
                                icon = { Text(when (item) { "Beranda" -> "⌂"; "Koleksi" -> "▤"; else -> "●" }, fontSize = 18.sp) },
                                label = { Text(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookCard(book: Book, categoryName: String?, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!book.coverPath.isNullOrBlank()) {
                AsyncImage(
                    model = publicCoverUrl(book.coverPath),
                    contentDescription = "Sampul ${book.title}",
                    modifier = Modifier.size(76.dp, 100.dp).clip(RoundedCornerShape(12.dp)).background(Mint)
                )
            } else {
                Box(
                    Modifier.size(76.dp, 100.dp).clip(RoundedCornerShape(12.dp)).background(Mint),
                    contentAlignment = Alignment.Center
                ) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("N", color = Green, fontSize = 32.sp, fontWeight = FontWeight.Black); Text("NEXTY", color = Green, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) } }
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(book.title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Ink, maxLines = 2)
                Text(book.author.ifBlank { "Penulis belum dicantumkan" }, color = Green, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                if (!categoryName.isNullOrBlank()) {
                    Text(categoryName, color = Color.Gray, fontSize = 11.sp)
                }
                Spacer(Modifier.height(5.dp))
                Text(book.description.ifBlank { "Buka detail untuk melihat informasi buku." }, color = Color.Gray, fontSize = 12.sp, maxLines = 2)
            }
        }
    }
}

@Composable
private fun BookDetail(book: Book, categoryName: String?, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Paper).padding(20.dp)) {
        Text("‹  Kembali", color = Green, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onBack).padding(vertical = 8.dp))
        Spacer(Modifier.height(24.dp))
        if (!book.coverPath.isNullOrBlank()) {
            AsyncImage(
                model = publicCoverUrl(book.coverPath),
                contentDescription = "Sampul ${book.title}",
                modifier = Modifier.fillMaxWidth().height(220.dp).background(Color(0xFFE6EFE8), RoundedCornerShape(18.dp))
            )
        } else {
            Box(Modifier.fillMaxWidth().height(180.dp).background(Color(0xFFE6EFE8), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                Text("NEXTY LIBRARY", color = Green, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(book.title, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Text("Oleh ${book.author}", color = Green)
        if (!categoryName.isNullOrBlank()) Text(categoryName, color = Color.Gray, fontSize = 12.sp)
        if (book.pageCount > 0) Text("${book.pageCount} halaman", color = Color.Gray, fontSize = 12.sp)
        Spacer(Modifier.height(14.dp))
        Text(book.description.ifBlank { "Deskripsi buku belum tersedia." }, color = Color.DarkGray, lineHeight = 22.sp)
        Spacer(Modifier.height(24.dp))
        Text("Pembaca buku dan proses pembukaan akses akan ditambahkan setelah autentikasi serta verifikasi iklan sisi server selesai.", color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
private fun PlaceholderPage(title: String, subtitle: String) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = Color.Gray)
    }
}

private fun fetchCategories(): List<BookCategory> {
    val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') +
        "/rest/v1/categories?select=id,name&order=sort_order.asc"
    val connection = URL(endpoint).openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
        connection.setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_PUBLISHABLE_KEY}")
        connection.setRequestProperty("Accept", "application/json")
        val code = connection.responseCode
        if (code !in 200..299) throw IllegalStateException("Supabase kategori merespons HTTP $code")
        val array = JSONArray(connection.inputStream.bufferedReader().use { it.readText() })
        return (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            BookCategory(item.optString("id"), item.optString("name"))
        }
    } finally {
        connection.disconnect()
    }
}

private fun fetchBooks(): List<Book> {
    val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') +
        "/rest/v1/books?select=id,title,author,description,category_id,page_count,cover_path&is_published=eq.true&order=published_at.desc"
    val connection = URL(endpoint).openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
        connection.setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_PUBLISHABLE_KEY}")
        connection.setRequestProperty("Accept", "application/json")
        val code = connection.responseCode
        if (code !in 200..299) throw IllegalStateException("Supabase buku merespons HTTP $code")
        val array = JSONArray(connection.inputStream.bufferedReader().use { it.readText() })
        return (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            Book(
                id = item.optString("id"),
                title = item.optString("title", "Tanpa judul"),
                author = item.optString("author"),
                description = item.optString("description"),
                categoryId = item.optString("category_id").takeIf { it.isNotBlank() && it != "null" },
                pageCount = item.optInt("page_count", 0),
                coverPath = item.optString("cover_path").takeIf { it.isNotBlank() && it != "null" }
            )
        }
    } finally {
        connection.disconnect()
    }
}


private fun publicCoverUrl(path: String): String {
    val encodedPath = path.trimStart('/').split('/').joinToString("/") { Uri.encode(it) }
    return BuildConfig.SUPABASE_URL.trimEnd('/') +
        "/storage/v1/object/public/book-covers/" + encodedPath
}
