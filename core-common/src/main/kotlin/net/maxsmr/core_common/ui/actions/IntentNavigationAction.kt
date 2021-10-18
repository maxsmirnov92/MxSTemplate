package net.maxsmr.core_common.ui.actions

import android.content.Intent
import me.ilich.juggler.gui.JugglerFragment
import net.maxsmr.commonutils.gui.actions.BaseViewModelAction

class IntentNavigationAction(
        val info: IntentNavigationInfo
): BaseViewModelAction<JugglerFragment>() {

    override fun doAction(actor: JugglerFragment) {
        super.doAction(actor)
        info.requestCode?.let {
            actor.startActivityForResult(info.intent, it)
        } ?: actor.startActivity(info.intent)
    }

    data class IntentNavigationInfo(
            val intent: Intent,
            val requestCode: Int? = null
    )
}