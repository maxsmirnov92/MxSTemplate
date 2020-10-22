package net.maxsmr.core_common.ui.viewmodel

import androidx.lifecycle.ViewModel
import java.io.Serializable

/**
 * Маркер, описывающий данные конкретной [ViewModel],
 * для сохранения/восстановления её полей
 */
interface BaseScreenData: Serializable