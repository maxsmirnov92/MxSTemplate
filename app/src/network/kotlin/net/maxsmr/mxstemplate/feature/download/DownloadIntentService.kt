package net.maxsmr.mxstemplate.feature.download

import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.MainThread
import com.google.gson.annotations.SerializedName
import dagger.android.DaggerIntentService
import net.maxsmr.commonutils.android.getShareIntent
import net.maxsmr.commonutils.android.getViewIntent
import net.maxsmr.commonutils.data.digest
import net.maxsmr.commonutils.data.states.LoadState
import net.maxsmr.commonutils.data.states.Status
import net.maxsmr.commonutils.data.text.EMPTY_STRING
import net.maxsmr.core_network.downloader.NotificationWrapper
import net.maxsmr.core_network.downloader.OkHttpDownloader
import net.maxsmr.core_network.downloader.OkHttpDownloader.DeleteUnfinishedMode
import net.maxsmr.core_network.downloader.OkHttpDownloader.DeleteUnfinishedMode.AUTO
import net.maxsmr.mxstemplate.R
import net.maxsmr.mxstemplate.di.network.DI_NAME_OK_HTTP_DOWNLOADER
import net.maxsmr.mxstemplate.feature.download.info.DownloadInfo
import net.maxsmr.mxstemplate.feature.download.info.IntentSenderParams
import net.maxsmr.networkutils.NetworkHelper
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okio.ByteString
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.Serializable
import javax.inject.Inject
import javax.inject.Named

const val EXTRA_DOWNLOAD_SERVICE_PARAMS = "download_service_params"

private const val LOG_TAG = "DownloadIntentService"

class DownloadIntentService : DaggerIntentService("DownloadIntentService") {

    private val handler = Handler(Looper.getMainLooper())

    @Inject
    @Named(DI_NAME_OK_HTTP_DOWNLOADER)
    lateinit var okHttpClient: OkHttpClient

    @Inject
    lateinit var downloadsHolder: DownloadsHolder

