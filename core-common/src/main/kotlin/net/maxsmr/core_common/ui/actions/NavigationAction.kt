package net.maxsmr.core_common.ui.actions

import me.ilich.juggler.Navigable
import me.ilich.juggler.change.Add
import me.ilich.juggler.change.Remove
import net.maxsmr.commonutils.gui.actions.BaseViewModelAction

data class NavigationAction(
        val remove: Remove.Interface?,
        val add: Add.Interface?
): BaseViewModelAction<Navigable>() {

    override fun doAction(actor: Navigable) {
        if (remove != null && add != null) {
            actor.state(remove, add)
        } else if (remove != null) {
            actor.state(remove)
        } else {
            actor.state(add)
        }
        super.doAction(actor)
    }
}