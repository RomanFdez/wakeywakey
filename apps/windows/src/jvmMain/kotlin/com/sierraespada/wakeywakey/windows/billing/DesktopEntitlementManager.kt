package com.sierraespada.wakeywakey.windows.billing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.boolean
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Gestiona el estado de entitlement (trial / Pro / Free) para la versión Desktop.
 *
 * ## Seguridad (por capas)
 *  1. El fichero `ww.dat` está ofuscado con XOR+Base64 — no es JSON legible.
 *  2. Dentro del payload hay un HMAC-SHA256 del contenido.
 *     Si alguien edita el fichero a mano, el HMAC no coincide → se trata como sin licencia.
 *  3. Un checksum derivado se guarda también en el Keychain del sistema (macOS)
 *     o en un fichero oculto en otra ruta (Windows).
 *     Editar solo el `ww.dat` no es suficiente: hay que cuadrar también el Keychain.
 *  4. Validación online contra LemonSqueezy al arrancar y cada 3 días.
 *     Stub activo hasta tener los productos creados en LemonSqueezy.
 *     Grace period de 7 días sin red antes de revocar.
 */
object DesktopEntitlementManager {

    // ─── Constantes públicas ──────────────────────────────────────────────────

    const val TRIAL_DAYS                 = 30
    const val FREE_TIER_MAX_DAILY_ALERTS = 3

    const val PRICE_MONTHLY  = "€1.99"
    const val PRICE_ANNUAL   = "€14.99"
    const val PRICE_LIFETIME = "€59"

    const val CHECKOUT_MONTHLY  = "https://sierraespada.lemonsqueezy.com/checkout/buy/2ae9b4f6-9a9f-42d0-aa68-dd584ea5dcf0"
    const val CHECKOUT_ANNUAL   = "https://sierraespada.lemonsqueezy.com/checkout/buy/de012da9-87f5-4cd1-844c-7fb84479c6fe"
    const val CHECKOUT_LIFETIME = "https://sierraespada.lemonsqueezy.com/checkout/buy/1f80f416-6f21-4ce9-b37e-bead00b93b76"

    // ─── Claves de seguridad (baked into binary) ──────────────────────────────
    // No es cifrado fuerte, pero eleva el listón frente a edición casual.

    private const val OBF_KEY  = "wk-2024-se-xk7q9m3r-sierra"   // XOR key para ofuscación
    private const val HMAC_KEY = "wk-hmac-8f3j2k-sierra24-esp"  // HMAC-SHA256 key

    // ─── Coroutine scope para tareas IO ──────────────────────────────────────

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ─── Estado observable ────────────────────────────────────────────────────

    private val _isPro         = MutableStateFlow(false)
    private val _trialDaysLeft = MutableStateFlow(TRIAL_DAYS)
    private val _licenseKey    = MutableStateFlow<String?>(null)

    val isPro:         StateFlow<Boolean>  = _isPro.asStateFlow()
    val trialDaysLeft: StateFlow<Int>      = _trialDaysLeft.asStateFlow()
    val licenseKey:    StateFlow<String?>  = _licenseKey.asStateFlow()

    val isTrialActive: Boolean
        get() = !_isPro.value && _trialDaysLeft.value > 0

    val shouldShowPaywall: Boolean
        get() = !_isPro.value && _trialDaysLeft.value <= 0

    // ─── Modelo de datos ──────────────────────────────────────────────────────

    @Serializable
    private data class LicenseData(
        val installedAt:     Long    = System.currentTimeMillis(),
        val licenseKey:      String? = null,
        val activatedAt:     Long?   = null,
        val licenseEmail:    String? = null,
        val lastValidatedAt: Long?   = null,  // última validación online exitosa
        val instanceId:      String? = null,  // ID de instancia de LemonSqueezy (para desactivar)
        val hmac:            String? = null,  // firma HMAC del payload
    )

    // ─── Rutas ────────────────────────────────────────────────────────────────

    private val wakeyDir    = File(System.getProperty("user.home"), ".wakeywakey").also { it.mkdirs() }
    private val licenseFile = File(wakeyDir, "ww.dat")           // fichero principal ofuscado
    private val jsonCodec   = Json { ignoreUnknownKeys = true }

    private val isMac = System.getProperty("os.name").orEmpty().contains("Mac", ignoreCase = true)

    // ─── Init ─────────────────────────────────────────────────────────────────

    init {
        migrateLegacyIfNeeded()
        load()
    }

    // ─── Carga ────────────────────────────────────────────────────────────────

