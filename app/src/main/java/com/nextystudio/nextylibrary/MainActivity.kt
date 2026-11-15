package com.nextystudio.nextylibrary

import android.net.Uri
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.PI
import kotlin.math.sin

private val Ink = Color(0xFF14251F)
private val Green = Color(0xFF16765A)
private val DeepGreen = Color(0xFF103C30)
private val Paper = Color(0xFFF4F7F4)
private val Mint = Color(0xFFDDF1E6)
private val Muted = Color(0xFF78877F)

data class Book(val id: String, val title: String, val author: String, val description: String, val categoryId: String?, val pageCount: Int, val coverPath: String?, val contentPath: String?)
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
    var reading by remember { mutableStateOf<Book?>(null) }
    var tab by remember { mutableStateOf("Beranda") }
    var reloadKey by remember { mutableIntStateOf(0) }
    val musicPlayer = remember { AmbientMusicPlayer() }
    var musicPlaying by remember { mutableStateOf(false) }
    var musicVolume by remember { mutableFloatStateOf(1f) }
    DisposableEffect(musicPlayer) {
        onDispose { musicPlayer.release() }
    }

    suspend fun loadCatalog() {
        loading = true
        error = null
        try {
            val result = withContext(Dispatchers.IO) { Pair(fetchCategories(), fetchBooks()) }
            categories = result.first
            books = result.second
        } catch (e: Exception) { error = e.localizedMessage ?: "Tidak dapat memuat katalog." }
        finally { loading = false }
    }
    LaunchedEffect(reloadKey) { loadCatalog() }

    MaterialTheme(colorScheme = lightColorScheme(primary = Green, onPrimary = Color.White, background = Paper, surface = Color.White, onSurface = Ink)) {
        Surface(Modifier.fillMaxSize(), color = Paper) {
            Column(Modifier.fillMaxSize()) {
                if (reading != null) {
                    ReaderScreen(reading!!, onBack = { reading = null })
                } else if (selected != null) {
                    BookDetail(selected!!, categories.firstOrNull { it.id == selected!!.categoryId }?.name, onBack = { selected = null }, onRead = { reading = selected })
                } else {
                    when (tab) {
                        "Beranda" -> Box(Modifier.weight(1f)) { HomeScreen(books, categories, loading, error, search, { search = it }, selectedCategory, { selectedCategory = it }, { reloadKey++ }, { selected = it }, musicPlayer, musicPlaying, { if (musicPlaying) musicPlayer.pause() else musicPlayer.play(); musicPlaying = !musicPlaying }, musicVolume, { musicVolume = it; musicPlayer.setVolume(it) }) }
                        "Koleksi" -> Box(Modifier.weight(1f)) { CollectionEmptyScreen() }
                        else -> Box(Modifier.weight(1f)) { AccountScreen() }
                    }
                    NavigationBar(containerColor = Color.White, tonalElevation = 10.dp, modifier = Modifier.shadow(12.dp)) {
                        listOf("Beranda", "Koleksi", "Akun").forEach { item ->
                            NavigationBarItem(selected = tab == item, onClick = { tab = item },
                                icon = { Text(when (item) { "Beranda" -> "⌂"; "Koleksi" -> "▣"; else -> "◉" }, fontSize = 21.sp, fontWeight = FontWeight.Bold) },
                                label = { Text(item, fontWeight = if (tab == item) FontWeight.Bold else FontWeight.Medium) },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = Green, selectedTextColor = Green, indicatorColor = Mint))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(books: List<Book>, categories: List<BookCategory>, loading: Boolean, error: String?,
    search: String, onSearch: (String) -> Unit, selectedCategory: String?, onCategory: (String?) -> Unit,
    onReload: () -> Unit, onBook: (Book) -> Unit, musicPlayer: AmbientMusicPlayer,
    musicPlaying: Boolean, onMusicToggle: () -> Unit, musicVolume: Float, onMusicVolume: (Float) -> Unit) {
    val filtered = books.filter { b ->
        (b.title.contains(search, true) || b.author.contains(search, true)) &&
        (selectedCategory == null || b.categoryId == selectedCategory)
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 22.dp)) {
        item { TopHeader() }
        item { HeroBanner(books.size) }
        item { MusicPlayerCard(musicPlaying, onMusicToggle, musicVolume, onMusicVolume) }
        item {
            Column(Modifier.padding(top = 22.dp)) {
                Text("Temukan bacaanmu", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 20.dp))
                Text("Jelajahi cerita, ilmu, dan inspirasi.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(start = 20.dp, top = 3.dp, bottom = 13.dp))
                OutlinedTextField(value = search, onValueChange = onSearch, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    singleLine = true, placeholder = { Text("Cari buku atau penulis...") }, leadingIcon = { Text("⌕", color = Green, fontSize = 25.sp) },
                    shape = RoundedCornerShape(17.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, unfocusedBorderColor = Color(0xFFE1EAE3), focusedContainerColor = Color.White, unfocusedContainerColor = Color.White))
            }
        }
        item {
            Column(Modifier.padding(top = 21.dp)) {
                Text("Kategori", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 20.dp))
                Row(Modifier.fillMaxWidth().padding(top = 12.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CategoryTile("✦", "Semua", selectedCategory == null) { onCategory(null) }
                    categories.forEach { c -> CategoryTile(categoryIcon(c.name), c.name, selectedCategory == c.id) { onCategory(c.id) } }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 14.dp, top = 24.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Pilihan bacaan", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Koleksi terbaru dari NextyLibrary", color = Muted, fontSize = 11.sp)
                }
                TextButton(onClick = onReload, enabled = !loading) { Text("↻ Segarkan", color = Green) }
            }
        }
        when {
            loading -> item { Box(Modifier.fillMaxWidth().height(190.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Green) } }
            error != null -> item { EmptyVisual("Koneksi belum tersedia", error, "Coba lagi", onReload) }
            filtered.isEmpty() -> item {
                EmptyVisual(if (books.isEmpty()) "Rak bukumu masih kosong" else "Buku tidak ditemukan",
                    if (books.isEmpty()) "Buku yang diterbitkan akan muncul di sini lengkap dengan sampul dan detailnya." else "Coba kata kunci atau kategori yang berbeda.",
                    if (books.isEmpty()) null else "Hapus filter", { onCategory(null); onSearch("") })
            }
            else -> items(filtered.chunked(2)) { rowBooks ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowBooks.forEach { b -> BookGridCard(b, categories.firstOrNull { it.id == b.categoryId }?.name, Modifier.weight(1f)) { onBook(b) } }
                    if (rowBooks.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TopHeader() {
    Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(43.dp).clip(RoundedCornerShape(15.dp)).background(Brush.linearGradient(listOf(Green, Color(0xFF72B99A)))), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.width(12.dp).height(21.dp).clip(RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp))
                        .background(Color.White).padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(3) { Box(Modifier.fillMaxWidth().height(1.5.dp).background(Green.copy(alpha = 0.65f))) }
                    }
                }
                Spacer(Modifier.width(1.dp))
                Box(
                    Modifier.width(12.dp).height(21.dp).clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                        .background(Color(0xFFE8F8EF)).padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(3) { Box(Modifier.fillMaxWidth().height(1.5.dp).background(Green.copy(alpha = 0.65f))) }
                    }
                }
            }
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text("NEXTY STUDIO", color = Green, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.5.sp)
            Text("NextyLibrary", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
        Box(Modifier.size(39.dp).clip(CircleShape).background(Paper).border(1.dp, Color(0xFFE6EEE8), CircleShape), contentAlignment = Alignment.Center) {
            Text("♧", color = Green, fontSize = 20.sp)
        }
    }
}

@Composable
private fun HeroBanner(bookCount: Int) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 14.dp).height(184.dp).clip(RoundedCornerShape(25.dp))
        .background(Brush.linearGradient(listOf(Color(0xFF10392F), Color(0xFF176D51), Color(0xFF4D9C78))))) {
        Box(Modifier.size(190.dp).offset(x = 215.dp, y = (-65).dp).clip(CircleShape).background(Color.White.copy(alpha = .07f)))
        Box(Modifier.size(145.dp).align(Alignment.BottomEnd).offset(x = 38.dp, y = 48.dp).clip(CircleShape).background(Color.White.copy(alpha = .08f)))
        Column(Modifier.align(Alignment.CenterStart).padding(start = 20.dp, end = 110.dp)) {
            Text("RUANG BACA DIGITAL", color = Color(0xFFB9E8D0), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            Text("Satu buku,\nbanyak dunia.", color = Color.White, fontSize = 25.sp, lineHeight = 29.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(7.dp))
            Text("$bookCount bacaan tersedia untuk dijelajahi", color = Color.White.copy(alpha = .82f), fontSize = 10.sp)
        }
        BookStackIllustration(Modifier.align(Alignment.CenterEnd).padding(end = 14.dp))
        Text("✦", color = Color(0xFFFFD98D), fontSize = 22.sp, modifier = Modifier.align(Alignment.TopEnd).padding(top = 19.dp, end = 22.dp))
    }
}

@Composable
private fun MusicPlayerCard(
    playing: Boolean,
    onToggle: () -> Unit,
    volume: Float,
    onVolumeChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(15.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF263D78), Color(0xFF8B70C9)))),
                contentAlignment = Alignment.Center
            ) { Text("♫", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Piano Chill", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                Text("Musik latar tetap berjalan saat kamu membaca", color = Muted, fontSize = 10.sp, maxLines = 2)
                Text("NEXTY PIANO CHILL • ORIGINAL", color = Green, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = onToggle,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Green, contentColor = Color.White)
            ) {
                Text(if (playing) "Ⅱ" else "▶", fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("♪", color = Muted, fontSize = 14.sp)
            Slider(
                value = volume,
                onValueChange = onVolumeChange,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(thumbColor = Green, activeTrackColor = Green)
            )
            Text((volume * 100).toInt().toString() + "%", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private class AmbientMusicPlayer {
    private var track: AudioTrack? = null
    private var volume: Float = 1f

    fun setVolume(value: Float) {
        volume = value.coerceIn(0f, 1f)
        track?.setVolume(volume)
    }

    fun play() {
        val current = track
        if (current != null) {
            if (current.playState != AudioTrack.PLAYSTATE_PLAYING) current.play()
            return
        }
        val sampleRate = 22050
        val seconds = 8
        val frames = sampleRate * seconds
        val samples = ShortArray(frames * 2)
        val chordSteps = intArrayOf(0, 5, 7, 3)
        val melodySteps = intArrayOf(12, 7, 10, 7, 15, 12, 10, 7, 12, 17, 15, 12, 10, 7, 5, 7)
        for (frame in 0 until frames) {
            val time = frame.toDouble() / sampleRate
            val beatPosition = time * 2.0
            val chordIndex = (time / 2.0).toInt() % chordSteps.size
            val root = 220.0 * Math.pow(2.0, chordSteps[chordIndex] / 12.0)
            val fadeIn = (time / 0.18).coerceIn(0.0, 1.0)
            val fadeOut = ((seconds - time) / 0.22).coerceIn(0.0, 1.0)
            val envelope = minOf(fadeIn, fadeOut)
            val pulse = 0.88 + 0.12 * sin(2.0 * PI * beatPosition)
            val pad = sin(2.0 * PI * root * 0.5 * time) * 0.18 +
                sin(2.0 * PI * root * time) * 0.13 +
                sin(2.0 * PI * root * 1.5 * time) * 0.07
            val bass = sin(2.0 * PI * root * 0.5 * time) * 0.22
            val beatInBar = (beatPosition % 4.0)
            val noteIndex = (beatPosition.toInt() % melodySteps.size)
            val noteHz = 440.0 * Math.pow(2.0, (melodySteps[noteIndex] - 12) / 12.0)
            val notePhase = beatInBar % 1.0
            val pluckEnvelope = Math.exp(-notePhase * 6.5)
            val pluck = (sin(2.0 * PI * noteHz * time) +
                0.28 * sin(2.0 * PI * noteHz * 2.0 * time)) * pluckEnvelope * 0.34
            val sparkleHz = noteHz * 2.0
            val sparkle = sin(2.0 * PI * sparkleHz * time) * pluckEnvelope * 0.045
            val mix = (pad + bass + pluck + sparkle) * envelope * pulse * 0.72
            val value = (mix * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            samples[frame * 2] = value
            samples[frame * 2 + 1] = value
        }
        val audioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .build()
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val created = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(audioFormat)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(samples.size * 2)
            .build()
        val written = created.write(samples, 0, samples.size)
        if (written > 0) {
            created.setLoopPoints(0, frames, -1)
            created.setVolume(volume)
            track = created
            created.play()
        } else {
            created.release()
        }
    }

    fun pause() {
        track?.let { if (it.playState == AudioTrack.PLAYSTATE_PLAYING) it.pause() }
    }

    fun release() {
        track?.let {
            try { it.pause(); it.flush(); it.release() } catch (_: Exception) { }
        }
        track = null
    }
}

@Composable
private fun BookStackIllustration(modifier: Modifier = Modifier) {
    Box(modifier.size(112.dp, 130.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.offset(x = 7.dp, y = 12.dp).size(62.dp, 91.dp).shadow(8.dp, RoundedCornerShape(7.dp)).clip(RoundedCornerShape(7.dp)).background(Color(0xFFE5B66F))) {
            Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Text("✦", color = Color(0xFF80552D), fontSize = 18.sp)
                Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF80552D).copy(alpha = .5f)))
            }
        }
        Box(Modifier.offset(x = (-16).dp, y = 19.dp).size(59.dp, 84.dp).shadow(8.dp, RoundedCornerShape(7.dp)).clip(RoundedCornerShape(7.dp)).background(Color(0xFFDBE9D9))) {
            Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Text("N", color = Green, fontSize = 21.sp, fontWeight = FontWeight.Black)
                Box(Modifier.fillMaxWidth().height(2.dp).background(Green.copy(alpha = .35f)))
            }
        }
        Box(Modifier.offset(x = (-2).dp, y = (-20).dp).size(58.dp, 82.dp).shadow(8.dp, RoundedCornerShape(7.dp)).clip(RoundedCornerShape(7.dp)).background(Color(0xFFFAF4E8))) {
            Column(Modifier.fillMaxSize().padding(7.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Text("READ", color = DeepGreen, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text("✧", color = Color(0xFFC18A4D), fontSize = 23.sp)
                Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFFC18A4D).copy(alpha = .5f)))
            }
        }
    }
}

