package net.maxsmr.mxstemplate.utils

import android.telephony.PhoneNumberUtils
import android.text.Editable
import android.widget.EditText
import android.widget.TextView
import net.maxsmr.commonutils.format.*
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.mxstemplate.utils.validation.isPhoneNumberRusValid
import ru.tinkoff.decoro.MaskImpl
import ru.tinkoff.decoro.slots.PredefinedSlots
import ru.tinkoff.decoro.watchers.MaskFormatWatcher

const val PHONE_RUS_PREFIX_SEVEN_ = "7"
const val PHONE_RUS_PREFIX_PLUS_SEVEN = "+7"
const val PHONE_RUS_PREFIX_EIGHT = "8"
const val PHONE_RUS_PREFIX_NINE = "9"
const val PHONE_RUS_MASK_DEFAULT = "$PHONE_RUS_PREFIX_PLUS_SEVEN (___) ___-__-__"
const val PHONE_RUS_LENGTH_CLEAR = 10
const val PHONE_RUS_LENGTH_SEVEN_OR_EIGHT = PHONE_RUS_LENGTH_CLEAR + 1
const val PHONE_RUS_LENGTH_PLUS_SEVEN = PHONE_RUS_LENGTH_CLEAR + 2

/**
 * @return номер телефона в виде +71234567890
 * удаляя () и 8 в +7
 */
@JvmOverloads
fun normalizePhoneNumber(
    phoneNumber: CharSequence?,
    prefixReplaceWith: String = PHONE_RUS_PREFIX_PLUS_SEVEN,
    checkDigits: Boolean = true
): String {
    if (phoneNumber.isNullOrEmpty()) return EMPTY_STRING
    var prefixReplaceWith = prefixReplaceWith
    var normalized = if (checkDigits) phoneNumber.replace("[^0-9+]".toRegex(), EMPTY_STRING) else phoneNumber.toString()
    if (prefixReplaceWith.isNotEmpty()) {
        val seven = PHONE_RUS_PREFIX_SEVEN_
        val plusSeven = PHONE_RUS_PREFIX_PLUS_SEVEN
        val eight = PHONE_RUS_PREFIX_EIGHT
        val nine = PHONE_RUS_PREFIX_NINE
        val replaceSubstring = when {
            seven != prefixReplaceWith && normalized.startsWith(seven) -> {
                seven
            }
            plusSeven != prefixReplaceWith && normalized.startsWith(plusSeven) -> {
                plusSeven
            }
            eight != prefixReplaceWith && normalized.startsWith(eight) -> {
                eight
            }
            nine != prefixReplaceWith && normalized.startsWith(nine) -> {
                nine
            }
            else -> {
                EMPTY_STRING
            }
        }
        if (replaceSubstring == nine) {
            prefixReplaceWith += nine
        }
        if (replaceSubstring.isNotEmpty()) {
            normalized = normalized.replaceFirst(replaceSubstring, prefixReplaceWith)
        } else if (!normalized.startsWith(prefixReplaceWith)) {
            normalized = "$prefixReplaceWith$normalized"
        }
    }
    if (normalized == prefixReplaceWith) {
        normalized = EMPTY_STRING
    }
    return normalized
}

/**
 * Возвращает номер телефона в виде 71234567890
 * удаляя (), + и 8 превращая в 7
 */
fun normalizePhoneNumberRemovePlus(phoneNumber: CharSequence): String =
    normalizePhoneNumber(phoneNumber).trim('+')

/**
 * @return ожидаемая длина телефона в зав-ти от его префикса
 */
fun getPhoneNumberLengthByPrefix(phoneNumber: CharSequence): Int =
    when {
        phoneNumber.startsWith(PHONE_RUS_PREFIX_PLUS_SEVEN) -> PHONE_RUS_LENGTH_PLUS_SEVEN
        phoneNumber.startsWith(PHONE_RUS_PREFIX_EIGHT) || phoneNumber.startsWith(PHONE_RUS_PREFIX_SEVEN_) -> PHONE_RUS_LENGTH_SEVEN_OR_EIGHT
        else -> PHONE_RUS_LENGTH_CLEAR
    }

