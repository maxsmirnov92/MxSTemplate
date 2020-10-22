package net.maxsmr.core_network.downloader

import android.annotation.TargetApi
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.os.Build.VERSION_CODES.Q
import android.provider.MediaStore
import android.provider.MediaStore.Downloads.*
import net.maxsmr.commonutils.data.FileHelper
import net.maxsmr.commonutils.data.FileHelper.createNewFile
import net.maxsmr.core_network.utils.executeCall
import net.maxsmr.core_network.utils.responseBodyToOutputStream
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

private val PROJECTION = arrayOf(MediaStore.Downloads.TITLE)
private const val SORT_ORDER = MediaStore.Downloads.TITLE

/**
 * @param downloadDir нужно для версий < Q
 */
class OkHttpDownloader(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val downloadDir: File
) {

    fun listTitles() =
        if (Build.VERSION.SDK_INT >= Q) {
            listTitlesQ()
        } else {
            listTitlesLegacy()
        }

    @Throws(RuntimeException::class)
    fun download(url: String, filename: String, mimeType: String, requestConfigurator: (Request.Builder) -> Unit) {
        if (Build.VERSION.SDK_INT >= Q) {
            downloadQ(filename, mimeType, requestConfigurator)
        } else {
            downloadLegacy(filename, requestConfigurator)
        }
    }

    @TargetApi(Q)
    private fun listTitlesQ(): List<String>? = context.contentResolver.query(
        EXTERNAL_CONTENT_URI,
        PROJECTION,
        null,
        null,
        SORT_ORDER
    )?.use { cursor ->
        cursor.mapToList { it.getString(0) }
    }

    private fun listTitlesLegacy() =
        FileHelper.getFiles(downloadDir, FileHelper.GetMode.FILES, null, null, 1)
            .map { it.name }
            .sorted()

    @TargetApi(Q)
    @Throws(RuntimeException::class)
    private fun downloadQ(
        filename: String,
        mimeType: String,
        requestConfigurator: (Request.Builder) -> Unit
    ) {

        val values = ContentValues().apply {
            put(DISPLAY_NAME, filename)
            put(MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                put(IS_DOWNLOAD, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(EXTERNAL_CONTENT_URI, values)
            ?: throw RuntimeException("insert to ${EXTERNAL_CONTENT_URI} failed")


        val response = executeCall(okHttpClient, requestConfigurator)

        try {
            if (!response.isSuccessful) {
                throw RuntimeException("Response ended with: ${response.code()}")
            }

            values.clear()
            values.put(IS_PENDING, 1)

            try {
                responseBodyToOutputStream(response, resolver.openOutputStream(uri))
            } catch (e: FileNotFoundException) {
                throw RuntimeException(e)
            }

        } finally {
            with(values) {
                clear()
                put(IS_PENDING, 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    put(IS_DOWNLOAD, 0)
                }
                resolver.update(uri, values, null, null)
            }
        }
    }


    @Throws(RuntimeException::class)
    private fun downloadLegacy(
        filename: String,
        requestConfigurator: (Request.Builder) -> Unit
    ) {
        val response = executeCall(okHttpClient, requestConfigurator)

        if (!response.isSuccessful) {
            throw RuntimeException("Response ended with: ${response.code()}")
        }

        val newFile = createNewFile(filename, downloadDir.absolutePath) ?: throw RuntimeException("Can't create file $filename in $downloadDir")

        try {
            responseBodyToOutputStream(response, FileOutputStream(newFile))
        } catch (e: FileNotFoundException) {
            throw RuntimeException(e)
        }
    }

    private fun <T : Any> Cursor.mapToList(predicate: (Cursor) -> T): List<T> =
        generateSequence { if (moveToNext()) predicate(this) else null }
            .toList()
}
