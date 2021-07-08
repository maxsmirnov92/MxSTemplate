package net.maxsmr.mxstemplate.ui.common.permissions

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import net.maxsmr.commonutils.getAppSettingsIntent
import net.maxsmr.commonutils.gui.actions.message.text.TextMessage
import net.maxsmr.commonutils.gui.fragments.dialogs.AlertTypedDialogFragment
import net.maxsmr.commonutils.gui.fragments.dialogs.BaseTypedDialogFragment
import net.maxsmr.commonutils.gui.fragments.dialogs.holder.DialogFragmentsHolder
import net.maxsmr.core_common.R
import net.maxsmr.mxstemplate.ui.common.BaseActivity
import net.maxsmr.permissionchecker.BaseDeniedPermissionsHandler
import net.maxsmr.permissionchecker.PermissionsCallbacks
import java.util.*

private const val DIALOG_TAG_RATIONALE = "rationale"

open class DialogHolderDeniedPermissionsHandler(
    private val dialogFragmentsHolder: DialogFragmentsHolder,
    private val activity: BaseActivity
) : BaseDeniedPermissionsHandler() {

    private var lastRequestCode: Int? = null
    private var lastDeniedPermissions: PermissionsCallbacks.DeniedPermissions? = null
    private var lastNegativeAction: ((Set<String>) -> Unit)? = null

    init {
        dialogFragmentsHolder.buttonClickLiveEvents(
            DIALOG_TAG_RATIONALE,
            BaseTypedDialogFragment::class.java,
            null,
            listOf(DialogInterface.BUTTON_POSITIVE, DialogInterface.BUTTON_NEGATIVE)
        ).subscribe(activity) {
            when (it.value) {
                DialogInterface.BUTTON_POSITIVE -> {
                    onPositiveClick()

                }
                DialogInterface.BUTTON_NEGATIVE -> {
                    onNegativeClick()
                }
            }
        }
    }

    override fun formatDeniedPermissionsMessage(deniedPerms: PermissionsCallbacks.DeniedPermissions): String = Companion.formatDeniedPermissionsMessage(activity, deniedPerms.allDenied)

    override fun doShowMessage(
        requestCode: Int,
        message: String,
        deniedPerms: PermissionsCallbacks.DeniedPermissions,
        negativeAction: ((Set<String>) -> Unit)?
    ) {
        lastRequestCode = requestCode
        lastDeniedPermissions = deniedPerms
        lastNegativeAction = negativeAction
        dialogFragmentsHolder.show(
            DIALOG_TAG_RATIONALE,
            AlertTypedDialogFragment.Builder()
                .setMessage(TextMessage(message))
                .setButtons(TextMessage(android.R.string.yes), null, TextMessage(android.R.string.no))
                .build(activity),
            true
        )
    }

    open fun onPositiveClick() {
        lastRequestCode?.let {
            activity.startActivityForResult(getAppSettingsIntent(activity), it)
        }
    }

    open fun onNegativeClick() {
        lastNegativeAction?.let {
            lastDeniedPermissions?.let { denied ->
                it(denied.allDenied)
            }
        }
    }

    companion object {

        @JvmStatic
        fun permissionName(context: Context, permission: String): String {
            return when (permission) {
                Manifest.permission.CAMERA ->
                    context.getString(R.string.permission_camera)
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE ->
                    context.getString(R.string.permission_storage)
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION ->
                    context.getString(R.string.permission_location)
                Manifest.permission.RECORD_AUDIO ->
                    context.getString(R.string.permission_microphone)
                Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR ->
                    context.getString(R.string.permission_calendar)
                else -> try {
                    //в этом случае название не совпадает
                    val pm = context.packageManager
                    pm.getPermissionInfo(permission, 0).loadLabel(pm).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    permission
                }
            }
        }

        @JvmStatic
        fun formatDeniedPermissionsMessage(context: Context, perms: Collection<String>): String {
            val sb = StringBuilder()
            for (perm in perms) {
                val permission = permissionName(context, perm)
                if (!sb.toString().contains(permission)) {
                    sb.append(permission).append(", ")
                }
            }
            sb.delete(sb.length - 2, sb.length)
            return context.getString(
                R.string.dialog_message_permission_request_rationale_settings_format,
                sb.toString().toLowerCase(Locale.getDefault())
            )
        }
    }
}