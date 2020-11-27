package net.maxsmr.mxstemplate.feature.download.info

import android.content.IntentSender
import android.net.Uri

data class IntentSenderParams(
    val downloadId: Int,
    val existingUri: Uri?,
    val targetFileName: String,
    val intentSender: IntentSender
)