package net.maxsmr.core_common.ui.dialog

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import net.maxsmr.commonutils.android.gui.fragments.dialogs.TypedDialogFragment
import net.maxsmr.core_common.R

class ProgressDialogFragment : TypedDialogFragment<AlertDialog>() {

    private class ProgressDialogBuilder(context: Context) : Builder<ProgressDialogFragment>(context) {

        override fun build(): ProgressDialogFragment {
            return instance(
                createArgs()
            )
        }
    }

    companion object {

        @JvmStatic
        fun instance(context: Context, isCancelable: Boolean): ProgressDialogFragment =
                ProgressDialogBuilder(context)
                    .setStyleResId(R.style.ProgressDialogTheme)
                    .setBackgroundResId(R.drawable.bg_dialog_rounded)
                    .setCustomView(R.layout.dialog_progress_rounded)
                    .setCancelable(isCancelable)
                    .build() as ProgressDialogFragment

        private fun instance(args: Bundle): ProgressDialogFragment {
            val fragment =
                ProgressDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
}