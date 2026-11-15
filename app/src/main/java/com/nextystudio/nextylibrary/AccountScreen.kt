package com.nextystudio.nextylibrary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.util.Base64
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.SecretKey
import org.json.JSONObject
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private data class AuthSession(val email: String, val accessToken: String, val refreshToken: String)

private const val SESSION_ALIAS = "nextylibrary_session_key"
private const val SESSION_PREFS = "nextylibrary_session"

private fun sessionKey(): SecretKey {
    val store = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    (store.getKey(SESSION_ALIAS, null) as? SecretKey)?.let { return it }
    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
    generator.init(KeyGenParameterSpec.Builder(SESSION_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build())
    return generator.generateKey()
}

private fun saveSession(context: Context, email: String, refreshToken: String) {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, sessionKey()) }
    val plain = "$email\\n$refreshToken".toByteArray(Charsets.UTF_8)
    val encrypted = cipher.iv + cipher.doFinal(plain)
    val encoded = Base64.encodeToString(encrypted, Base64.NO_WRAP)
    context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE).edit().putString("blob", encoded).apply()
}

private fun readSavedSession(context: Context): Pair<String, String>? = runCatching {
    val encoded = context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE).getString("blob", null) ?: return null
    val all = Base64.decode(encoded, Base64.NO_WRAP)
    val iv = all.copyOfRange(0, 12)
    val payload = all.copyOfRange(12, all.size)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, sessionKey(), GCMParameterSpec(128, iv)) }
    val parts = String(cipher.doFinal(payload), Charsets.UTF_8).split("\\n", limit = 2)
    if (parts.size != 2 || parts[1].isBlank()) null else Pair(parts[0], parts[1])
}.getOrNull()

private fun clearSavedSession(context: Context) { context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE).edit().clear().apply() }

@Composable
fun AccountScreen(onAuthenticated: () -> Unit = {}) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var session by remember { mutableStateOf<AuthSession?>(null) }
    var registerMode by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var restoring by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        val saved = withContext(Dispatchers.IO) { readSavedSession(context) }
        if (saved != null) {
            runCatching { withContext(Dispatchers.IO) { refreshAuthSession(saved.first, saved.second) } }
                .onSuccess { restored ->
                    withContext(Dispatchers.IO) { saveSession(context, restored.email, restored.refreshToken) }
                    session = restored
                    onAuthenticated()
                }
                .onFailure { withContext(Dispatchers.IO) { clearSavedSession(context) } }
        }
        restoring = false
    }

    Column(
        Modifier.fillMaxSize().background(Color(0xFFF3F7F3)).padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(26.dp))
        Box(Modifier.size(76.dp).background(Color(0xFFDDF1E6), RoundedCornerShape(25.dp)), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.width(20.dp).height(34.dp).background(Color.White, RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp)).padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(3) { Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF16765A).copy(alpha = 0.7f))) }
                    }
                }
                Spacer(Modifier.width(1.dp))
                Box(
                    Modifier.width(20.dp).height(34.dp).background(Color(0xFFF1FFF6), RoundedCornerShape(topEnd = 5.dp, bottomEnd = 5.dp)).padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(3) { Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF16765A).copy(alpha = 0.7f))) }
                    }
                }
            }
        }
        Spacer(Modifier.height(17.dp))
        Text(if (session == null) "Akun NextyLibrary" else "Akun kamu", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF14251F))
        Text("Simpan koleksi dan nikmati pengalaman membaca.", color = Color(0xFF74847B), fontSize = 12.sp)
        Spacer(Modifier.height(25.dp))

        if (restoring) {
            Spacer(Modifier.height(70.dp))
            CircularProgressIndicator(color = Color(0xFF16765A))
            Spacer(Modifier.height(12.dp))
            Text("Memeriksa sesi akun…", color = Color(0xFF74847B), fontSize = 12.sp)
        } else if (session == null) {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(19.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (registerMode) "Buat akun baru" else "Selamat datang kembali", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF14251F))
                    OutlinedTextField(
                        value = email, onValueChange = { email = it.trim() },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text("Email") }, placeholder = { Text("nama@email.com") },
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text("Kata sandi") }, visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    if (message != null) Text(message!!, color = Color(0xFF9A5B25), fontSize = 12.sp)
                    Button(
                        onClick = {
                            if (email.isBlank() || !email.contains("@") || password.length < 6) {
                                message = "Masukkan email yang valid dan kata sandi minimal 6 karakter."
                            } else {
                                scope.launch {
                                    busy = true
                                    message = null
                                    try {
                                        val result = withContext(Dispatchers.IO) {
                                            if (registerMode) registerWithEmail(email, password) else signInWithEmail(email, password)
                                        }
                                        if (registerMode) {
                                            message = result
                                            registerMode = false
                                        } else {
                                            session = result
                                            withContext(Dispatchers.IO) { saveSession(context, result.email, result.refreshToken) }
                                            password = ""
                                            message = null
                                            onAuthenticated()
                                        }
                                    } catch (e: Exception) {
                                        message = e.message ?: "Autentikasi gagal. Coba lagi."
                                    } finally { busy = false }
                                }
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (busy) CircularProgressIndicator(Modifier.size(19.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text(if (registerMode) "Daftar sekarang" else "Masuk", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { registerMode = !registerMode; message = null }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (registerMode) "Sudah punya akun? Masuk" else "Belum punya akun? Daftar")
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("Akun menggunakan Supabase Auth. Jika konfirmasi email aktif, periksa kotak masuk setelah mendaftar.", color = Color(0xFF74847B), fontSize = 11.sp, lineHeight = 17.sp)
        } else {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("EMAIL AKUN", color = Color(0xFF74847B), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(7.dp))
                    Text(session!!.email, color = Color(0xFF14251F), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(18.dp))
                    Text("Kamu sudah masuk. Fitur sinkronisasi koleksi dan progres akan dihubungkan pada tahap berikutnya.", color = Color(0xFF74847B), fontSize = 12.sp, lineHeight = 18.sp)
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            val current = session
                            scope.launch {
                                busy = true
                                try {
                                    withContext(Dispatchers.IO) { signOutFromSupabase(current?.accessToken.orEmpty()) }
                                } catch (_: Exception) {
                                    // Clear the local session even if the network is unavailable.
                                } finally {
                                    session = null
                                    withContext(Dispatchers.IO) { clearSavedSession(context) }
                                    email = ""
                                    password = ""
                                    busy = false
                                }
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (busy) "Memproses..." else "Keluar dari akun")
                    }
                }
            }
        }
    }
}

