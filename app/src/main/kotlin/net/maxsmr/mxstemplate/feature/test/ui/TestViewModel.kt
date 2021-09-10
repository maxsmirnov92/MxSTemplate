package net.maxsmr.mxstemplate.feature.test.ui

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.lifecycle.SavedStateHandle
import com.google.android.material.snackbar.Snackbar
import io.reactivex.disposables.Disposables
import io.reactivex.internal.operators.observable.ObservableInterval
import me.ilich.juggler.states.State
import net.maxsmr.commonutils.gui.actions.dialog.DialogBuilderFragmentShowMessageAction
import net.maxsmr.commonutils.gui.actions.message.SnackBuilderMessageAction
import net.maxsmr.commonutils.gui.actions.message.ToastBuilderMessageAction
import net.maxsmr.commonutils.gui.actions.message.ToastMessageAction
import net.maxsmr.commonutils.gui.actions.message.text.TextMessage
import net.maxsmr.commonutils.gui.fragments.dialogs.AlertTypedDialogFragment
import net.maxsmr.commonutils.live.event.VmListEvent
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.logException
import net.maxsmr.core_common.BaseApplication
import net.maxsmr.core_common.arch.ErrorHandler
import net.maxsmr.core_common.arch.StringsProvider
import net.maxsmr.core_common.arch.rx.scheduler.SchedulersProvider
import net.maxsmr.core_common.ui.viewmodel.BaseScreenData
import net.maxsmr.core_common.ui.viewmodel.BaseViewModel
import net.maxsmr.core_common.ui.viewmodel.BaseVmFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// TODO BaseHandleableViewModel

const val DIALOG_TAG_ERROR = "error"
const val DIALOG_TAG_TEST = "test"
const val SNACKBAR_TAG_TYPE_ONE = "one"
const val SNACKBAR_TAG_TYPE_TWO = "two"

private const val TIMER_INTERVAL = 10L

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>(TestViewModel::class.java)

class TestViewModel constructor(
    savedState: SavedStateHandle,
    schedulersProvider: SchedulersProvider,
    stringsProvider: StringsProvider,
    errorHandler: ErrorHandler?
) : BaseViewModel<BaseScreenData>(savedState, schedulersProvider, stringsProvider, errorHandler) {

    private var timerDisposable = Disposables.disposed()

    private var counter = 0

    @SuppressLint("ShowToast")
    override fun onInitialized() {
        super.onInitialized()
        // тост, собранный через билдер с наиболее частыми филдами
        toastMessageCommands.setNewConsumableEvent(ToastBuilderMessageAction(ToastBuilderMessageAction.Builder(message = TextMessage("TEST 1"))))
        // тост, сделанный обычным способом
        toastMessageCommands.setNewConsumableEvent(ToastMessageAction(Toast.makeText(BaseApplication.context, "TEST 2", Toast.LENGTH_SHORT)))
        startTimer()
    }

    private fun startTimer() {
        timerDisposable.dispose()
        timerDisposable = subscribe(ObservableInterval
            .interval(TIMER_INTERVAL, TimeUnit.SECONDS),
            {
                val action: SnackBuilderMessageAction
                val options: VmListEvent.AddOptions
                if (counter % 2 == 0) {
                    action = SnackBuilderMessageAction(
                        SnackBuilderMessageAction.Builder(
                            message = TextMessage("Indefinite Snackbar with button"),
                            action = TextMessage("Dismiss"),
                            duration = Snackbar.LENGTH_INDEFINITE
                        )
                    )
                    options = VmListEvent.AddOptions(
                        SNACKBAR_TAG_TYPE_ONE,
                        VmListEvent.AddOptions.Priority.NORMAL,
                        VmListEvent.UniqueStrategy.Replace
                    )
                } else {
                    action = SnackBuilderMessageAction(
                        SnackBuilderMessageAction.Builder(
                            message = TextMessage("Short Snackbar without button")
                        ),
                    )
                    options = VmListEvent.AddOptions(
                        SNACKBAR_TAG_TYPE_TWO,
                        VmListEvent.AddOptions.Priority.HIGHEST,
                        VmListEvent.UniqueStrategy.Replace
                    )
                    // на каждый второй тик к очереди диалогов добавляется этот
                    // без перепоказа (reshow=false) в следующий раз, если в холдере уже висит такой тэг
                    // НО: в самом DialogFragment возможен показ только одного диалога в данный момент, поэтому checkSingle=true
                    val dialogTag = "${DIALOG_TAG_TEST}_$it"
                    showDialogCommands.setNewConsumableEvent(
                        DialogBuilderFragmentShowMessageAction(
                            dialogTag,
                            AlertTypedDialogFragment.Builder()
                                .setMessage(TextMessage("Very test message ($it)"))
                                .setButtons(TextMessage("OK")),
                            false
                        ),
                        dialogTag
                    )
                }
                // у обоих снеков вывод возможен по одному экземпляру в текущий момент (checkSingle = true)
                snackMessageCommands.setNewEvent(action, options)
                counter++
            }, {
                logException(logger, it)
                showDialogCommands.setNewConsumableEvent(
                    DialogBuilderFragmentShowMessageAction(
                        DIALOG_TAG_ERROR,
                        AlertTypedDialogFragment.Builder()
                            .setMessage(TextMessage("Something went wrong: $it"))
                    ),
                    DIALOG_TAG_ERROR
                )
            })
    }

    class Factory @Inject constructor(
        private val schedulersProvider: SchedulersProvider,
        private val stringsProvider: StringsProvider
    ) : BaseVmFactory<TestViewModel> {

        override fun create(handle: SavedStateHandle, params: State.Params?): TestViewModel {
            return TestViewModel(
                handle,
                schedulersProvider,
                stringsProvider,
                null
            )
        }
    }
}