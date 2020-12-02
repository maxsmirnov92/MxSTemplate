package net.maxsmr.mxstemplate.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import me.ilich.juggler.states.State
import net.maxsmr.mxstemplate.feature.test.ui.TestState
import net.maxsmr.mxstemplate.ui.common.BaseActivity

class MainActivity: BaseActivity() {

    override fun createState(): State<*> {
        return TestState()
    }

    companion object {

        fun getIntent(context: Context, targetState: State<*>?): Intent {
            val intent = Intent(context, MainActivity::class.java)
            return addState(intent, targetState)
        }

        fun startWithState(
            context: Context,
            targetState: State<*>?,
            flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK,
            requestCode: Int? = null
        ) {
            getIntent(context, targetState).apply {
                this.flags = flags
                if (context is Activity && requestCode != null) {
                    context.startActivityForResult(this, requestCode)
                } else {
                    context.startActivity(this)
                }
            }
        }

    }
}