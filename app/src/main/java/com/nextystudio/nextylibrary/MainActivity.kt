package com.nextystudio.nextylibrary

import android.net.Uri
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
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
import androidx.compose.animation.core.*
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
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
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent { NextyLibraryApp() }
    }
}

@Composable
private fun IntroScreen() {
    val infinite = rememberInfiniteTransition(label = "intro")
    val floatY by infinite.animateFloat(0f, -12f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float")
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF0D3329), Color(0xFF176B50), Color(0xFF73A98A)))), contentAlignment = Alignment.Center) {
        Box(Modifier.size(300.dp).offset(x = 150.dp, y = (-260).dp).clip(CircleShape).background(Color.White.copy(alpha = .055f)))
        Box(Modifier.size(240.dp).offset(x = (-150).dp, y = 260.dp).clip(CircleShape).background(Color.White.copy(alpha = .06f)))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = floatY.dp).padding(horizontal = 30.dp)) {
            Box(Modifier.size(112.dp).clip(RoundedCornerShape(32.dp)).background(Color.White.copy(alpha = .16f)).border(1.dp, Color.White.copy(alpha = .25f), RoundedCornerShape(32.dp)), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(31.dp).height(45.dp).clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp)).background(Color.White).padding(5.dp), contentAlignment = Alignment.Center) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) { repeat(3) { Box(Modifier.fillMaxWidth().height(2.dp).background(Green)) } }
                    }
                    Box(Modifier.width(31.dp).height(45.dp).clip(RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp)).background(Color(0xFFE5F5EB)).padding(5.dp), contentAlignment = Alignment.Center) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) { repeat(3) { Box(Modifier.fillMaxWidth().height(2.dp).background(Green)) } }
                    }
                }
            }
            Spacer(Modifier.height(26.dp))
            Text("NextyLibrary", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(9.dp))
            Text("Buka wawasan. Temukan pengetahuan.", color = Color(0xFFE0F1E6), fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(34.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                repeat(3) { index ->
                    val dotAlpha by infinite.animateFloat(.35f, 1f, animationSpec = infiniteRepeatable(tween(650, delayMillis = index * 180), RepeatMode.Reverse), label = "dot$index")
                    Box(Modifier.size(7.dp).clip(CircleShape).background(Color.White.copy(alpha = dotAlpha)))
                }
            }
            Spacer(Modifier.height(54.dp))
            Text("NEXTY STUDIO", color = Color.White.copy(alpha = .72f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        }
    }
}

@Composable
fun NextyLibraryApp() {
    var entryStage by remember { mutableStateOf("intro") }
    LaunchedEffect(Unit) {
        delay(2600)
        if (entryStage == "intro") entryStage = "auth"
    }
    if (entryStage == "intro") {
        MaterialTheme(colorScheme = lightColorScheme(primary = Green, onPrimary = Color.White, background = Paper, surface = Color.White, onSurface = Ink)) {
            IntroScreen()
        }
        return
    }
    if (entryStage == "auth") {
        MaterialTheme(colorScheme = lightColorScheme(primary = Green, onPrimary = Color.White, background = Paper, surface = Color.White, onSurface = Ink)) {
            AccountScreen(onAuthenticated = { entryStage = "main" })
        }
        return
    }
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
                    ReaderScreen(reading!!, musicPlaying, { if (musicPlaying) { musicPlayer.pause(); musicPlaying = false } else { runCatching { musicPlayer.play() }.onSuccess { musicPlaying = true } } }, onBack = { reading = null })
                } else if (selected != null) {
                    BookDetail(selected!!, categories.firstOrNull { it.id == selected!!.categoryId }?.name, onBack = { selected = null }, onRead = { reading = selected })
                } else {
                    when (tab) {
                        "Beranda" -> Box(Modifier.weight(1f)) { HomeScreen(books, categories, loading, error, search, { search = it }, selectedCategory, { selectedCategory = it }, { reloadKey++ }, { selected = it }, musicPlayer, musicPlaying, { if (musicPlaying) { musicPlayer.pause(); musicPlaying = false } else { runCatching { musicPlayer.play() }.onSuccess { musicPlaying = true } } }, musicVolume, { musicVolume = it; musicPlayer.setVolume(it) }) }
                        "Koleksi" -> Box(Modifier.weight(1f)) { CollectionEmptyScreen() }
                        "Donasi" -> Box(Modifier.weight(1f)) { DonationScreen() }
                        else -> Box(Modifier.weight(1f)) { AccountScreen() }
                    }
                    NavigationBar(containerColor = Color.White, tonalElevation = 10.dp, modifier = Modifier.shadow(12.dp)) {
                        listOf("Beranda", "Koleksi", "Donasi", "Akun").forEach { item ->
                            NavigationBarItem(selected = tab == item, onClick = { tab = item },
                                icon = { Text(when (item) { "Beranda" -> "⌂"; "Koleksi" -> "▣"; "Donasi" -> "♡"; else -> "◉" }, fontSize = 21.sp, fontWeight = FontWeight.Bold) },
                                label = { Text(item, fontWeight = if (tab == item) FontWeight.Bold else FontWeight.Medium) },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = Green, selectedTextColor = Green, indicatorColor = Mint))
                        }
                    }
                }
            }
        }
    }
}


