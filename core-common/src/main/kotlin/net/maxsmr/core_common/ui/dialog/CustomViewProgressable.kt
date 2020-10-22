package net.maxsmr.core_common.ui.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewTreeObserver.*
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import net.maxsmr.jugglerhelper.progressable.Progressable

/**
 * Реализация Progressable, отображающая [layoutRes] в диалоге поверх контента.
 * @param isAlert в виде [AlertDialog] или обычного [Dialog]
 */
class CustomViewProgressable @JvmOverloads constructor(
    private val context: Context,
    @LayoutRes private val layoutRes: Int,
    @IdRes private val containerId: Int = 0,
    private val cancelable: Boolean = false,
    private val isAlert: Boolean = false
) : Progressable {

    private val handler = Handler(Looper.getMainLooper())

    private var progressDialog: Dialog? = null

    override fun onStart() {
        handler.post {
            try {
                // не перебивает текущий
                if (progressDialog == null) {
                    progressDialog = if (isAlert) {
                        createAlertDialog().apply {
                            show()
                            setupAlertDialog(this)
                        }
                    } else {
                        ProgressDialog(context, layoutRes, cancelable).apply {
                            show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onStop() {
        handler.post {
            try {
                progressDialog?.dismiss()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            progressDialog = null
        }
    }

    private fun createAlertDialog(): AlertDialog {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(null)
        builder.setCancelable(cancelable)
        builder.setOnCancelListener(null)
        builder.setView(LayoutInflater.from(context).inflate(layoutRes, null))
        return builder.create()
    }

    // FIXME wrap_content при использовании AlertDialog
    private fun setupAlertDialog(dialog: AlertDialog) {
        dialog.setCanceledOnTouchOutside(cancelable)
//        dialog.setContentView(layoutRes)
        dialog.window?.let {
//            it.setContentView(layoutRes)
            if (containerId != 0) {
                val dialogContainer = it.findViewById(containerId) as View
                dialogContainer.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        it.setLayout(WRAP_CONTENT, WRAP_CONTENT)
                        dialogContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                })
//                forceLayoutParams(dialogContainer, WRAP_CONTENT, WRAP_CONTENT)
            } else {
                it.setLayout(WRAP_CONTENT, WRAP_CONTENT)
                it.setGravity(Gravity.CENTER)
            }
        }
    }

    @Deprecated("use AlertDialog")
    private class ProgressDialog(
        context: Context,
        layoutRes: Int,
        cancelable: Boolean
    ) : Dialog(context) {

        init {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setTitle(null)
            setCancelable(cancelable)
            setCanceledOnTouchOutside(cancelable)
            setOnCancelListener(null)
            setContentView(LayoutInflater.from(context).inflate(layoutRes, null))
        }
    }
}