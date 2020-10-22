package net.maxsmr.mxstemplate.feature.test

import me.ilich.juggler.gui.JugglerFragment
import me.ilich.juggler.states.ContentBelowToolbarState
import me.ilich.juggler.states.VoidParams
import net.maxsmr.jugglerhelper.fragments.toolbar.CommonToolbarFragment

class TestState : ContentBelowToolbarState<VoidParams>(VoidParams.instance()) {

    override fun onConvertContent(params: VoidParams?, fragment: JugglerFragment?) = TestFragment.instance()

    override fun onConvertToolbar(
        params: VoidParams?,
        fragment: JugglerFragment?
    ): JugglerFragment  = CommonToolbarFragment.newInstance()
}