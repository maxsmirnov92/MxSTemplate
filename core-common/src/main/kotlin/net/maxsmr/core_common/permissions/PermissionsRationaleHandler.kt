package net.maxsmr.core_common.permissions

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.LifecycleOwner
import net.maxsmr.commonutils.android.asActivityOrThrow
import net.maxsmr.commonutils.android.asContextOrThrow
import net.maxsmr.commonutils.android.getAppSettingsIntent
import net.maxsmr.commonutils.android.gui.fragments.dialogs.TypedDialogFragment
import net.maxsmr.commonutils.android.gui.fragments.dialogs.holder.DialogFragmentsHolder
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.formatException
import net.maxsmr.core_common.R
import net.maxsmr.permissionchecker.filterRationalePermissions
import java.util.*

private const val DIALOG_TAG_RATIONALE = "rationale"

open class PermissionsRationaleHandler(
    private val dialogFragmentsHolder: DialogFragmentsHolder,
    private val viewLifecycleOwner: LifecycleOwner
) {

    private val logger = BaseLoggerHolder.getInstance().getLogger<BaseLogger>(PermissionsRationaleHandler::class.java)

    protected var lastCallObj: Any? = null
    protected var lastFinishOnReject: Boolean = false
    protected var lastNegativeAction: (() -> Unit)? = null

    init {
        dialogFragmentsHolder.buttonClickLiveEvents(
            DIALOG_TAG_RATIONALE,
            TypedDialogFragment::class.java,
            null,
            listOf(DialogInterface.BUTTON_POSITIVE, DialogInterface.BUTTON_NEGATIVE)
        ).subscribe(viewLifecycleOwner) {
            when (it.value) {
                DialogInterface.BUTTON_POSITIVE -> {
                    onRationalePositiveClick()
                }
                DialogInterface.BUTTON_NEGATIVE -> {
                    onRationaleNegativeClick()
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    fun displayRationalePermissionDialog(
        callObj: Any,
        rationale: String,
        perms: List<String>,
        finishOnReject: Boolean = false,
        negativeAction: (() -> Unit)? = null,
    ) {
        lastCallObj = callObj
        lastFinishOnReject = finishOnReject
        lastNegativeAction = negativeAction

        val context = callObj.asContextOrThrow()

        val rationalePermissions = filterRationalePermissions(context, perms.toList())
        val message = if (rationalePermissions.isEmpty()) {
            rationale
        } else {
            formatPermissionsRationale(context, rationalePermissions)
        }
        dialogFragmentsHolder.show(
            DIALOG_TAG_RATIONALE,
            TypedDialogFragment.DefaultTypedDialogBuilder()
                .setMessage(message)
                .setButtons(context, android.R.string.yes, null, android.R.string.no)
                .build(),
            true
        )
    }

    protected open fun onRationalePositiveClick() {
        val context = viewLifecycleOwner.asContextOrThrow()
        context.startActivity(getAppSettingsIntent(context))
    }

    protected open fun onRationaleNegativeClick() {
        lastCallObj?.let {
            lastNegativeAction?.invoke()
            if (lastFinishOnReject) {
                finish(it)
            }
        }
    }

    protected open fun finish(callObj: Any) {
        callObj.asActivityOrThrow().finish()
    }

    private fun formatPermissionsRationale(context: Context, perms: List<String>): String {
        val sb = StringBuilder()
        for (perm in perms) {
            var permission: String
            permission = when (perm) {
                Manifest.permission.CAMERA -> context.getString(R.string.permission_camera)
                Manifest.permission.WRITE_EXTERNAL_STORAGE -> context.getString(R.string.permission_storage)
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION -> context.getString(
                    R.string.permission_location
                )
                Manifest.permission.RECORD_AUDIO -> context.getString(R.string.permission_microphone)
                Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR -> context.getString(
                    R.string.permission_calendar
                )
                else -> try {
                    //в этом случае название не совпадает
                    val pm = context.packageManager
                    pm.getPermissionInfo(perm, 0).loadLabel(pm).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    logger.e(formatException(e, "getPermissionInfo"))
                    perm
                }
            }
            if (!sb.toString().contains(permission)) {
                sb.append(permission).append(", ")
            }
        }
        if (sb.length > 2) {
            sb.delete(sb.length - 2, sb.length)
        }
        return context.getString(
            R.string.dialog_message_permission_request_rationale_settings_format,
            sb.toString().toLowerCase(Locale.getDefault())
        )
    }
}