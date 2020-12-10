package net.maxsmr.mxstemplate.feature.test.ui

import android.Manifest
import android.os.Bundle
import android.view.View
import androidx.lifecycle.SavedStateHandle
import me.ilich.juggler.states.State
import net.maxsmr.commonutils.android.gui.fragments.dialogs.TypedDialogFragment
import net.maxsmr.core_common.arch.StringsProvider
import net.maxsmr.core_common.arch.rx.scheduler.SchedulersProvider
import net.maxsmr.core_common.permissions.PermissionsRationaleHandler
import net.maxsmr.mxstemplate.R
import net.maxsmr.mxstemplate.di.ui.BaseVmFactory
import net.maxsmr.mxstemplate.ui.common.BaseFragment
import net.maxsmr.permissionchecker.checkAndRequestPermissionsStorage
import pub.devrel.easypermissions.AfterPermissionGranted
import javax.inject.Inject

private const val STORAGE_PERMISSIONS_REQUEST_CODE = 1

private const val DIALOG_TAG_PERMISSIONS_GRANTED = "permissions_granted"

class TestFragment : BaseFragment<TestViewModel>() {

    override val viewModelClass: Class<TestViewModel> = TestViewModel::class.java

    override val layoutId: Int = R.layout.fragment_test

    @Inject
    override lateinit var viewModelFactory: Factory

    private lateinit var permissionsRationaleHandler: PermissionsRationaleHandler

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
        viewModel: TestViewModel
    ) {
        permissionsRationaleHandler = PermissionsRationaleHandler(dialogFragmentsHolder, viewLifecycleOwner)
        onRequestStoragePermissions()
    }

    @AfterPermissionGranted(STORAGE_PERMISSIONS_REQUEST_CODE)
    fun onRequestStoragePermissions() {
        val perms = listOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        checkAndRequestPermissionsStorage(this,
            getString(R.string.dialog_message_permission_request_rationale),
            STORAGE_PERMISSIONS_REQUEST_CODE,
            perms,
            rationaleAction = {
                permissionsRationaleHandler.displayRationalePermissionDialog(
                    it.callObj,
                    it.rationale,
                    it.perms,
                    true
                )
            },
            targetAction = {
                dialogFragmentsHolder.show(
                    DIALOG_TAG_PERMISSIONS_GRANTED,
                    TypedDialogFragment.DefaultTypedDialogBuilder()
                        .setMessage(getString(R.string.dialog_message_permission_granted_format, perms))
                        .setButtons(requireContext(), android.R.string.ok, null, null)
                        .build()
                )
            }
        )
    }

    class Factory @Inject constructor(
        private val schedulersProvider: SchedulersProvider,
        private val stringsProvider: StringsProvider
    ) : BaseVmFactory<TestViewModel> {

        override fun create(handle: SavedStateHandle, params: State.Params): TestViewModel {
            return TestViewModel(
                handle,
                schedulersProvider,
                stringsProvider,
                null
            )
        }
    }

    companion object {

        fun instance() = TestFragment()
    }
}