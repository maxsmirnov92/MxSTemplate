package net.maxsmr.core_network.model.request.log.service.params

import android.content.Intent

interface LogSendParams {

    fun toIntent(): Intent
}