@Composable
private fun CategoryTile(icon: String, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(Modifier.width(82.dp).clip(RoundedCornerShape(17.dp)).background(if (selected) Mint else Color.White)
        .border(1.dp, if (selected) Color(0xFF9ACDB1) else Color(0xFFE9EFEA), RoundedCornerShape(17.dp))
        .clickable(onClick = onClick).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(37.dp).clip(RoundedCornerShape(13.dp)).background(if (selected) Color.White.copy(alpha = .8f) else Paper), contentAlignment = Alignment.Center) {
            Text(icon, color = Green, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(7.dp))
        Text(label, color = if (selected) Green else Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun BookGridCard(book: Book, categoryName: String?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.fillMaxWidth().padding(9.dp)) {
            Box(Modifier.fillMaxWidth().height(158.dp).clip(RoundedCornerShape(13.dp)).background(Mint), contentAlignment = Alignment.Center) {
                if (!book.coverPath.isNullOrBlank()) {
                    AsyncImage(model = publicCoverUrl(book.coverPath), contentDescription = "Sampul " + book.title, modifier = Modifier.fillMaxSize())
                } else {
                    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF174D3B), Color(0xFF75AA87)))), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(100.dp).clip(CircleShape).border(1.dp, Color.White.copy(alpha = .25f), CircleShape))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("N", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black)
                            Text("NEXTY BOOKS", color = Color.White.copy(alpha = .8f), fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        }
                    }
                }
                Box(Modifier.align(Alignment.TopEnd).padding(7.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = .92f)).padding(horizontal = 7.dp, vertical = 4.dp)) {
                    Text(categoryName ?: "BUKU", color = Green, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(book.title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
            Text(book.author.ifBlank { "Penulis Nexty" }, color = Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (book.pageCount > 0) book.pageCount.toString() + " hlm" else "Lihat detail", color = Green, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("›", color = Green, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyVisual(title: String, subtitle: String, action: String?, onAction: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(116.dp).clip(RoundedCornerShape(34.dp)).background(Brush.linearGradient(listOf(Mint, Color(0xFFF0F6F1)))), contentAlignment = Alignment.Center) {
            Text("▤", color = Green, fontSize = 61.sp, fontWeight = FontWeight.Light)
            Text("✦", color = Color(0xFFC18A4D), fontSize = 23.sp, modifier = Modifier.offset(x = 34.dp, y = (-34).dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
        if (action != null) {
            Spacer(Modifier.height(13.dp))
            Button(onClick = onAction, colors = ButtonDefaults.buttonColors(containerColor = Green), shape = RoundedCornerShape(13.dp)) { Text(action) }
        }
    }
}

@Composable
private fun CollectionEmptyScreen() {
    Column(Modifier.fillMaxSize().background(Paper).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(122.dp).clip(RoundedCornerShape(35.dp)).background(Mint), contentAlignment = Alignment.Center) { Text("▣", color = Green, fontSize = 58.sp) }
        Spacer(Modifier.height(18.dp))
        Text("Rak buku pribadimu", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(7.dp))
        Text("Buku yang sudah berhasil kamu buka akan tersimpan di sini.", color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
    }
}

@Composable
private fun BookDetail(book: Book, categoryName: String?, onBack: () -> Unit, onRead: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Paper)) {
        Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 18.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", color = Green, fontSize = 30.sp, modifier = Modifier.clickable(onClick = onBack))
            Spacer(Modifier.width(12.dp))
            Text("Detail buku", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
        LazyColumn(contentPadding = PaddingValues(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            item {
                Box(Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(24.dp)).background(Mint), contentAlignment = Alignment.Center) {
                    if (!book.coverPath.isNullOrBlank()) {
                        AsyncImage(model = publicCoverUrl(book.coverPath), contentDescription = "Sampul " + book.title, modifier = Modifier.fillMaxSize())
                    } else {
                        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(DeepGreen, Color(0xFF68A17D)))), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("N", color = Color.White, fontSize = 86.sp, fontWeight = FontWeight.Black)
                                Text("NEXTY LIBRARY", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
                Text(book.title, color = Ink, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(6.dp))
                Text("Oleh " + book.author, color = Green, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(13.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!categoryName.isNullOrBlank()) InfoPill(categoryName)
                    if (book.pageCount > 0) InfoPill(book.pageCount.toString() + " halaman")
                }
                Spacer(Modifier.height(22.dp))
                Text("Tentang buku", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text(book.description.ifBlank { "Deskripsi buku belum tersedia." }, color = Muted, fontSize = 13.sp, lineHeight = 21.sp, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(24.dp))
                Button(onClick = onRead, enabled = !book.contentPath.isNullOrBlank(),
                    modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                    Text("Baca sekarang", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                Text(if (book.contentPath.isNullOrBlank()) "Teks buku belum tersedia." else "Edisi teks domain publik • dapat dibaca gratis",
                    color = Muted, fontSize = 11.sp)
            }
        }
    }
}


@Composable
private fun ReaderScreen(book: Book, onBack: () -> Unit) {
    var body by remember(book.id) { mutableStateOf<String?>(null) }
    var error by remember(book.id) { mutableStateOf<String?>(null) }
    var loading by remember(book.id) { mutableStateOf(true) }
    LaunchedEffect(book.id, book.contentPath) {
        loading = true
        error = null
        try {
            val source = book.contentPath ?: throw IllegalStateException("Sumber teks buku belum tersedia.")
            body = withContext(Dispatchers.IO) { fetchPublicBookText(source) }
        } catch (e: Exception) {
            error = e.localizedMessage ?: "Buku gagal dimuat. Periksa koneksi internet."
        } finally { loading = false }
    }
    Column(Modifier.fillMaxSize().background(Paper)) {
        Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", color = Green, fontSize = 30.sp, modifier = Modifier.clickable(onClick = onBack))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(book.title, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(book.author, color = Muted, fontSize = 11.sp)
            }
            Text("DOMAIN PUBLIK", color = Green, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Green)
                    Spacer(Modifier.height(12.dp))
                    Text("Memuat buku…", color = Muted)
                }
            }
            error != null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Buku belum dapat dibuka", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(error ?: "", color = Muted)
                }
            }
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 26.dp, bottom = 50.dp)) {
                item {
                    Text(book.title, color = Ink, fontSize = 25.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(8.dp))
                    Text("Oleh " + book.author, color = Green, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider(color = Mint)
                    Spacer(Modifier.height(20.dp))
                }
                items((body ?: "").split(Regex("\\n\\s*\\n")).filter { it.isNotBlank() }) { paragraph ->
                    Text(paragraph.trim(), color = Ink, fontSize = 16.sp, lineHeight = 28.sp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp))
                }
                item { Text("— Akhir teks —", color = Muted, fontSize = 12.sp) }
            }
        }
    }
}

private fun fetchPublicBookText(source: String): String {
    val connection = URL(source).openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 20000
        connection.readTimeout = 30000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "NextyLibrary/1.0")
        val code = connection.responseCode
        if (code !in 200..299) throw IllegalStateException("Sumber buku merespons HTTP " + code)
        val raw = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val startRegex = Regex("(?im)^\\*\\*\\*\\s*START OF (?:THE|THIS) PROJECT GUTENBERG EBOOK[^\\n]*\\*\\*\\*\\s*")
        val endRegex = Regex("(?im)^\\*\\*\\*\\s*END OF (?:THE|THIS) PROJECT GUTENBERG EBOOK[^\\n]*\\*\\*\\*")
        val start = startRegex.find(raw)
        val end = endRegex.find(raw, start?.range?.last?.plus(1) ?: 0)
        val clean = if (start != null && end != null) raw.substring(start.range.last + 1, end.range.first) else raw
        return clean.replace("\r\n", "\n").replace(Regex("\n{3,}"), "\n\n").trim()
    } finally { connection.disconnect() }
}

