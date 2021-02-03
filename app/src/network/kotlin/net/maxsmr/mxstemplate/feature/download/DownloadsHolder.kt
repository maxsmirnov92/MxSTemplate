package net.maxsmr.mxstemplate.feature.download

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import net.maxsmr.commonutils.prefs.SharedPrefsHolder
import net.maxsmr.commonutils.digest
import net.maxsmr.commonutils.fromJsonObjectString
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.commonutils.states.Status
import net.maxsmr.commonutils.rx.live.toLive
import net.maxsmr.commonutils.toJsonString
import net.maxsmr.mxstemplate.feature.download.info.DownloadInfo
import net.maxsmr.mxstemplate.feature.download.info.DownloadInfoTypeAdapter
import net.maxsmr.mxstemplate.feature.download.info.IntentSenderParams
import net.maxsmr.tasksutils.storage.ids.IdHolder

private const val PREF_KEY_LAST_DOWNLOAD_ID = "last_download_id"
private const val PREF_KEY_LAST_NOTIFICATION_REQUEST_CODE_ID = "last_notification_request_code_id"
private const val PREF_KEY_DOWNLOADS_INFO = "downloads_info"

class DownloadsHolder(
    private val context: Context,
    sharedPrefsName: String
) {

    private val gsonBuilder = GsonBuilder()
        .serializeNulls()
        .setLenient()

    private val gson = gsonBuilder
        .registerTypeAdapter(object : TypeToken<Map<DownloadInfo, Status>>() {}.type, DownloadInfoTypeAdapter(gsonBuilder.create()))
        .create()

    private val sp = if (sharedPrefsName.isNotEmpty()) context.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE) else null

    private val downloadIdHolder = IdHolder(sp?.getInt(PREF_KEY_LAST_DOWNLOAD_ID, 0) ?: 0)

    private val notificationRequestCodeHolder = IdHolder(sp?.getInt(PREF_KEY_LAST_NOTIFICATION_REQUEST_CODE_ID, 0) ?: 0)

    private val infoMap = mutableMapOf<DownloadInfo, Status>()

    private val eventSubject: PublishSubject<LoadState<DownloadInfo>> = PublishSubject.create()

    private val eventObservable: Observable<LoadState<DownloadInfo>> = eventSubject.hide()

    val intentSenderSubject: PublishSubject<IntentSenderParams> = PublishSubject.create()

    private val intentSenderObservable: Observable<IntentSenderParams> = intentSenderSubject.hide()

    init {
        sp?.let {
            val prefValue = SharedPrefsHolder.getValue<String>(sp, PREF_KEY_DOWNLOADS_INFO, SharedPrefsHolder.PrefType.STRING)
            val map = fromJsonObjectString<Map<DownloadInfo, Status>?>(
                gson,
                prefValue,
                object : TypeToken<Map<DownloadInfo, Status>>() {}.type
            )
            map?.let {
                infoMap.putAll(it)
            }
        }
    }

    fun nextDownloadId(): Int {
        val id = downloadIdHolder.incrementAndGet()
        sp?.let {
            SharedPrefsHolder.setValue(sp, PREF_KEY_LAST_DOWNLOAD_ID, id)
        }
        return id
    }

    fun nextNotificationRequestCode(): Int {
        val id = notificationRequestCodeHolder.incrementAndGet()
        sp?.let {
            SharedPrefsHolder.setValue(sp, PREF_KEY_LAST_NOTIFICATION_REQUEST_CODE_ID, id)
        }
        return id
    }

    fun nextDownload(loadState: LoadState<DownloadInfo>, notifySubject: Boolean = true) {
        loadState.data?.let { data ->
            synchronized(infoMap) {
                // сначала убираем всю предыдущую инфу по этому имени и id (для перестраховки)
                filterDownloadInfoByPredicate {
                    it.key.downloadId == data.downloadId
                            || it.key.params.fileName == data.params.fileName
                }.forEach { previous ->
                    infoMap.remove(previous.first)
                }
                infoMap[data] = loadState.getStatus()
            }
            sp?.let {
                SharedPrefsHolder.setValue(
                    sp,
                    PREF_KEY_DOWNLOADS_INFO,
                    toJsonString(gson, infoMap, object : TypeToken<Map<DownloadInfo, Status>>() {}.type)
                )
            }
        }
        if (notifySubject) {
            eventSubject.onNext(loadState)
        }
    }

    fun eventLiveObservable(downloadIds: Collection<Int>) = eventObservable.toLive({
        downloadIds.contains(it.data?.downloadId)
    })

    fun intentSenderLiveObservable(downloadIds: Collection<Int>) = intentSenderObservable.toLive({
        downloadIds.contains(it.downloadId)
    })

    fun findDownloadInfoById(id: Int): Pair<DownloadInfo, Status>? = filterDownloadInfoById(id).getOrNull(0)

    fun filterDownloadInfoById(id: Int): List<Pair<DownloadInfo, Status>> =
        filterDownloadInfoByPredicate { it.key.downloadId == id }

    fun findDownloadInfoByName(fileName: String): Pair<DownloadInfo, Status>? = filterDownloadInfoByName(fileName).getOrNull(0)

    fun filterDownloadInfoByName(fileName: String): List<Pair<DownloadInfo, Status>> =
        filterDownloadInfoByPredicate { it.key.params.fileName == fileName }

    fun findDownloadInfoByPredicate(predicate: (Map.Entry<DownloadInfo, Status>) -> Boolean): Pair<DownloadInfo, Status>? =
        filterDownloadInfoByPredicate(predicate).getOrNull(0)

    fun filterDownloadInfoByPredicate(predicate: (Map.Entry<DownloadInfo, Status>) -> Boolean): List<Pair<DownloadInfo, Status>> {
        synchronized(infoMap) {
            return infoMap.entries.filter {
                predicate(it)
            }.map {
                Pair(it.key, it.value)
            }
        }
    }

    @JvmOverloads
    fun isDownloaded(
        fileName: String,
        status: Status? = Status.SUCCESS,
        checkHash: Boolean = false
    ) = getDownloaded(fileName, status, checkHash) != null

    @JvmOverloads
    fun getDownloaded(
        fileName: String,
        status: Status? = Status.SUCCESS,
        checkHash: Boolean = false
    ): Triple<DownloadInfo, ByteArray?, Status>? =
        with(findDownloadInfoByName(fileName)) {
            if (this != null && (status == null || this.second == status)) {
                val uri = this.first.uri
                if (uri != null || !checkHash) {
                    val currentHash = if (uri != null) digest(context, uri) else null
                    val targetHash = this.first.hash
                    if (!checkHash || currentHash != null && targetHash != null && currentHash.contentEquals(targetHash)) {
                        return@with Triple(this.first, currentHash, this.second)
                    }
                }
            }
            return@with null
        }
}