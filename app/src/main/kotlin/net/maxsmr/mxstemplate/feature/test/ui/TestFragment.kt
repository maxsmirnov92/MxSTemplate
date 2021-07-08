package net.maxsmr.mxstemplate.feature.test.ui

import android.Manifest
import android.os.Bundle
import android.view.View
import net.maxsmr.commonutils.gui.actions.message.text.TextMessage
import net.maxsmr.commonutils.gui.fragments.dialogs.AlertTypedDialogFragment
import net.maxsmr.core_common.ui.viewmodel.delegates.VmFactoryParams
import net.maxsmr.core_common.ui.viewmodel.delegates.viewBinding
import net.maxsmr.core_common.ui.viewmodel.delegates.vmFactoryParams
import net.maxsmr.mxstemplate.R
import net.maxsmr.mxstemplate.databinding.FragmentTestBinding
import net.maxsmr.mxstemplate.ui.common.BaseFragment
import javax.inject.Inject

class TestFragment : BaseFragment<TestViewModel>() {

    private val binding by viewBinding(FragmentTestBinding::bind)

    override val vmFactoryParams: VmFactoryParams<TestViewModel> get() = vmFactoryParams(viewModelFactory)

    override val layoutId: Int = R.layout.fragment_test

    @Inject
    lateinit var viewModelFactory: TestViewModel.Factory

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
        viewModel: TestViewModel
    ) {
        onRequestStoragePermissions()
    }

    private fun onRequestStoragePermissions() {
        val perms = listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        doOnPermissionsResult(STORAGE_PERMISSIONS_REQUEST_CODE, perms, onDenied = {
            dialogFragmentsHolder.show(
                DIALOG_TAG_PERMISSIONS_DENIED,
                AlertTypedDialogFragment.Builder()
                    .setMessage(TextMessage(R.string.dialog_message_permission_denied_format, it.toString()))
                    .setButtons(TextMessage(android.R.string.ok))
                    .build(requireContext())
            )
        }) {
            dialogFragmentsHolder.show(
                DIALOG_TAG_PERMISSIONS_GRANTED,
                AlertTypedDialogFragment.Builder()
                    .setMessage(TextMessage(R.string.dialog_message_permission_granted_format, perms.toString()))
                    .setButtons(TextMessage(android.R.string.ok))
                    .build(requireContext())
            )
        }
    }

    companion object {

        private const val STORAGE_PERMISSIONS_REQUEST_CODE = 1

        private const val DIALOG_TAG_PERMISSIONS_GRANTED = "permissions_granted"
        private const val DIALOG_TAG_PERMISSIONS_DENIED = "permissions_denied"

        fun instance() = TestFragment()
    }
}