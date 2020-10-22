package net.maxsmr.mxstemplate.api

import net.maxsmr.commonutils.data.text.EMPTY_STRING
import net.maxsmr.core_network.HostManager

class HostManagerImpl: HostManager {

    override fun useHttps(): Boolean = true // BuildConfig.HTTPS

    override fun getHost(): String = EMPTY_STRING // BuildConfig.HOST

    override fun getPort(): String = EMPTY_STRING // BuildConfig.PORT
}