private const val NEXTY_QRIS_PAYLOAD = "00020101021126570011ID.DANA.WWW011893600915303674781302090367478130303UMI51440014ID.CO.QRIS.WWW0215ID10266334682060303UMI5204594553033605802ID5920Nexty Studio, Mainan6006Asahan61052127363041953"

@Composable
private fun DonationScreen() {
    val qrBitmap = remember {
        val matrix = QRCodeWriter().encode(NEXTY_QRIS_PAYLOAD, BarcodeFormat.QR_CODE, 720, 720)
        Bitmap.createBitmap(720, 720, Bitmap.Config.ARGB_8888).also { bitmap ->
            for (y in 0 until 720) for (x in 0 until 720) {
                bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(66.dp).clip(CircleShape).background(Mint),
                    contentAlignment = Alignment.Center
                ) { Text("♡", color = Green, fontSize = 36.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(14.dp))
                Text("Dukung NextyLibrary", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Dukunganmu membantu kami menghadirkan lebih banyak buku dan bahan belajar.",
                    color = Muted, fontSize = 14.sp, lineHeight = 21.sp, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(22.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("DONASI MELALUI QRIS", color = Green, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(14.dp))
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Kode QRIS donasi Nexty Studio",
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(Modifier.height(14.dp))
                        Text("Nexty Studio, Mainan", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text("NMID: ID1026633468206", color = Muted, fontSize = 13.sp)
                        Text("Kode QRIS: A01", color = Muted, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Mint)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Cara berdonasi", color = DeepGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Text("1. Simpan atau tampilkan kode QRIS ini.", color = Ink, fontSize = 14.sp)
                        Text("2. Buka aplikasi pembayaran yang mendukung QRIS pada perangkat lain.", color = Ink, fontSize = 14.sp)
                        Text("3. Pindai kode dan periksa nama penerima sebelum melanjutkan.", color = Ink, fontSize = 14.sp)
                        Text("4. Masukkan nominal sesuai keinginan dan konfirmasi hanya jika semua detail benar.", color = Ink, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Donasi sepenuhnya sukarela. Semua buku dan fitur NextyLibrary tetap gratis, tanpa iklan dan tanpa konten terkunci.",
                    color = Muted, fontSize = 13.sp, lineHeight = 19.sp, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Pastikan nama penerima yang muncul di aplikasi pembayaran adalah Nexty Studio, Mainan sebelum membayar.",
                    color = Green, fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center
                )
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
                Text("Tambah pengetahuan", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 20.dp))
                Text("Buku pengetahuan dalam bahasa Indonesia.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(start = 20.dp, top = 3.dp, bottom = 13.dp))
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
                Text(if (playing) "Musik sedang diputar • tetap aktif saat membaca" else "Tekan tombol putar untuk memulai musik latar", color = Muted, fontSize = 10.sp, maxLines = 2)
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
    @Volatile private var running = false
    @Volatile private var track: AudioTrack? = null
    @Volatile private var worker: Thread? = null
    @Volatile private var volume: Float = 1f
    private var loopSamples: ShortArray? = null

    fun setVolume(value: Float) {
        volume = value.coerceIn(0f, 1f)
        track?.setVolume(volume)
    }

    private fun makeMusic(): ShortArray {
        val sampleRate = 24000
        val frames = sampleRate * 8
        val samples = ShortArray(frames * 2)
        val chordSteps = intArrayOf(0, 5, 7, 3)
        val melody = intArrayOf(0, 4, 7, 11, 7, 4, 2, 7, 9, 7, 4, 2, 0, 4, 7, 4)
        for (frame in 0 until frames) {
            val time = frame.toDouble() / sampleRate
            val beat = time * 2.0
            val root = 196.0 * Math.pow(2.0, chordSteps[(time / 2.0).toInt() % chordSteps.size] / 12.0)
            val note = 440.0 * Math.pow(2.0, (melody[beat.toInt() % melody.size] - 9) / 12.0)
            val pulse = (beat % 1.0)
            val pluckEnvelope = Math.exp(-pulse * 5.5)
            val pad = sin(2.0 * PI * root * time) * 0.13 +
                sin(2.0 * PI * root * 1.5 * time) * 0.055
            val bass = sin(2.0 * PI * root * 0.5 * time) * 0.12
            val piano = (sin(2.0 * PI * note * time) + 0.2 * sin(2.0 * PI * note * 2.0 * time)) * pluckEnvelope * 0.34
            val fade = minOf((time / 0.15).coerceIn(0.0, 1.0), ((8.0 - time) / 0.15).coerceIn(0.0, 1.0))
            val sample = ((pad + bass + piano) * fade * 0.88 * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            samples[frame * 2] = sample
            samples[frame * 2 + 1] = sample
        }
        return samples
    }

    @Synchronized
    fun play() {
        if (running) return
        val samples = loopSamples ?: makeMusic().also { loopSamples = it }
        var audio = track
        if (audio == null || audio.state != AudioTrack.STATE_INITIALIZED) {
            val sampleRate = 24000
            val format = AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build()
            val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
            val minBuffer = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
            audio = AudioTrack.Builder().setAudioAttributes(attributes).setAudioFormat(format)
                .setTransferMode(AudioTrack.MODE_STREAM).setBufferSizeInBytes(maxOf(minBuffer * 2, 16384)).build()
            if (audio.state != AudioTrack.STATE_INITIALIZED) {
                audio.release()
                throw IllegalStateException("AudioTrack gagal diinisialisasi")
            }
            track = audio
        }
        audio.setVolume(volume)
        running = true
        audio.play()
        val activeTrack = audio
        worker = Thread {
            var cursor = 0
            val chunk = ShortArray(4096)
            try {
                while (running && track === activeTrack) {
                    for (i in chunk.indices) {
                        chunk[i] = samples[cursor]
                        cursor++
                        if (cursor >= samples.size) cursor = 0
                    }
                    val written = activeTrack.write(chunk, 0, chunk.size, AudioTrack.WRITE_BLOCKING)
                    if (written < 0) break
                }
            } catch (_: Exception) { }
        }.apply { name = "NextyLibraryMusic"; isDaemon = true; start() }
    }

    @Synchronized
    fun pause() {
        running = false
        track?.let { try { it.pause(); it.flush() } catch (_: Exception) { } }
        try { worker?.join(500) } catch (_: InterruptedException) { Thread.currentThread().interrupt() }
        worker = null
    }

    @Synchronized
    fun release() {
        running = false
        track?.let { try { it.pause(); it.flush() } catch (_: Exception) { } }
        try { worker?.join(500) } catch (_: InterruptedException) { Thread.currentThread().interrupt() }
        worker = null
        track?.let { try { it.release() } catch (_: Exception) { } }
        track = null
        loopSamples = null
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
private fun BookCoverArt(book: Book, categoryName: String?, modifier: Modifier = Modifier) {
    val title = book.title.lowercase()
    val palette = when {
        "tata surya" in title -> listOf(Color(0xFF101D45), Color(0xFF345A91))
        "ekosistem" in title || "iklim" in title -> listOf(Color(0xFF164B3A), Color(0xFF77A66D))
        "keuangan" in title -> listOf(Color(0xFF174A58), Color(0xFF4C9A91))
        "kemerdekaan" in title -> listOf(Color(0xFF713D36), Color(0xFFC27A52))
        "gizi" in title -> listOf(Color(0xFF7B4932), Color(0xFFD59A58))
        else -> listOf(Color(0xFF3D326D), Color(0xFF8A79B8))
    }
    val symbol = when {
        "tata surya" in title -> "🪐"
        "ekosistem" in title || "iklim" in title -> "🌿"
        "keuangan" in title -> "📊"
        "kemerdekaan" in title -> "🇮🇩"
        "gizi" in title -> "🥗"
        else -> "💡"
    }
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(palette))) {
        Box(Modifier.size(150.dp).align(Alignment.TopEnd).offset(x = 48.dp, y = (-42).dp).clip(CircleShape).background(Color.White.copy(alpha = .08f)))
        Box(Modifier.size(110.dp).align(Alignment.BottomStart).offset(x = (-30).dp, y = 38.dp).clip(CircleShape).background(Color.White.copy(alpha = .07f)))
        Column(Modifier.fillMaxSize().padding(horizontal = 15.dp, vertical = 16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text((categoryName ?: "PENGETAHUAN").uppercase(), color = Color.White.copy(alpha = .85f), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(symbol, fontSize = 35.sp)
                Text(book.title, color = Color.White, fontSize = 18.sp, lineHeight = 21.sp, fontWeight = FontWeight.ExtraBold, maxLines = 4, overflow = TextOverflow.Ellipsis)
                Box(Modifier.width(35.dp).height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = .75f)))
                Text(book.author, color = Color.White.copy(alpha = .82f), fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text("BUKU PENGETAHUAN", color = Color.White.copy(alpha = .75f), fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
        }
    }
}

@Composable
private fun BookGridCard(book: Book, categoryName: String?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.fillMaxWidth().padding(9.dp)) {
            Box(Modifier.fillMaxWidth().height(210.dp)) {
                if (!book.coverPath.isNullOrBlank()) {
                    AsyncImage(model = publicCoverUrl(book.coverPath), contentDescription = "Sampul " + book.title, modifier = Modifier.fillMaxSize())
                } else {
                    BookCoverArt(book, categoryName, Modifier.fillMaxSize())
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
                Box(Modifier.fillMaxWidth().height(360.dp)) {
                    if (!book.coverPath.isNullOrBlank()) {
                        AsyncImage(model = publicCoverUrl(book.coverPath), contentDescription = "Sampul " + book.title, modifier = Modifier.fillMaxSize())
                    } else {
                        BookCoverArt(book, categoryName, Modifier.fillMaxSize())
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
                Text(if (book.contentPath.isNullOrBlank()) "Isi buku belum tersedia." else "Bahasa Indonesia • Materi edukasi • Lisensi CC BY 4.0",
                    color = Muted, fontSize = 11.sp)
            }
        }
    }
}


private data class ReaderSection(val heading: String?, val paragraphs: List<String>)

private fun prepareReaderSections(raw: String, title: String): List<ReaderSection> {
    var text = raw.replace("\r\n", "\n").replace('\r', '\n')
    text = text.replace(Regex("(?im)^\\s*\\*{3}\\s*START OF (?:THE|THIS) PROJECT GUTENBERG EBOOK[^\\n]*\\n?"), "")
    text = text.replace(Regex("(?im)^\\s*\\*{3}\\s*END OF (?:THE|THIS) PROJECT GUTENBERG EBOOK[^\\n]*[\\s\\S]*$"), "")
    text = text.replace(Regex("(?is)\\[illustration:.*?\\]"), "")
    text = text.replace(Regex("(?m)^\\s*\\[.*?\\]\\s*$"), "")
    val titleWords = title.split(Regex("\\s+")).filter { it.length > 2 }
    val titleMatch = titleWords.firstOrNull()?.let { Regex(Regex.escape(it), RegexOption.IGNORE_CASE).find(text) }
    if (titleMatch != null && titleMatch.range.first > 0) text = text.substring(titleMatch.range.first)
    val chapterPattern = Regex("(?im)^\\s*(?:CHAPTER\\s+[IVXLCDM0-9]+[.:]?|ADVENTURE\\s+[IVXLCDM0-9]+[^\\n]*|BOOK\\s+[IVXLCDM0-9]+[^\\n]*|PART\\s+[IVXLCDM0-9]+[^\\n]*)\\s*$")
    val firstChapter = chapterPattern.find(text)
    if (firstChapter != null) text = text.substring(firstChapter.range.first)
    text = text.replace(Regex("(?m)^\\s*_{1,}([^\\n]+)_{1,}\\s*$")) { it.groupValues[1] }
        .replace(Regex("(?m)^\\s*\\*{2,}\\s*$"), "")
        .replace(Regex("\\n{3,}"), "\\n\\n")
        .trim()
    val chunks = text.split(Regex("\\n\\s*\\n")).map { it.trim().replace(Regex("\\s*\\n\\s*"), " ") }.filter { it.isNotBlank() }
    val headingRegex = Regex("(?i)^(?:CHAPTER\\s+[IVXLCDM0-9]+[.:]?|ADVENTURE\\s+[IVXLCDM0-9]+.*|BOOK\\s+[IVXLCDM0-9]+.*|PART\\s+[IVXLCDM0-9]+.*)$")
    val result = mutableListOf<ReaderSection>()
    var currentHeading: String? = null
    var paragraphs = mutableListOf<String>()
    fun flush() {
        if (paragraphs.isNotEmpty() || currentHeading != null) {
            result.add(ReaderSection(currentHeading, paragraphs.toList()))
            paragraphs = mutableListOf()
        }
    }
    for (chunk in chunks) {
        val cleaned = chunk.replace(Regex("_([^_]+)_")) { it.groupValues[1] }.replace(Regex("\\s{2,}"), " ").trim()
        if (headingRegex.matches(cleaned) || (cleaned.length in 3..90 && cleaned == cleaned.uppercase() && cleaned.any { it.isLetter() })) {
            flush()
            currentHeading = cleaned
        } else paragraphs.add(cleaned)
    }
    flush()
    return result
}

@Composable
private fun PdfBookReaderScreen(book: Book, musicPlaying: Boolean, onMusicToggle: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var pdfFile by remember(book.id) { mutableStateOf<File?>(null) }
    var renderer by remember(book.id) { mutableStateOf<PdfRenderer?>(null) }
    var descriptor by remember(book.id) { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pageCount by remember(book.id) { mutableIntStateOf(0) }
    var pageIndex by remember(book.id) { mutableIntStateOf(0) }
    var pageBitmap by remember(book.id) { mutableStateOf<Bitmap?>(null) }
    var loading by remember(book.id) { mutableStateOf(true) }
    var error by remember(book.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(book.id, book.contentPath) {
        loading = true
        error = null
        try {
            val source = book.contentPath ?: throw IllegalStateException("Alamat PDF belum diatur.")
            val file = withContext(Dispatchers.IO) {
                val url = when {
                    source.startsWith("https://") || source.startsWith("http://") -> source
                    source.startsWith("pdf://") -> BuildConfig.SUPABASE_URL.trimEnd('/') +
                        "/storage/v1/object/public/public-books/" +
                        source.removePrefix("pdf://").trimStart('/').split('/').joinToString("/") { Uri.encode(it) }
                    else -> throw IllegalStateException("Buku PDF belum memiliki alamat berkas yang sah.")
                }
                val target = File(context.cacheDir, "nextylibrary_${book.id}.pdf")
                val connection = URL(url).openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = 20000
                    connection.readTimeout = 60000
                    connection.instanceFollowRedirects = true
                    connection.setRequestProperty("User-Agent", "NextyLibrary/1.0")
                    if (connection.responseCode !in 200..299) throw IllegalStateException("PDF tidak dapat diunduh (HTTP ${connection.responseCode}).")
                    connection.inputStream.use { input -> FileOutputStream(target).use { output -> input.copyTo(output) } }
                } finally { connection.disconnect() }
                target
            }
            val opened = withContext(Dispatchers.IO) {
                val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                Pair(fd, PdfRenderer(fd))
            }
            descriptor?.close()
            renderer?.close()
            descriptor = opened.first
            renderer = opened.second
            pdfFile = file
            pageCount = opened.second.pageCount
            val saved = context.getSharedPreferences("reader_progress", android.content.Context.MODE_PRIVATE)
                .getInt(book.id, 0).coerceIn(0, (pageCount - 1).coerceAtLeast(0))
            pageIndex = saved
        } catch (e: Exception) {
            error = e.localizedMessage ?: "PDF gagal dibuka."
        } finally { loading = false }
    }

    LaunchedEffect(renderer, pageIndex) {
        val current = renderer ?: return@LaunchedEffect
        try {
            pageBitmap = withContext(Dispatchers.IO) {
                val page = current.openPage(pageIndex)
                try {
                    val width = 1200
                    val height = (width.toFloat() * page.height / page.width).toInt().coerceAtLeast(1)
                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                } finally { page.close() }
            }
            context.getSharedPreferences("reader_progress", android.content.Context.MODE_PRIVATE)
                .edit().putInt(book.id, pageIndex).apply()
        } catch (e: Exception) {
            error = e.localizedMessage ?: "Halaman PDF gagal ditampilkan."
        }
    }

    DisposableEffect(book.id) {
        onDispose {
            runCatching { renderer?.close() }
            runCatching { descriptor?.close() }
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF2F0E9))) {
        Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", color = Green, fontSize = 30.sp, modifier = Modifier.clickable(onClick = onBack))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(book.title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(book.author, color = Muted, fontSize = 10.sp)
            }
            FilledIconButton(onClick = onMusicToggle, colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (musicPlaying) Green else Color(0xFFE8EFEA), contentColor = if (musicPlaying) Color.White else Green)) {
                Text(if (musicPlaying) "Ⅱ" else "♫", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
        when {
            loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Green)
                    Spacer(Modifier.height(12.dp))
                    Text("Memuat buku PDF lengkap…", color = Muted)
                }
            }
            error != null -> Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(error ?: "PDF gagal dibuka.", color = Muted, textAlign = TextAlign.Center)
            }
            pageBitmap != null -> androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
            ) {
                item {
                    Image(
                        bitmap = pageBitmap!!.asImageBitmap(),
                        contentDescription = "Halaman ${pageIndex + 1} dari ${pageCount}",
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(5.dp)).shadow(2.dp)
                    )
                }
            }
            else -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Halaman PDF belum tersedia.", color = Muted)
            }
        }
        if (pageCount > 0) {
            Row(
                Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { pageIndex = (pageIndex - 1).coerceAtLeast(0) }, enabled = pageIndex > 0) { Text("‹ Sebelumnya") }
                Text("${pageIndex + 1} / ${pageCount}", color = DeepGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                TextButton(onClick = { pageIndex = (pageIndex + 1).coerceAtMost(pageCount - 1) }, enabled = pageIndex < pageCount - 1) { Text("Berikutnya ›") }
            }
        }
    }
}

private fun isPdfBookSource(source: String): Boolean {
    val normalized = source.lowercase()
    return normalized.startsWith("pdf://") ||
        normalized.substringBefore('?').endsWith(".pdf") ||
        normalized.contains("/view/pdfcoverpage") ||
        normalized.contains("format=pdf")
}

@Composable
private fun ReaderScreen(book: Book, musicPlaying: Boolean, onMusicToggle: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    if (book.contentPath?.let { isPdfBookSource(it) } == true) {
        PdfBookReaderScreen(book, musicPlaying, onMusicToggle, onBack)
        return
    }
    var body by remember(book.id) { mutableStateOf<String?>(null) }
    var error by remember(book.id) { mutableStateOf<String?>(null) }
    var loading by remember(book.id) { mutableStateOf(true) }
    var fontSize by remember(book.id) { mutableFloatStateOf(18f) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(book.id, book.contentPath) {
        loading = true
        error = null
        try {
            val source = book.contentPath ?: throw IllegalStateException("Sumber teks buku belum tersedia.")
            body = withContext(Dispatchers.IO) { if (source.startsWith("asset://")) context.assets.open(source.removePrefix("asset://")).bufferedReader(Charsets.UTF_8).use { it.readText() } else fetchPublicBookText(source) }
        } catch (e: Exception) {
            error = e.localizedMessage ?: "Buku gagal dimuat. Periksa koneksi internet."
        } finally { loading = false }
    }
    val sections = remember(body, book.title) { body?.let { prepareReaderSections(it, book.title) } ?: emptyList() }
    Column(Modifier.fillMaxSize().background(Color(0xFFF8F4EA))) {
        Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", color = Green, fontSize = 30.sp, modifier = Modifier.clickable(onClick = onBack))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(book.title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(book.author, color = Muted, fontSize = 10.sp)
            }
            FilledIconButton(onClick = onMusicToggle, colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (musicPlaying) Green else Color(0xFFE8EFEA), contentColor = if (musicPlaying) Color.White else Green)) {
                Text(if (musicPlaying) "Ⅱ" else "♫", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
        Row(Modifier.fillMaxWidth().background(Color(0xFFF0EADF)).padding(horizontal = 18.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Ukuran teks", color = Muted, fontSize = 10.sp)
            Slider(value = fontSize, onValueChange = { fontSize = it }, valueRange = 15f..23f, modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                colors = SliderDefaults.colors(thumbColor = Green, activeTrackColor = Green))
            Text(fontSize.toInt().toString(), color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Green)
                    Spacer(Modifier.height(12.dp))
                    Text("Menyiapkan halaman buku…", color = Muted)
                }
            }
            error != null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Buku belum dapat dibuka", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(error ?: "", color = Muted, textAlign = TextAlign.Center)
                }
            }
            sections.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Isi buku belum tersedia.", color = Muted)
            }
            else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(Color(0xFF174A3A), Color(0xFF4F8A70)))).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NEXTY LIBRARY", color = Color(0xFFD5EBDD), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                        Spacer(Modifier.height(28.dp))
                        Text(book.title, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 31.sp)
                        Spacer(Modifier.height(10.dp))
                        Text(book.author, color = Color(0xFFE3F0E8), fontSize = 14.sp)
                        Spacer(Modifier.height(25.dp))
                        Text("MATERI EDUKASI • BAHASA INDONESIA", color = Color(0xFFD5EBDD), fontSize = 9.sp, letterSpacing = 1.5.sp)
                    }
                }
                items(sections) { section ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFFFFDF8)).padding(horizontal = 20.dp, vertical = 22.dp)) {
                        if (!section.heading.isNullOrBlank()) {
                            Text(section.heading, color = DeepGreen, fontSize = 19.sp, fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp))
                            HorizontalDivider(color = Color(0xFFE7E0D3))
                            Spacer(Modifier.height(16.dp))
                        }
                        section.paragraphs.forEachIndexed { index, paragraph ->
                            Text(paragraph, color = Color(0xFF302F2A), fontSize = fontSize.sp, lineHeight = (fontSize * 1.65f).sp,
                                fontFamily = FontFamily.Serif, textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth().padding(bottom = if (index == section.paragraphs.lastIndex) 0.dp else 17.dp))
                        }
                    }
                }
                item {
                    Text("— Akhir buku —", color = Muted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp), textAlign = TextAlign.Center)
                }
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
