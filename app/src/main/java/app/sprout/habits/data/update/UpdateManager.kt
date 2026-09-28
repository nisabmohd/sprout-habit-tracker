package app.sprout.habits.data.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import app.sprout.habits.BuildConfig
import app.sprout.habits.data.SettingsRepository
import app.sprout.habits.domain.isNewerVersion
import app.sprout.habits.domain.releaseHighlights
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** A newer release on GitHub. */
data class AppUpdate(
    val version: String,
    /** Up to five "What's new" lines from the release notes. */
    val notes: List<String>,
    /** The APK to download, or null when the release has none (then the release page opens). */
    val apkUrl: String?,
    val apkName: String?,
    /** SHA256SUMS.txt from the same release, to check the download against, if there is one. */
    val checksumsUrl: String?,
    val pageUrl: String,
)

sealed interface UpdateCheck {
    data class Available(val update: AppUpdate) : UpdateCheck
    data object UpToDate : UpdateCheck
    data object Failed : UpdateCheck
}

sealed interface DownloadState {
    /** 0..1, or null while the size is unknown. */
    data class Progress(val fraction: Float?) : DownloadState
    data class Ready(val apk: File) : DownloadState
    data object Failed : DownloadState
}

/**
 * Checks GitHub Releases for a newer Sprout and installs it: download the APK, check it against
 * the release's SHA256SUMS.txt, then hand it to Android's installer (which also checks it is
 * signed with the same key). Only the github build does this ([enabled]); the F-Droid and Play
 * builds are updated by their stores and never touch the network for it.
 */
class UpdateManager(private val context: Context, private val settings: SettingsRepository) {
    val enabled: Boolean = BuildConfig.GITHUB_UPDATES
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Asks GitHub now, and remembers the answer for the "Update available" pill. */
    suspend fun check(now: Long = System.currentTimeMillis()): UpdateCheck {
        if (!enabled) return UpdateCheck.UpToDate
        // Blocking network calls (DNS above all) ignore cancellation, so the request runs outside
        // this coroutine and the timeout can give up on it without waiting.
        val request = io.async { runCatching { latestRelease() }.getOrNull() }
        val result = withTimeoutOrNull(TIMEOUT_MS) { request.await() } ?: run {
            request.cancel()
            return UpdateCheck.Failed
        }
        val newer = result.takeIf { isNewerVersion(it.version, BuildConfig.VERSION_NAME) }
        settings.setUpdateCheck(newer?.version, now)
        return if (newer != null) UpdateCheck.Available(newer) else UpdateCheck.UpToDate
    }

    /** The automatic check: at most once a day, silent on failure, and never for a release the user put off. */
    suspend fun checkIfDue(now: Long = System.currentTimeMillis()): AppUpdate? {
        if (!enabled) return null
        val s = settings.settings.first()
        if (s.lastUpdateCheckAt != null && now - s.lastUpdateCheckAt < DAY_MS) return null
        val update = (check(now) as? UpdateCheck.Available)?.update ?: return null
        return update.takeIf { it.version != s.dismissedUpdate }
    }

    suspend fun later(update: AppUpdate) = settings.dismissUpdate(update.version)

    /** Downloads the release APK into the cache, reporting progress, and checks its SHA-256. */
    fun download(update: AppUpdate): Flow<DownloadState> = flow {
        val url = update.apkUrl ?: run { emit(DownloadState.Failed); return@flow }
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val apk = File(dir, "sprout-${update.version}.apk")
        val digest = MessageDigest.getInstance("SHA-256")
        try {
            emit(DownloadState.Progress(null))
            val c = open(url)
            val total = c.contentLengthLong.takeIf { it > 0 }
            c.inputStream.use { input ->
                apk.outputStream().use { out ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    var lastEmit = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        digest.update(buffer, 0, n)
                        done += n
                        if (total != null && done - lastEmit > total / 50) {
                            lastEmit = done
                            emit(DownloadState.Progress(done.toFloat() / total))
                        }
                    }
                }
            }
            val expected = update.checksumsUrl?.let { expectedSha256(it, update.apkName) }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (expected != null && !expected.equals(actual, ignoreCase = true)) {
                apk.delete()
                emit(DownloadState.Failed)
            } else {
                emit(DownloadState.Ready(apk))
            }
        } catch (e: IOException) {
            apk.delete()
            emit(DownloadState.Failed)
        }
    }.flowOn(Dispatchers.IO)

    /** False on Android 8+ until the user allows Sprout to install apps; see [openInstallPermission]. */
    fun canInstall(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    /** Opens "Install unknown apps" for Sprout. */
    fun openInstallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            start(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
        }
    }

    /** Hands the downloaded APK to Android's installer. */
    fun install(apk: File): Boolean {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        return start(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
        )
    }

    private fun start(intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }

    private fun latestRelease(): AppUpdate {
        val repo = BuildConfig.REPO_URL.removePrefix("https://github.com/").trimEnd('/')
        val json = Json.parseToJsonElement(read("https://api.github.com/repos/$repo/releases/latest")).jsonObject
        val assets = json["assets"]?.jsonArray.orEmpty().map { it.jsonObject }
        fun name(a: kotlinx.serialization.json.JsonObject) = a["name"]?.jsonPrimitive?.content.orEmpty()
        // This build's own APK first, then any APK that isn't the Play build.
        val apk = assets.firstOrNull { name(it).endsWith("-github.apk") }
            ?: assets.firstOrNull { name(it).endsWith(".apk") && !name(it).contains("-play") }
        val sums = assets.firstOrNull { name(it).equals("SHA256SUMS.txt", ignoreCase = true) }
        return AppUpdate(
            version = json.getValue("tag_name").jsonPrimitive.content.removePrefix("v"),
            notes = releaseHighlights(json["body"]?.jsonPrimitive?.content),
            apkUrl = apk?.get("browser_download_url")?.jsonPrimitive?.content,
            apkName = apk?.let(::name),
            checksumsUrl = sums?.get("browser_download_url")?.jsonPrimitive?.content,
            pageUrl = json["html_url"]?.jsonPrimitive?.content ?: "${BuildConfig.REPO_URL}/releases/latest",
        )
    }

    /** The hash listed for [apkName] in a SHA256SUMS.txt ("<hash>  <file>" lines), if listed. */
    private fun expectedSha256(url: String, apkName: String?): String? {
        apkName ?: return null
        return runCatching { read(url) }.getOrNull()?.lineSequence()
            ?.map { it.trim().split(Regex("\\s+")) }
            ?.firstOrNull { it.size >= 2 && it[1].removePrefix("*") == apkName }
            ?.get(0)
    }

    private fun read(url: String): String = open(url).inputStream.use { it.readBytes().decodeToString() }

    private fun open(url: String): HttpURLConnection {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 30_000
        c.setRequestProperty("Accept", "application/vnd.github+json")
        c.setRequestProperty("User-Agent", "Sprout/${BuildConfig.VERSION_NAME}")
        if (c.responseCode !in 200..299) {
            c.disconnect()
            throw IOException("HTTP ${c.responseCode} for $url")
        }
        return c
    }

    companion object {
        private const val TIMEOUT_MS = 10_000L
        private const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