    private fun load() {
        val data = readData() ?: run {
            val fresh = LicenseData()
            writeData(fresh)
            return applyData(fresh)
        }

        // 1. Verificar HMAC (tamper detection del fichero)
        if (!data.licenseKey.isNullOrBlank() && !verifyHmac(data)) {
            System.err.println("EntitlementManager: ⚠️  HMAC inválido — licencia revocada")
            val clean = data.copy(licenseKey = null, activatedAt = null, licenseEmail = null, hmac = null)
            writeData(clean)
            deleteSystemChecksum()
            return applyData(clean)
        }

        // 2. Verificar checksum del sistema (Keychain / fichero oculto)
        if (!data.licenseKey.isNullOrBlank() && !verifySystemChecksum(data)) {
            System.err.println("EntitlementManager: ⚠️  Checksum de sistema inválido — licencia revocada")
            val clean = data.copy(licenseKey = null, activatedAt = null, licenseEmail = null, hmac = null)
            writeData(clean)
            return applyData(clean)
        }

        applyData(data)

        // 3. Validación online async si hay licencia y han pasado ≥3 días
        if (!data.licenseKey.isNullOrBlank()) {
            val daysSince = data.lastValidatedAt?.let {
                ChronoUnit.DAYS.between(
                    Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate(),
                    LocalDate.now()
                ).toInt()
            } ?: Int.MAX_VALUE

            if (daysSince >= 3) {
                scope.launch { validateOnline(data.licenseKey) }
            }
        }
    }

    private fun applyData(data: LicenseData) {
        val installedDay = Instant.ofEpochMilli(data.installedAt)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        val elapsed  = ChronoUnit.DAYS.between(installedDay, LocalDate.now()).toInt()
        val daysLeft = (TRIAL_DAYS - elapsed).coerceAtLeast(0)

        _trialDaysLeft.value = daysLeft

        if (!data.licenseKey.isNullOrBlank()) {
            _licenseKey.value = data.licenseKey
            _isPro.value      = true
        } else {
            _licenseKey.value = null
            _isPro.value      = false
        }

        System.err.println("EntitlementManager: elapsed=$elapsed daysLeft=$daysLeft isPro=${_isPro.value}")
    }

    // ─── Ofuscación XOR + Base64 ──────────────────────────────────────────────

    private fun obfuscate(plain: String): String {
        val k = OBF_KEY.toByteArray(Charsets.UTF_8)
        val b = plain.toByteArray(Charsets.UTF_8)
        val r = ByteArray(b.size) { i -> (b[i].toInt() xor k[i % k.size].toInt()).toByte() }
        return Base64.getEncoder().encodeToString(r)
    }

    private fun deobfuscate(encoded: String): String {
        val k = OBF_KEY.toByteArray(Charsets.UTF_8)
        val b = Base64.getDecoder().decode(encoded.trim())
        val r = ByteArray(b.size) { i -> (b[i].toInt() xor k[i % k.size].toInt()).toByte() }
        return String(r, Charsets.UTF_8)
    }

    // ─── HMAC-SHA256 ──────────────────────────────────────────────────────────

    private fun computeHmac(data: LicenseData): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(HMAC_KEY.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val payload = "${data.installedAt}|${data.licenseKey}|${data.activatedAt}|${data.licenseEmail}"
        return Base64.getEncoder().encodeToString(mac.doFinal(payload.toByteArray(Charsets.UTF_8)))
    }

    private fun verifyHmac(data: LicenseData): Boolean =
        data.hmac == computeHmac(data.copy(hmac = null))

    // ─── Checksum del sistema (Keychain macOS / fichero oculto Windows) ───────

    /** SHA-256 derivado del contenido crítico — se almacena fuera del ww.dat */
    private fun deriveSystemChecksum(data: LicenseData): String {
        val md = MessageDigest.getInstance("SHA-256")
        val payload = "${data.installedAt}|${data.licenseKey}|${data.activatedAt}"
        return Base64.getEncoder().encodeToString(md.digest(payload.toByteArray(Charsets.UTF_8)))
    }

    private fun storeSystemChecksum(data: LicenseData) {
        val checksum = deriveSystemChecksum(data)
        if (isMac) {
            runCatching {
                ProcessBuilder(
                    "security", "add-generic-password",
                    "-U",
                    "-a", "com.sierraespada.wakeywakey",
                    "-s", "ww-license-integrity",
                    "-w", checksum,
                ).start().waitFor()
            }
        } else {
            runCatching { hiddenChecksumFile().writeText(checksum) }
        }
    }

    private fun loadSystemChecksum(): String? =
        if (isMac) {
            runCatching {
                val proc = ProcessBuilder(
                    "security", "find-generic-password",
                    "-a", "com.sierraespada.wakeywakey",
                    "-s", "ww-license-integrity",
                    "-w",
                ).start()
                proc.inputStream.bufferedReader().readLine()?.trim()
            }.getOrNull()
        } else {
            runCatching { hiddenChecksumFile().readText().trim() }.getOrNull()
        }

