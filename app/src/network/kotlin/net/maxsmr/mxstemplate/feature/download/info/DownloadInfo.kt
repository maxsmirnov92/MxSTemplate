package net.maxsmr.mxstemplate.feature.download.info

import android.net.Uri
import com.google.gson.annotations.SerializedName
import net.maxsmr.mxstemplate.feature.download.DownloadIntentService
import java.io.Serializable

/**
 * Информация о загрузке, включая исходные [params]
 * @param uriString урла успешно загруженного файла (через FileProvider или ContentResolver)
 * @param hash посчитанная контрольная сумма успешно загруженного файла
 */
data class DownloadInfo(
    @SerializedName("params")
    val params: DownloadIntentService.FileParams<*>,
    @SerializedName("downloadId")
    val downloadId: Int,
    @SerializedName("uri")
    val uriString: String? = null,
    @SerializedName("hash")
    val hash: ByteArray? = null
) : Serializable {

    val uri get() = uriString?.let {
        Uri.parse(it)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DownloadInfo) return false

        if (params != other.params) return false
        if (uriString != other.uriString) return false
        if (hash != null) {
            if (other.hash == null) return false
            if (!hash.contentEquals(other.hash)) return false
        } else if (other.hash != null) return false
        if (downloadId != other.downloadId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = params.hashCode()
        result = 31 * result + (uriString?.hashCode() ?: 0)
        result = 31 * result + (hash?.contentHashCode() ?: 0)
        result = 31 * result + downloadId.hashCode()
        return result
    }
}