@JvmOverloads
fun formatPhoneNumber(
    phoneNumber: String,
    withMask: Boolean = false,
    rangeToMask: IntRange = IntRange(8, 10)
): String {
    var phoneFormatted = normalizePhoneNumber(phoneNumber)

    return if (phoneFormatted.startsWith(PHONE_RUS_PREFIX_PLUS_SEVEN) && phoneFormatted.length == PHONE_RUS_LENGTH_PLUS_SEVEN) {
        if (withMask) {
            phoneFormatted = phoneFormatted.replaceRange(rangeToMask, "*".repeat(rangeToMask.last - rangeToMask.first + 1))
        }
        "${phoneFormatted.substring(0, 2)} " +
                "(${phoneFormatted.substring(2, 5)}) " +
                "${phoneFormatted.substring(5, 8)} " +
                "${phoneFormatted.substring(8, 10)} " +
                phoneFormatted.substring(10, 12)
    } else {
        PhoneNumberUtils.formatNumber(phoneFormatted)
    }
}


/**
 * Может быть использован в afterTextChanges в динамике;
 * в активном [watcher] меняются маски в зав-ти от условий
 */
@JvmOverloads
fun EditText.formatPhone(
    current: Editable?,
    watcher: MaskFormatWatcher,
    mask: MaskImpl = createDefaultPhoneMask()
) {
    // watcher переиспользуется
    current?.let {
        if (current.length == 1 && isPhoneNumberRusValid(current[0].toString())) {
            setPhoneHead(current)
            applyToMask(this.text, mask, watcher)
        } else if (current.isEmpty()) {
            EMPTY_MASK.clear()
            watcher.setMask(EMPTY_MASK)
        } else {
            // do nothing
        }
    }
}

fun EditText.setPhoneHead(editable: Editable) {
    if (editable[0] in hashSetOf('+', '7', '8')) {
        this.text.clear()
        this.append("+7")
    } else {
        val text = "+7${editable[0]}"
        this.text.clear()
        this.append(text)
    }
}

@JvmOverloads
fun TextView.setPhoneFormattedText(
    text: CharSequence,
    isTerminated: Boolean = true,
    hideHardcodedHead: Boolean = false,
    isDigit: Boolean = true,
    prefix: String = EMPTY_STRING,
    installOnAndFill: Boolean = false,
    applyWatcher: Boolean = true,
    isDistinct: Boolean = true
) = setFormattedText(
    text,
    createDefaultPhoneMask(isTerminated, hideHardcodedHead, isDigit),
    prefix,
    installOnAndFill,
    applyWatcher,
    isDistinct
) {
    isPhoneNumberRusValid(it)
}

@JvmOverloads
fun createDefaultPhoneMask(
    isTerminated: Boolean = true,
    hideHardcodedHead: Boolean = false,
    isDigit: Boolean = true
): MaskImpl = if (isDigit) {
    // getUnformattedText не сработает, пользовать normalizePhoneNumber на выходе
    createDigitsMask(PHONE_RUS_MASK_DEFAULT, isTerminated, hideHardcodedHead)
} else {
    // getUnformattedText сработает
    createMask(PredefinedSlots.RUS_PHONE_NUMBER.toList(), isTerminated, hideHardcodedHead)
}

@JvmOverloads
fun createDefaultPhoneWatcher(
    isTerminated: Boolean = true,
    hideHardcodedHead: Boolean = false,
    isDigit: Boolean = true,
    maskConfigurator: ((MaskImpl) -> Unit)? = null
) = createWatcher(createDefaultPhoneMask(isTerminated, hideHardcodedHead, isDigit), maskConfigurator)

/**
 * Выставить [createDefaultPhoneWatcher] + вручную prefix, т.к. hideHardcodedHead = false недостаточно
 */
@JvmOverloads
fun TextView.installDefaultPhoneWatcher(
    isTerminated: Boolean = true,
    hideHardcodedHead: Boolean = false,
    isDigit: Boolean = true,
    maskConfigurator: ((MaskImpl) -> Unit)? = null
): MaskFormatWatcher =
    createDefaultPhoneWatcher(isTerminated, hideHardcodedHead, isDigit, maskConfigurator).apply {
        installOn(this@installDefaultPhoneWatcher)
        if (!hideHardcodedHead) {
            text = "$PHONE_RUS_PREFIX_PLUS_SEVEN ("
        }
    }