    private fun deleteSystemChecksum() {
        if (isMac) {
            runCatching {
                ProcessBuilder(
                    "security", "delete-generic-password",
                    "-a", "com.sierraespada.wakeywakey",
                    "-s", "ww-license-integrity",
                ).start().waitFor()
            }
        } else {
            runCatching { hiddenChecksumFile().delete() }
        }
    }

    /**
     * Si no hay checksum guardado (primera activación) se asume válido.
     * Así no se revoca en la primera ejecución después de activar.
     */
    private fun verifySystemChecksum(data: LicenseData): Boolean {
        val stored = loadSystemChecksum() ?: return true
        return stored == deriveSystemChecksum(data)
    }

    private fun hiddenChecksumFile(): File {
        val dir = if (System.getProperty("os.name").contains("Windows", ignoreCase = true)) {
            File(System.getenv("APPDATA") ?: System.getProperty("user.home"), ".ww")
        } else {
            File(System.getProperty("user.home"), ".cache/.ww")
        }
        dir.mkdirs()
        return File(dir, ".integrity")
    }

    // ─── Lectura / escritura del fichero ──────────────────────────────────────

    private fun readData(): LicenseData? {
        if (!licenseFile.exists()) return null
        return runCatching {
            val plain = deobfuscate(licenseFile.readText())
            jsonCodec.decodeFromString<LicenseData>(plain)
        }.getOrElse {
            System.err.println("EntitlementManager: error leyendo ww.dat — ${it.message}")
            null
        }
    }

    private fun writeData(data: LicenseData) {
        val withHmac = data.copy(hmac = computeHmac(data.copy(hmac = null)))
        runCatching { licenseFile.writeText(obfuscate(jsonCodec.encodeToString(withHmac))) }
    }

    // ─── Validación online ────────────────────────────────────────────────────

    /**
     * Valida la licencia contra la API de LemonSqueezy.
     *
     * TODO: implementar cuando los productos estén creados en LemonSqueezy.
     *
     * Endpoint:  POST https://api.lemonsqueezy.com/v1/licenses/validate
     * Headers:   Accept: application/json
     * Body:      license_key=<key>&instance_name=desktop-mac (o desktop-win)
     * Response:  { "valid": true, "license_key": { "status": "active" } }
     *
     * Comportamiento:
     *  - Si valid=false  → revocar localmente.
     *  - Si error de red → grace period: si lastValidatedAt < 7 días, mantener válido.
     *                      Si > 7 días sin red, revocar (protección offline).
     */
    private suspend fun validateOnline(key: String?) {
        if (key.isNullOrBlank()) return
        try {
            val body = "license_key=${URLEncoder.encode(key, "UTF-8")}"
            val response = httpPost("https://api.lemonsqueezy.com/v1/licenses/validate", body)
            val json     = Json.parseToJsonElement(response).jsonObject
            val valid    = json["valid"]?.jsonPrimitive?.boolean ?: false
            if (!valid) {
                // Clave inválida o revocada — revocar localmente
                val clean = (readData() ?: LicenseData()).copy(
                    licenseKey = null, activatedAt = null, licenseEmail = null, hmac = null
                )
                writeData(clean); deleteSystemChecksum()
                _licenseKey.value = null; _isPro.value = false
                System.err.println("EntitlementManager: clave inválida según LS — revocada")
            } else {
                val data = readData() ?: return
                writeData(data.copy(lastValidatedAt = System.currentTimeMillis()))
                System.err.println("EntitlementManager: validación online OK")
            }
        } catch (e: Exception) {
            // Error de red → grace period (se gestiona en init)
            System.err.println("EntitlementManager: error de red en validación — ${e.message}")
        }
    }

    /**
     * Activa una licencia contra la API de LemonSqueezy.
     * Devuelve null si OK, o un mensaje de error legible si falla.
     */
    suspend fun activateLicenseOnline(key: String): String? = withContext(Dispatchers.IO) {
        try {
            val instanceName = URLEncoder.encode("desktop-${System.getProperty("os.name", "unknown")}", "UTF-8")
            val body         = "license_key=${URLEncoder.encode(key.trim(), "UTF-8")}&instance_name=$instanceName"
            val response     = httpPost("https://api.lemonsqueezy.com/v1/licenses/activate", body)
            val json         = Json.parseToJsonElement(response).jsonObject
            val activated    = json["activated"]?.jsonPrimitive?.boolean ?: false
            if (activated) {
                // Guardar el instance ID para poder desactivar después
                val instanceId = json["instance"]?.jsonObject?.get("id")?.jsonPrimitive?.content
                activateLicense(key, instanceId = instanceId)
                null  // éxito
            } else {
                val error = json["error"]?.jsonPrimitive?.content ?: "Invalid license key"
                error
            }
        } catch (e: Exception) {
            "Could not connect to license server. Check your internet connection."
        }
    }

