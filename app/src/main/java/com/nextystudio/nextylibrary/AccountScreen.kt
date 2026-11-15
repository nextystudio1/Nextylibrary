package com.nextystudio.nextylibrary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

private data class AuthSession(val email: String, val accessToken: String)

@Composable
fun AccountScreen() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var session by remember { mutableStateOf<AuthSession?>(null) }
    var registerMode by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().background(Color(0xFFF3F7F3)).padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(26.dp))
        Box(Modifier.size(76.dp).background(Color(0xFFDDF1E6), RoundedCornerShape(25.dp)), contentAlignment = Alignment.Center) {
            Text("N", color = Color(0xFF16765A), fontSize = 39.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(17.dp))
        Text(if (session == null) "Akun NextyLibrary" else "Akun kamu", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF14251F))
        Text("Simpan koleksi dan nikmati pengalaman membaca.", color = Color(0xFF74847B), fontSize = 12.sp)
        Spacer(Modifier.height(25.dp))

        if (session == null) {
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
                                            session = AuthSession(email, result)
                                            password = ""
                                            message = null
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

private fun signInWithEmail(email: String, password: String): String {
    val body = JSONObject().put("email", email).put("password", password)
    val response = authRequest("/auth/v1/token?grant_type=password", body, null)
    val token = response.optString("access_token")
    if (token.isBlank()) throw IllegalStateException("Login belum menghasilkan sesi. Periksa konfirmasi email dan kredensial.")
    return token
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
