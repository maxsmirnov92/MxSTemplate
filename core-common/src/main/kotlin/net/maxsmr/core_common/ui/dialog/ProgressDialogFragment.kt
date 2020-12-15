package net.maxsmr.core_common.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import net.maxsmr.commonutils.android.gui.fragments.dialogs.*
import net.maxsmr.core_common.R

const val ARG_IS_ALERT = "AlertDialogFragment#ARG_IS_ALERT"
const val ARG_CONTAINER_ID = "AlertDialogFragment#ARG_CONTAINER_ID"

class ProgressDialogFragment : BaseProgressDialogFragment<Dialog>() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val isAlert = args.getBoolean(ARG_IS_ALERT)
        val containerId = args.getInt(ARG_CONTAINER_ID)
        val title = args.getString(ARG_TITLE)
        val message = args.getString(ARG_MESSAGE)
        val customViewId = args.getInt(ARG_CUSTOM_VIEW_RES_ID)
        val cancelable = args.getBoolean(ARG_CANCELABLE)
        customView = LayoutInflater.from(context).inflate(customViewId, null)
        return if (isAlert) {
            createAlertDialog(title, message, customView, cancelable).apply {
                show()
                setupAlertDialog(this, cancelable, containerId)
            }
        } else {
            ProgressDialog(requireContext(), title, customView, cancelable).apply {
                show()
            }
        }
    }

    private fun createAlertDialog(
        title: String?,
        message: String?,
        contentView: View?,
        cancelable: Boolean
    ): AlertDialog {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setCancelable(cancelable)
        builder.setOnCancelListener(null)
        contentView?.let {
            builder.setView(contentView)
        }
        customView?.let {
            builder.setView(it)
        }
        return builder.create()
    }


    // FIXME wrap_content при использовании AlertDialog
    private fun setupAlertDialog(
        dialog: AlertDialog,
        cancelable: Boolean,
        @IdRes containerId: Int
    ) {
        dialog.setCanceledOnTouchOutside(cancelable)
//        dialog.setContentView(layoutRes)
        dialog.window?.let {
//            it.setContentView(layoutRes)
            if (containerId != 0) {
                val dialogContainer = it.findViewById(containerId) as View
                dialogContainer.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        it.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                        dialogContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                })
//                forceLayoutParams(dialogContainer, WRAP_CONTENT, WRAP_CONTENT)
            } else {
                it.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                it.setGravity(Gravity.CENTER)
            }
        }
    }

    /**
     * @param isAlert в виде [AlertDialog] или обычного [Dialog]
     */
    class ProgressDialogBuilder @JvmOverloads constructor(
        private val isAlert: Boolean = false,
        @IdRes
        private val containerId: Int = R.id.progressBar,
        @IdRes
        progressBarId: Int = R.id.progressBar,
        @IdRes
        loadingMessageViewId: Int = 0
    ) : Builder<Dialog, ProgressDialogFragment>(progressBarId, loadingMessageViewId) {

        init {
            styleResId = R.style.ProgressDialogTheme
            backgroundResId = R.drawable.bg_dialog_rounded
            customViewResId = R.layout.dialog_progress_rounded
        }

        override fun createArgs(): Bundle {
            return super.createArgs().apply {
                putBoolean(ARG_IS_ALERT, isAlert)
                if (containerId != 0) {
                    putInt(ARG_CONTAINER_ID, containerId)
                }
            }
        }

        override fun build(): ProgressDialogFragment {
            return instance(createArgs())
        }
    }

    private class ProgressDialog(
        context: Context,
        title: String?,
        contentView: View?,
        cancelable: Boolean
    ) : Dialog(context) {

        init {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setTitle(title)
            setCancelable(cancelable)
            setCanceledOnTouchOutside(cancelable)
            setOnCancelListener(null)
            contentView?.let {
                setContentView(it)
            }
        }
    }

    companion object {

        private fun instance(args: Bundle): ProgressDialogFragment = ProgressDialogFragment().apply {
            arguments = args
        }
    }
}