    override fun onHandleIntent(intent: Intent?) {

        val params = intent?.getSerializableExtra(EXTRA_DOWNLOAD_SERVICE_PARAMS) as? FileParams<*>
            ?: return

        val notificationWrapper = NotificationWrapper(this)

        val triple = downloadsHolder.getDownloaded(params.fileName, null) // интересует любой статус по данному имени

        if (params.checkFileExists && triple != null) {
            val currentHash = triple.second
            val targetHash = triple.first.hash
            if (currentHash != null && targetHash != null && currentHash.contentEquals(targetHash)) {
                Log.w(LOG_TAG, "Previous download for fileName {$params.fileName} was found: $triple")
                when (triple.third) {
                    Status.SUCCESS -> {
                        // success, hash'и совпадают -> кидаем ивент, завершаем
                        handler.post { onDownloadSuccess(notificationWrapper, triple.first) }
                        return
                    }
                    else -> {
                        // do nothing
                    }
                }
            }
        }

        val existingUri = triple?.first?.uri
        val currentDownloadInfo = DownloadInfo(params, triple?.first?.downloadId ?: downloadsHolder.nextDownloadId(), existingUri.toString())

        if (params.checkConnection && !NetworkHelper.isOnline(this)) {
            handler.post { onDownloadFailed(notificationWrapper, currentDownloadInfo, RuntimeException("No connection")) }
            return
        }

        handler.post { onDownloadProcessing(notificationWrapper, currentDownloadInfo) }

        val okHttpDownloader = OkHttpDownloader(this, okHttpClient, params.dir)

        var error: RuntimeException? = null

        var contentUri: Uri? = null

        try {
            val mediaStoreErrorHandler = { e: SecurityException ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (e is RecoverableSecurityException) {
                        downloadsHolder.intentSenderSubject.onNext(
                            IntentSenderParams(
                                currentDownloadInfo.downloadId,
                                triple?.first?.uri,
                                params.fileName,
                                e.userAction.actionIntent.intentSender
                            )
                        )
                    }
                }
            }
            val notifyInterval = params.notificationParams?.downloadingNotifyInterval

            contentUri = okHttpDownloader.download(
                params.fileName,
                params.dataType,
                existingUri != null && (triple.third == Status.LOADING || triple.third == Status.ERROR),
                params.deleteUnfinishedFile,
                existingUri,
                requestConfigurator = {
                    it.url(params.url)
                        .method(
                            params.method,
                            params.createRequestBody()
                        )
                    params.headers.forEach { header ->
                        if (header.key.isNotEmpty()) {
                            it.header(header.key, header.value)
                        }
                    }
                    params.url
                },
                deleteHandler = mediaStoreErrorHandler,
                insertUpdateHandler = mediaStoreErrorHandler,
                downloadNotifier = if (notifyInterval != null) {
                    object : OkHttpDownloader.IDownloadNotifier {

                        override val notifyInterval: Long = notifyInterval

                        override fun onUriReady(uri: Uri) {
                            contentUri = uri
                        }

                        override fun onProcessing(
                            inputStream: InputStream,
                            outputStream: OutputStream,
                            bytesWrite: Long,
                            bytesLeft: Long
                        ): Boolean {
                            handler.post {
                                onDownloadProcessing(
                                    notificationWrapper,
                                    currentDownloadInfo,
                                    bytesWrite,
                                    if (bytesLeft > 0) bytesLeft + bytesWrite else 0
                                )
                            }
                            return true
                        }
                    }
                } else {
                    null
                }
            )
            // дополнительное копирование всех загруженных файликов в /Downloads не требуется:
            // уже в правильном месте, в зав-ти от версии (copyToExternal(context, file(data.name), data.mimeType))
        } catch (e: RuntimeException) {
            error = e
        }
        if (error != null || contentUri == null) {
            handler.post { onDownloadFailed(notificationWrapper, currentDownloadInfo, error) }
        } else {
            with(contentUri) {
                handler.post {
                    onDownloadSuccess(
                        notificationWrapper,
                        currentDownloadInfo.copy(
                            uriString = this.toString(),
                            hash = if (this != null) digest(this@DownloadIntentService, this) else null
                        )
                    )
                }
            }
        }
    }

    /**
     * bytesTotal == 0 -> загрузка indeterminate = true
     */
    @MainThread
    private fun onDownloadProcessing(
        wrapper: NotificationWrapper,
        downloadInfo: DownloadInfo,
        bytesDownloaded: Long = 0,
        bytesTotal: Long = 0
    ) {
        Log.i(LOG_TAG, "Starting download $downloadInfo")
        showNotification(
            wrapper,
            downloadInfo,
            wrapper.context.getString(R.string.download_notification_progress_text),
            false,
            bytesDownloaded,
            bytesTotal
        )
        downloadsHolder.nextDownload(LoadState(true))
    }

    @MainThread
    private fun onDownloadSuccess(wrapper: NotificationWrapper, downloadInfo: DownloadInfo) {
        Log.i(LOG_TAG, "Download $downloadInfo success")
        showNotification(wrapper, downloadInfo, wrapper.context.getString(R.string.download_notification_success_text), true)
        downloadsHolder.nextDownload(LoadState(data = downloadInfo))
    }

    @MainThread
    private fun onDownloadFailed(wrapper: NotificationWrapper, downloadInfo: DownloadInfo, e: Exception?) {
        Log.e(LOG_TAG, "Download $downloadInfo failed with error $e")
        showNotification(
            wrapper, downloadInfo,
            if (e != null) {
                wrapper.context.getString(R.string.download_notification_failed_text_format, e.localizedMessage)
            } else {
                wrapper.context.getString(R.string.download_notification_failed_text)
            },
            true
        )
        downloadsHolder.nextDownload(LoadState(error = e))
    }

    @MainThread
    private fun showNotification(
        wrapper: NotificationWrapper,
        downloadInfo: DownloadInfo,
        message: String,
        isFinished: Boolean,
        bytesDownloaded: Long = 0,
        bytesTotal: Long = 0
    ) {
        with(downloadInfo.params.notificationParams) {
            if (this == null) {
                return
            }
            wrapper.showNotification(
                downloadInfo.downloadId,
                channelName,
                notificationConfigurator = {
                    it.setSmallIcon(R.mipmap.ic_launcher)
                    it.setContentTitle(downloadInfo.params.fileName)
                    it.setContentText(message)
                    if (!isFinished) {
                        val max = 100
                        if (bytesDownloaded >= 0 && bytesTotal > 0) {
                            it.setProgress(max, ((bytesDownloaded / bytesTotal.toFloat()) * max).toInt(), false)
                        } else {
                            it.setProgress(max, 0, true)
                        }
                        it.setOngoing(true)
                    } else {
                        it.setOngoing(false)
                    }
                    it.setAutoCancel(false)

                    if (isFinished) {
                        val uri = downloadInfo.uri
                        if (uri != null) {
                            // кнопки показываем при успешной загрузке (нет exception'а, урла есть)
                            if (actions.contains(NotificationParams.NotificationDownloadAction.VIEW)) {
                                var intent = getViewIntent(
                                        uri,
                                        downloadInfo.params.dataType
                                )
                                if (viewChooserTitle.isNotEmpty()) {
                                    intent = Intent.createChooser(intent, viewChooserTitle)
                                }
                                it.addAction(
                                    android.R.drawable.ic_menu_view,
                                    wrapper.context.getString(R.string.download_notification_success_view_button),
                                    PendingIntent.getActivity(
                                        wrapper.context,
                                        downloadsHolder.nextNotificationRequestCode(),
                                        intent,
                                        PendingIntent.FLAG_UPDATE_CURRENT
                                    )
                                )
                            }
                            if (actions.contains(NotificationParams.NotificationDownloadAction.SHARE)) {
                                var intent = getShareIntent(
                                    shareIntentSubject,
                                    shareIntentText,
                                    downloadInfo.params.dataType,
                                        uri,
                                )
                                if (shareChooserTitle.isNotEmpty()) {
                                    intent = Intent.createChooser(intent, shareChooserTitle)
                                }
                                it.addAction(
                                    android.R.drawable.ic_menu_send,
                                    wrapper.context.getString(R.string.download_notification_success_share_button),
                                    PendingIntent.getActivity(
                                        wrapper.context,
                                        downloadsHolder.nextNotificationRequestCode(),
                                        intent,
                                        PendingIntent.FLAG_UPDATE_CURRENT
                                    )
                                )
                            }
                        }
                    }
                })
        }
    }

    open class Params<RequestDataType : Serializable>(
        @SerializedName("url")
        val url: String,
        @SerializedName("method")
        val method: String = "GET",
        @SerializedName("headers")
        val headers: HashMap<String, String> = HashMap(),
        @SerializedName("data")
        val data: RequestDataType? = null,
        @SerializedName("dataType")
        val dataType: String = EMPTY_STRING,
        @SerializedName("checkConnection")
        val checkConnection: Boolean = true,
        @SerializedName("notificationParams")
        val notificationParams: NotificationParams? = NotificationParams()
    ) : Serializable {

        fun createRequestBody(): RequestBody? {
            if (data == null) return null
            val type = MediaType.parse(dataType)
            return when (data) {
                is File -> {
                    RequestBody.create(type, data)
                }
                is ByteArray -> {
                    RequestBody.create(type, data)
                }
                is String -> {
                    RequestBody.create(type, data)
                }
                is ByteString -> {
                    RequestBody.create(type, data)
                }
                else -> {
                    throw IllegalArgumentException("Wrong data type: " + data.javaClass)
                }
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Params<*>) return false

            if (url != other.url) return false
            if (method != other.method) return false
            if (data != other.data) return false
            if (dataType != other.dataType) return false
            if (checkConnection != other.checkConnection) return false
            if (notificationParams != other.notificationParams) return false

            return true
        }

        override fun hashCode(): Int {
            var result = url.hashCode()
            result = 31 * result + method.hashCode()
            result = 31 * result + (data?.hashCode() ?: 0)
            result = 31 * result + dataType.hashCode()
            result = 31 * result + checkConnection.hashCode()
            result = 31 * result + (notificationParams?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String {
            return "Params(url='$url', method='$method', data=$data, dataType='$dataType', checkConnection=$checkConnection, notificationParams=$notificationParams)"
        }
    }

    class FileParams<RequestDataType : Serializable>(
        @SerializedName("fileName")
        val fileName: String,
        @SerializedName("dir")
        val dir: File,
        @SerializedName("checkFileExists")
        val checkFileExists: Boolean = true,
        @SerializedName("deleteUnfinishedFile")
        val deleteUnfinishedFile: DeleteUnfinishedMode = AUTO,
        url: String,
        method: String = "GET",
        headers: HashMap<String, String> = HashMap(),
        data: RequestDataType? = null,
        dataType: String = EMPTY_STRING,
        shouldCheckConnection: Boolean = true,
        notificationParams: NotificationParams? = NotificationParams()
    ) : Params<RequestDataType>(url, method, headers, data, dataType, shouldCheckConnection, notificationParams) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is FileParams<*>) return false
            if (!super.equals(other)) return false

            if (fileName != other.fileName) return false
            if (dir != other.dir) return false
            if (checkFileExists != other.checkFileExists) return false
            if (deleteUnfinishedFile != other.deleteUnfinishedFile) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + fileName.hashCode()
            result = 31 * result + dir.hashCode()
            result = 31 * result + checkFileExists.hashCode()
            result = 31 * result + deleteUnfinishedFile.hashCode()
            return result
        }

        override fun toString(): String {
            return "FileParams(fileName='$fileName', dir=$dir, checkFileExists=$checkFileExists, deleteUnfinishedFile=$deleteUnfinishedFile)"
        }
    }

    /**
     * @param actions флаги показа действий в нотификации
     */
    data class NotificationParams(
        @SerializedName("channelName")
        val channelName: String = EMPTY_STRING,
        @SerializedName("shareIntentSubject")
        val shareIntentSubject: String = EMPTY_STRING,
        @SerializedName("shareIntentText")
        val shareIntentText: String = EMPTY_STRING,
        @SerializedName("shareChooserTitle")
        val shareChooserTitle: String = EMPTY_STRING,
        @SerializedName("viewChooserTitle")
        val viewChooserTitle: String = EMPTY_STRING,
        @SerializedName("actions")
        val actions: HashSet<NotificationDownloadAction> = hashSetOf(),
        @SerializedName("downloadingNotifyInterval")
        val downloadingNotifyInterval: Long = 200
    ) : Serializable {

        enum class NotificationDownloadAction(val id: Int) {
            @SerializedName("VIEW")
            VIEW(1),

            @SerializedName("SHARE")
            SHARE(2)
        }

        override fun toString(): String {
            return "NotificationParams(channelName='$channelName'," +
                    "shareIntentSubject='$shareIntentSubject'," +
                    "shareIntentText='$shareIntentText'," +
                    "shareChooserTitle='$shareChooserTitle'," +
                    "viewChooserTitle='$viewChooserTitle'," +
                    "actions=$actions," +
                    "downloadingNotifyInterval=$downloadingNotifyInterval)"
        }
    }
}