private fun registerWithEmail(email: String, password: String): String {
    val body = JSONObject().put("email", email).put("password", password)
    val response = authRequest("/auth/v1/signup", body, null)
    val user = response.optJSONObject("user") ?: response
    return if (user.optString("id").isNotBlank()) {
        "Pendaftaran berhasil. Jika verifikasi email aktif, buka email untuk mengonfirmasi akun lalu masuk."
    } else {
        "Permintaan pendaftaran terkirim. Periksa email untuk langkah berikutnya."
    }
}

private fun signInWithEmail(email: String, password: String): AuthSession {
    val body = JSONObject().put("email", email).put("password", password)
    val response = authRequest("/auth/v1/token?grant_type=password", body, null)
    val token = response.optString("access_token")
    val refresh = response.optString("refresh_token")
    if (token.isBlank() || refresh.isBlank()) throw IllegalStateException("Login belum menghasilkan sesi lengkap. Periksa konfirmasi email dan kredensial.")
    return AuthSession(email, token, refresh)
}

private fun refreshAuthSession(email: String, refreshToken: String): AuthSession {
    val body = JSONObject().put("refresh_token", refreshToken)
    val response = authRequest("/auth/v1/token?grant_type=refresh_token", body, null)
    val access = response.optString("access_token")
    val refresh = response.optString("refresh_token")
    if (access.isBlank() || refresh.isBlank()) throw IllegalStateException("Sesi akun tidak dapat diperbarui. Silakan masuk kembali.")
    val userEmail = response.optJSONObject("user")?.optString("email")?.takeIf { it.isNotBlank() } ?: email
    return AuthSession(userEmail, access, refresh)
}

private fun authRequest(path: String, body: JSONObject, bearer: String?): JSONObject {
    val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + path
    val connection = URL(endpoint).openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "POST"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.doOutput = true
        connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
        connection.setRequestProperty("Authorization", "Bearer " + (bearer ?: BuildConfig.SUPABASE_PUBLISHABLE_KEY))
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) {
            val error = runCatching { JSONObject(text).optString("msg").ifBlank { JSONObject(text).optString("message") } }.getOrNull()
            throw IllegalStateException(error?.takeIf { it.isNotBlank() } ?: "Supabase Auth merespons HTTP $code")
        }
        return JSONObject(text)
    } finally { connection.disconnect() }
}

private fun signOutFromSupabase(accessToken: String) {
    val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + "/auth/v1/logout"
    val connection = URL(endpoint).openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "POST"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.setRequestProperty("Accept", "application/json")
        val code = connection.responseCode
        if (code !in 200..299) throw IllegalStateException("Gagal mengakhiri sesi Auth (HTTP $code)")
    } finally { connection.disconnect() }
}
