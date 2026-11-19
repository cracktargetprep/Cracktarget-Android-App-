package com.cracktarget.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.UUID
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

internal class Downloads(private val activity: Activity) {
    fun start(uri: Uri, userAgent: String?, disposition: String?, mimeType: String?) {
        if (uri.scheme != "https" || uri.host.isNullOrBlank() || uri.userInfo != null) {
            toast(R.string.download_failed)
            return
        }
        toast(R.string.download_started)
        executor.execute {
            try {
                val (file, mime) = download(uri, userAgent, disposition, mimeType)
                activity.runOnUiThread {
                    if (!activity.isDestroyed) {
                        toast(R.string.download_ready)
                        if (mime == "application/pdf" || Build.VERSION.SDK_INT < 29) open(file, mime)
                    }
                }
            } catch (_: IOException) {
                failed()
            } catch (_: SecurityException) {
                failed()
            } catch (_: IllegalArgumentException) {
                failed()
            }
        }
    }

    private fun download(
        uri: Uri, userAgent: String?, disposition: String?, mimeType: String?,
    ): Pair<Uri, String> {
        var current = uri
        // Once a redirect leaves CrackTarget, never send account cookies again.
        var allowAccountCookies = uri.isSite()
        repeat(6) {
            if (BuildConfig.PLAY_DISTRIBUTION && !PlayWebPolicy.pageAllowed(current.toString())) {
                throw IOException("Download unavailable in the Play edition")
            }
            val connection = URL(current.toString()).openConnection() as HttpsURLConnection
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 15_000
                connection.readTimeout = 25_000
                if (!userAgent.isNullOrBlank()) connection.setRequestProperty("User-Agent", userAgent)
                if (allowAccountCookies && current.isSite()) {
                    CookieManager.getInstance().getCookie(current.toString())?.takeIf(String::isNotBlank)
                        ?.let { connection.setRequestProperty("Cookie", it) }
                }
                val status = connection.responseCode
                if (status in listOf(301, 302, 303, 307, 308)) {
                    val location = connection.getHeaderField("Location") ?: throw IOException("Missing download location")
                    current = Uri.parse(URL(connection.url, location).toString())
                    if (current.scheme != "https" || current.userInfo != null || current.host.isNullOrEmpty()) {
                        throw IOException("Unsafe download location")
                    }
                    if (!current.isSite()) allowAccountCookies = false
                } else {
                    if (status !in 200..299) throw IOException("Download failed")
                    val type = connection.contentType?.substringBefore(';')?.trim()?.lowercase()
                    if (type == "text/html" || type == "application/xhtml+xml") throw IOException("Expected a document")
                    val mime = type?.takeUnless { it == "application/octet-stream" } ?: mimeType ?: "application/octet-stream"
                    val filename = URLUtil.guessFileName(current.toString(), connection.getHeaderField("Content-Disposition") ?: disposition, mime)
                        .replace(Regex("[^\\p{L}\\p{N}._() -]"), "_").trim('.', ' ')
                        .ifBlank { "cracktarget-download" }.takeLast(120)
                    return save(connection, filename, mime) to mime
                }
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("Too many download redirects")
    }

    private fun save(connection: HttpsURLConnection, filename: String, mime: String): Uri {
        if (Build.VERSION.SDK_INT >= 29) {
            val resolver = activity.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/CrackTarget")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Cannot create download")
            try {
                val output = resolver.openOutputStream(uri) ?: throw IOException("Cannot write download")
                output.use { target -> connection.inputStream.use { it.copyTo(target) } }
                resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
                return uri
            } catch (error: Exception) {
                resolver.delete(uri, null, null)
                throw error
            }
        }
        val directory = File(activity.filesDir, "downloads/${UUID.randomUUID()}")
        if (!directory.mkdirs()) throw IOException("Cannot create download directory")
        val file = File(directory, filename)
        try {
            file.outputStream().use { target -> connection.inputStream.use { it.copyTo(target) } }
        } catch (error: IOException) {
            file.delete()
            directory.delete()
            throw error
        }
        return FileProvider.getUriForFile(activity, "${activity.packageName}.downloads", file)
    }

    private fun open(file: Uri, mime: String) {
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(file, mime)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        } catch (_: ActivityNotFoundException) {
            toast(R.string.download_no_viewer)
        }
    }

    private fun failed() = activity.runOnUiThread { if (!activity.isDestroyed) toast(R.string.download_failed) }
    private fun toast(message: Int) = Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()

    private companion object {
        val executor = Executors.newSingleThreadExecutor()
    }
}
