package com.nextystudio.nextylibrary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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

private val Ink = Color(0xFF17211D)
private val Green = Color(0xFF236B4A)
private val Paper = Color(0xFFF6F7F3)

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val description: String,
    val categoryId: String?
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NextyLibraryApp() }
    }
}

@Composable
fun NextyLibraryApp() {
    var books by remember { mutableStateOf<List<Book>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Book?>(null) }
    var tab by remember { mutableStateOf("Beranda") }
    var reloadKey by remember { mutableIntStateOf(0) }

    suspend fun loadBooks() {
        loading = true
        error = null
        try {
            books = withContext(Dispatchers.IO) { fetchBooks() }
        } catch (e: Exception) {
            error = e.localizedMessage ?: "Tidak dapat memuat katalog."
        } finally {
            loading = false
        }
    }

    LaunchedEffect(reloadKey) { loadBooks() }

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
                        Text("NEXTY", color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                        Text("Library", color = Ink, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Text("Baca. Temukan. Bertumbuh.", color = Green, fontSize = 11.sp)
                }

                if (selected != null) {
                    BookDetail(book = selected!!, onBack = { selected = null })
                } else {
                    when (tab) {
                        "Beranda" -> {
                            Column(modifier = Modifier.weight(1f).padding(horizontal = 18.dp)) {
                                Spacer(Modifier.height(18.dp))
                                Text("Temukan bacaanmu", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Ink)
                                Text("Jelajahi koleksi pilihan dari NextyLibrary.", color = Color.Gray, fontSize = 13.sp)
                                Spacer(Modifier.height(14.dp))
                                OutlinedTextField(
                                    value = search,
                                    onValueChange = { search = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    placeholder = { Text("Cari judul atau penulis...") },
                                    shape = RoundedCornerShape(14.dp)
                                )
                                Spacer(Modifier.height(14.dp))
                                Text("Katalog buku", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                                Spacer(Modifier.height(8.dp))
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
                                        val filtered = books.filter {
                                            it.title.contains(search, true) || it.author.contains(search, true)
                                        }
                                        if (filtered.isEmpty()) {
                                            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                                Text(if (books.isEmpty()) "Belum ada buku yang diterbitkan." else "Buku tidak ditemukan.", color = Color.Gray)
                                            }
                                        } else {
                                            LazyColumn(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                                contentPadding = PaddingValues(bottom = 16.dp)
                                            ) {
                                                items(filtered, key = { it.id }) { book ->
                                                    BookCard(book = book, onClick = { selected = book })
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        "Koleksi" -> PlaceholderPage("Koleksi", "Buku yang sudah kamu buka akan tampil di sini.")
                        else -> PlaceholderPage("Akun", "Masuk dan sinkronkan progres membaca di perangkatmu.")
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
private fun BookCard(book: Book, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(62.dp, 82.dp).background(Color(0xFFE6EFE8), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center
            ) { Text("📖", fontSize = 25.sp) }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(book.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
                Text(book.author.ifBlank { "Penulis belum dicantumkan" }, color = Green, fontSize = 12.sp)
                Spacer(Modifier.height(5.dp))
                Text(book.description.ifBlank { "Buka detail untuk melihat informasi buku." }, color = Color.Gray, fontSize = 12.sp, maxLines = 2)
            }
        }
    }
}

@Composable
private fun BookDetail(book: Book, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("‹  Kembali", color = Green, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onBack).padding(vertical = 8.dp))
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth().height(180.dp).background(Color(0xFFE6EFE8), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
            Text("📚", fontSize = 64.sp)
        }
        Spacer(Modifier.height(20.dp))
        Text(book.title, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Text("Oleh ${book.author}", color = Green)
        Spacer(Modifier.height(14.dp))
        Text(book.description.ifBlank { "Deskripsi buku belum tersedia." }, color = Color.DarkGray, lineHeight = 22.sp)
        Spacer(Modifier.height(24.dp))
        Text("Akses membaca dan pembukaan buku akan diaktifkan setelah autentikasi serta verifikasi iklan selesai dibuat.", color = Color.Gray, fontSize = 12.sp)
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

private fun fetchBooks(): List<Book> {
    val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') +
        "/rest/v1/books?select=id,title,author,description,category_id&is_published=eq.true&order=published_at.desc"
    val connection = URL(endpoint).openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
        connection.setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_PUBLISHABLE_KEY}")
        connection.setRequestProperty("Accept", "application/json")
        val code = connection.responseCode
        if (code !in 200..299) throw IllegalStateException("Supabase merespons HTTP $code")
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        val array = JSONArray(body)
        return (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            Book(
                id = item.optString("id"),
                title = item.optString("title", "Tanpa judul"),
                author = item.optString("author"),
                description = item.optString("description"),
                categoryId = item.optString("category_id").takeIf { it.isNotBlank() && it != "null" }
            )
        }
    } finally {
        connection.disconnect()
    }
}
