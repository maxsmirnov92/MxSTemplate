package net.maxsmr.core_network.downloader

import android.annotation.TargetApi
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES.Q
import android.provider.MediaStore.Downloads.*
import androidx.core.content.FileProvider
import net.maxsmr.commonutils.media.*
import net.maxsmr.commonutils.*
import net.maxsmr.core_network.error.exception.http.HttpProtocolException
import net.maxsmr.core_network.utils.executeCall
import net.maxsmr.core_network.utils.isResumeDownloadSupported
import net.maxsmr.core_network.utils.responseBodyToOutputStream
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.File

private val PROJECTION = listOf(TITLE)
private const val SORT_ORDER = TITLE

private const val HEADER_RANGE = "Range"

/**
 * @param downloadDir нужно для версий < Q
 */
class OkHttpDownloader(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val downloadDir: File
) {

    private val contentResolver = context.contentResolver

    fun listTitles(): List<String> =
        if (Build.VERSION.SDK_INT >= Q) {
            listTitlesQ()
        } else {
            listTitlesLegacy()
        }

    @Throws(RuntimeException::class)
    fun download(
        filename: String,
        mimeType: String,
        resumeDownloadIfPossible: Boolean,
        deleteUnfinishedFile: DeleteUnfinishedMode,
        existingUri: Uri?,
        requestConfigurator: (Request.Builder) -> String,
        deleteHandler: ((SecurityException) -> Unit)? = null,
        insertUpdateHandler: ((SecurityException) -> Unit)? = null,
        downloadNotifier: IDownloadNotifier? = null
    ): Uri = if (Build.VERSION.SDK_INT >= Q) {
        downloadQ(
            filename,
            mimeType,
            resumeDownloadIfPossible,
            deleteUnfinishedFile,
            existingUri,
            requestConfigurator = requestConfigurator,
            deleteHandler = deleteHandler,
            insertUpdateHandler = insertUpdateHandler,
            downloadNotifier = downloadNotifier
        )
    } else {
        downloadLegacy(
            filename,
            resumeDownloadIfPossible,
            deleteUnfinishedFile,
            existingUri,
            requestConfigurator,
            downloadNotifier
        )
    }

    @TargetApi(Q)
    private fun listTitlesQ(): List<String> = queryUri(
        contentResolver,
        EXTERNAL_CONTENT_URI,
        String::class.java,
        PROJECTION,
        sortOrder = SORT_ORDER
    )

    private fun listTitlesLegacy(): List<String> =
        getFiles(downloadDir, GetMode.FILES, depth = 0)
            .filter { isFileExists(it) }
            .map { it.name }
            .sorted()

    @TargetApi(Q)
    @Throws(RuntimeException::class)
    private fun downloadQ(
        filename: String,
        mimeType: String,
        resumeDownloadIfPossible: Boolean,
        deleteUnfinishedFile: DeleteUnfinishedMode,
        existingUri: Uri?,
        tryDeleteExisting: Boolean = false,
        requestConfigurator: (Request.Builder) -> String,
        deleteHandler: ((SecurityException) -> Unit)? = null,
        insertUpdateHandler: ((SecurityException) -> Unit)? = null,
        downloadNotifier: IDownloadNotifier? = null
    ): Uri {

        var existingUri = existingUri

        if (existingUri != null && ContentResolver.SCHEME_CONTENT != existingUri.scheme) {
            existingUri = null
        }

        val resolver = context.contentResolver

        fun tryInsert(values: ContentValues) = try {
            resolver.insert(EXTERNAL_CONTENT_URI, values)
        } catch (e: Exception) {
            if (e is SecurityException) {
                insertUpdateHandler?.invoke(e)
            }
            throw RuntimeException("Cannot insert to $EXTERNAL_CONTENT_URI", e)
        }

        fun tryUpdate(uri: Uri, values: ContentValues) = try {
            resolver.update(uri, values, null, null)
        } catch (e: SecurityException) {
            insertUpdateHandler?.invoke(e)
            throw RuntimeException("Cannot update uri $uri", e)
        }

        val request = Request.Builder()
        val downloadUrl = requestConfigurator(request)

        val values = ContentValues().apply {
//            put(DOCUMENT_ID, downloadId)
            put(TITLE, filename)
            put(DISPLAY_NAME, filename)
            put(MIME_TYPE, mimeType)
            put(DOWNLOAD_URI, downloadUrl)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                put(IS_DOWNLOAD, 1)
            }
        }

        if (tryDeleteExisting && existingUri != null && isResourceExists(contentResolver, existingUri)) {
            // апдейт не вызываем, удаляем то, что есть
            // (даже если файл не сушествует или его хэш неправильный - запись в таблице есть)
            try {
                resolver.delete(existingUri, null, null)
                existingUri = null
            } catch (e: SecurityException) {
                deleteHandler?.invoke(e)
                throw RuntimeException("Cannot delete existing resource $existingUri", e)
            }
        }

        val previousSize: Long
        val uri: Uri
        if (existingUri == null || !resumeDownloadIfPossible) {
            // вставка в external с нуля
            uri = tryInsert(values) ?: throw RuntimeException("Insert to $EXTERNAL_CONTENT_URI failed")
            previousSize = 0
        } else {
            // актуализируем инфу по текущей урле
            tryUpdate(existingUri, values)
            uri = existingUri
            previousSize = getResourceSize(contentResolver, existingUri)
        }

        downloadNotifier?.onUriReady(uri)

        addRangeHeader(request, previousSize)

        val response = try {
            okHttpClient.newCall(request.build()).execute()
        } catch (e: Exception) {
            throw RuntimeException("Request execute failed", e)
        }

        val isSuccessful = response.isSuccessful

        if (isSuccessful) {
            with(values) {
                clear()
                put(IS_PENDING, 1)
                tryUpdate(uri, this)
            }
        }

        val isResumeSupported = isSuccessful && isResumeDownloadSupported(response)

        checkResponseOrThrow(response)

        var responseBody: ResponseBody? = null
        try {
            responseBody = responseBodyToOutputStream(
                response,
                uri.openOutputStreamOrThrow(resolver),
                previousSize,
                downloadNotifier
            )
        } catch (e: RuntimeException) {
            val resultException = HttpProtocolException.RawBuilder(response, e).build()
            if (deleteUnfinishedFile.shouldDelete(isResumeSupported)) {
                try {
                    deleteResourceOrThrow(contentResolver, uri)
                } catch (e: RuntimeException) {
                    throw RuntimeException("Cannot delete unfinished resource $uri", resultException)
                }
            }
            throw resultException
        } finally {
            if (responseBody != null // response был получен
                || !deleteUnfinishedFile.shouldDelete(isResumeSupported)
            ) {
                with(values) {
                    clear()
                    put(IS_PENDING, 0)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        put(IS_DOWNLOAD, 0)
                    }
                    tryUpdate(uri, this)
                }
            }
        }

        return uri
    }


    @Throws(RuntimeException::class)
    private fun downloadLegacy(
        filename: String,
        resumeDownloadIfPossible: Boolean,
        deleteUnfinishedFile: DeleteUnfinishedMode,
        existingUri: Uri?,
        requestConfigurator: (Request.Builder) -> String,
        downloadNotifier: IDownloadNotifier? = null
    ): Uri {

        val previousSize = if (resumeDownloadIfPossible) getResourceSize(contentResolver, existingUri) else 0

        val response = executeCall(okHttpClient) {
            requestConfigurator(it)
            addRangeHeader(it, previousSize)
        }

        checkResponseOrThrow(response)

        val isResumeSupported = isResumeDownloadSupported(response)

        val newFile = createFileOrThrow(
            filename,
            downloadDir.absolutePath,
            previousSize <= 0 || !isResumeSupported // пересоздание файла
        )

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", newFile)

        downloadNotifier?.onUriReady(uri)

        try {
            responseBodyToOutputStream(
                response,
                newFile.openOutputStreamOrThrow(),
                previousSize,
                downloadNotifier
            )
        } catch (e: RuntimeException) {
            val resultException = HttpProtocolException.RawBuilder(response, e).build()
            if (deleteUnfinishedFile.shouldDelete(isResumeSupported)) {
                try {
                    deleteFileOrThrow(newFile)
                } catch (e: RuntimeException) {
                    throw RuntimeException("Cannot delete unfinished file $newFile", resultException)
                }
            }
            throw resultException
        }
        scanFile(context, newFile)
        return uri
    }

    private fun addRangeHeader(request: Request.Builder, size: Long) {
        if (size > 0) {
            request.addHeader(HEADER_RANGE, "bytes=$size-")
        }
    }

    @Throws(RuntimeException::class)
    private fun checkResponseOrThrow(response: Response) {
        if (!response.isSuccessful) {
            throw HttpProtocolException.RawBuilder(response, null).build()
        }
    }

    enum class DeleteUnfinishedMode {
        AUTO, ENABLED, DISABLED;

        fun shouldDelete(isResumeSupported: Boolean) =
            if (this == AUTO) !isResumeSupported else this == ENABLED
    }

    interface IDownloadNotifier: IStreamNotifier {

        fun onUriReady(uri: Uri)
    }
}