@Composable
private fun InfoPill(text: String) {
    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(Mint).padding(horizontal = 11.dp, vertical = 7.dp)) {
        Text(text, color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

private fun categoryIcon(name: String): String = when (name.lowercase()) {
    "novel" -> "♡"
    "pendidikan" -> "✎"
    "sejarah" -> "⌛"
    "sains" -> "⚛"
    "pengembangan diri" -> "✦"
    else -> "▤"
}

private fun fetchCategories(): List<BookCategory> {
    val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1/categories?select=id,name&order=sort_order.asc"
    val connection = URL(endpoint).openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
        connection.setRequestProperty("Authorization", "Bearer " + BuildConfig.SUPABASE_PUBLISHABLE_KEY)
        connection.setRequestProperty("Accept", "application/json")
        val code = connection.responseCode
        if (code !in 200..299) throw IllegalStateException("Supabase kategori merespons HTTP " + code)
        val array = JSONArray(connection.inputStream.bufferedReader().use { it.readText() })
        return (0 until array.length()).map { i ->
            val item = array.getJSONObject(i)
            BookCategory(item.optString("id"), item.optString("name"))
        }
    } finally { connection.disconnect() }
}

private fun fetchBooks(): List<Book> {
    val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1/books?select=id,title,author,description,category_id,page_count,cover_path,content_path&is_published=eq.true&order=published_at.desc"
    val connection = URL(endpoint).openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
        connection.setRequestProperty("Authorization", "Bearer " + BuildConfig.SUPABASE_PUBLISHABLE_KEY)
        connection.setRequestProperty("Accept", "application/json")
        val code = connection.responseCode
        if (code !in 200..299) throw IllegalStateException("Supabase buku merespons HTTP " + code)
        val array = JSONArray(connection.inputStream.bufferedReader().use { it.readText() })
        return (0 until array.length()).map { i ->
            val item = array.getJSONObject(i)
            Book(item.optString("id"), item.optString("title", "Tanpa judul"), item.optString("author"),
                item.optString("description"), item.optString("category_id").takeIf { it.isNotBlank() && it != "null" },
                item.optInt("page_count", 0), item.optString("cover_path").takeIf { it.isNotBlank() && it != "null" },
                item.optString("content_path").takeIf { it.isNotBlank() && it != "null" })
        }
    } finally { connection.disconnect() }
}

private fun publicCoverUrl(path: String): String {
    val encodedPath = path.trimStart('/').split('/').joinToString("/") { Uri.encode(it) }
    return BuildConfig.SUPABASE_URL.trimEnd('/') + "/storage/v1/object/public/book-covers/" + encodedPath
}