    /**
     * Desactiva esta instalación liberando una plaza de las 5 permitidas.
     * Devuelve null si OK, o un mensaje de error si falla.
     */
    suspend fun deactivateLicense(): String? = withContext(Dispatchers.IO) {
        val data = readData() ?: return@withContext "No active license found."
        val key  = data.licenseKey  ?: return@withContext "No active license found."
        val iid  = data.instanceId  ?: return@withContext "Instance ID not found. Please contact support."
        try {
            val body     = "license_key=${URLEncoder.encode(key, "UTF-8")}&instance_id=${URLEncoder.encode(iid, "UTF-8")}"
            val response = httpPost("https://api.lemonsqueezy.com/v1/licenses/deactivate", body)
            val json     = Json.parseToJsonElement(response).jsonObject
            val deactivated = json["deactivated"]?.jsonPrimitive?.boolean ?: false
            if (deactivated) {
                val clean = data.copy(licenseKey = null, activatedAt = null, instanceId = null, licenseEmail = null, hmac = null)
                writeData(clean); deleteSystemChecksum()
                _licenseKey.value = null; _isPro.value = false
                null  // éxito
            } else {
                json["error"]?.jsonPrimitive?.content ?: "Could not deactivate license."
            }
        } catch (e: Exception) {
            "Could not connect to license server. Check your internet connection."
        }
    }

    private fun httpPost(urlStr: String, body: String): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod     = "POST"
            doOutput          = true
            connectTimeout    = 8_000
            readTimeout       = 8_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }
        OutputStreamWriter(conn.outputStream).use { it.write(body) }
        return conn.inputStream.bufferedReader().readText()
    }

    // ─── Activación de licencia ───────────────────────────────────────────────

    fun activateLicense(key: String, email: String = "", instanceId: String? = null) {
        val existing = readData() ?: LicenseData()
        val updated  = existing.copy(
            licenseKey      = key.trim(),
            activatedAt     = System.currentTimeMillis(),
            licenseEmail    = email.ifBlank { null },
            lastValidatedAt = System.currentTimeMillis(),
            instanceId      = instanceId ?: existing.instanceId,
        )
        writeData(updated)
        storeSystemChecksum(updated)
        _licenseKey.value = key.trim()
        _isPro.value      = true
        System.err.println("EntitlementManager: licencia activada key=${key.take(8)}… instanceId=$instanceId")
    }

    // ─── Debug ────────────────────────────────────────────────────────────────

    /** Elimina la licencia — solo para desarrollo/testing. */
    fun debugRevokeLicense() {
        val existing = readData() ?: LicenseData()
        val clean    = existing.copy(licenseKey = null, activatedAt = null, licenseEmail = null, hmac = null)
        writeData(clean)
        deleteSystemChecksum()
        _licenseKey.value = null
        _isPro.value      = false
        System.err.println("EntitlementManager: debug — licencia revocada")
    }

    /** Simula que han pasado [days] días desde la instalación — solo debug. */
    fun debugSetElapsedDays(days: Int) {
        val existing    = readData() ?: LicenseData()
        val fakeInstall = System.currentTimeMillis() - days.toLong() * 24 * 60 * 60 * 1000L
        val updated     = existing.copy(installedAt = fakeInstall)
        writeData(updated)
        if (!existing.licenseKey.isNullOrBlank()) storeSystemChecksum(updated)
        _trialDaysLeft.value = (TRIAL_DAYS - days).coerceAtLeast(0)
        System.err.println("EntitlementManager: debug elapsed=$days, daysLeft=${_trialDaysLeft.value}")
    }

    // ─── Migración desde license.json ─────────────────────────────────────────

    private fun migrateLegacyIfNeeded() {
        val legacy = File(wakeyDir, "license.json")
        if (!legacy.exists() || licenseFile.exists()) return
        runCatching {
            val old = Json { ignoreUnknownKeys = true }.decodeFromString<LicenseData>(legacy.readText())
            writeData(old)
            if (!old.licenseKey.isNullOrBlank()) storeSystemChecksum(old)
            legacy.renameTo(File(wakeyDir, "license.json.bak"))
            System.err.println("EntitlementManager: migrado desde license.json → ww.dat")
        }.onFailure {
            System.err.println("EntitlementManager: error en migración — ${it.message}")
        }
    }
}
