package net.maxsmr.core_network

import android.net.Uri
import net.maxsmr.commonutils.data.text.EMPTY_STRING

interface HostManager {

    fun useHttps(): Boolean

    fun getHost(): String

    fun getPort(): String

    fun getBaseUrl(): String = String.format("%s://%s:%s",
            if (useHttps()) "https" else "http", getHost(), getPort())

    fun useLegacyApi(): Boolean = false

    fun getUrl(): String {
        val url = Uri.Builder()
        url.scheme(if (useHttps()) "https" else "http")
        with(getPort()) {
            url.encodedAuthority(getHost() + ":" + (if (this.isNotEmpty()) this else EMPTY_STRING))
        }
        return url.toString()
    }
}