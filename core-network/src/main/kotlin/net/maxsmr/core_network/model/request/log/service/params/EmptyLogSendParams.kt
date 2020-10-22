package net.maxsmr.core_network.model.request.log.service.params

import android.content.Intent
import net.maxsmr.core_network.model.request.log.service.params.LogSendParams

class EmptyLogSendParams : LogSendParams {

    override fun toIntent() = Intent()
}