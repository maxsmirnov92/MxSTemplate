package net.maxsmr.mxstemplate.utils.validation

import net.maxsmr.commonutils.data.conversion.format.parseDate
import net.maxsmr.commonutils.data.text.EMPTY_STRING
import net.maxsmr.mxstemplate.utils.PHONE_RUS_PREFIX_PLUS_SEVEN
import net.maxsmr.mxstemplate.utils.normalizePhoneNumber
import java.text.SimpleDateFormat

const val REG_EX_PHONE_NUMBER_RUS = "^((\\+7|7|8)+([0-9]){10})\$"
const val REG_EX_PHONE_NUMBER_RUS_WITHOUT_PREFIX = "^\\d{10}$"
const val REG_EX_EMAIL = "^[^ ]+@.[^!#\$%&'*+/=?^_`{|}~ -]+\\..[^!#\$%&'*+/=?^_`{|}~0-9 -]{1,6}\$"
const val REG_EX_EMAIL_ALT = "^.+@.+\\..+$"
const val REG_EX_SNILS = "^\\d{11}$"

@JvmOverloads
fun isPhoneNumberRusValid(
        phone: String?,
        normalize: Boolean = true,
        withoutPrefix: Boolean = false
): Boolean {
        val normalized = if (normalize) normalizePhoneNumber(phone, if (withoutPrefix) EMPTY_STRING else PHONE_RUS_PREFIX_PLUS_SEVEN) else phone
        return validate(normalized, if (!withoutPrefix) REG_EX_PHONE_NUMBER_RUS else REG_EX_PHONE_NUMBER_RUS_WITHOUT_PREFIX)
}

/**
 * Проверка валидности формата email {@link ValidationUtilsKt#validate}
 *
 * @return true - если формат соответствует формату [REG_EX_EMAIL]
 */
fun isEmailValid(email: CharSequence?): Boolean =
        validate(email, REG_EX_EMAIL)

/**
 * Проверка валидности формата email (выбор способа доставки чека, RZD-7521) [validate]
 *
 * @param email [String]
 * @return true - если формат соответствует формату [REG_EX_EMAIL_ALT]
 */
fun isEmailValidReceipt(email: String?): Boolean =
        validate(email, REG_EX_EMAIL_ALT)

fun isSnilsValid(snils: String?): Boolean =
        validate(snils, REG_EX_SNILS)

/**
 * Вызывать для проверки строки на соотвествие формату
 *
 * @param target  - проверяемая строка
 * @param pattern - формат
 * @return true, если соответствует формату
 */
fun validate(target: CharSequence?, pattern: String?): Boolean =
        target?.matches((pattern?: EMPTY_STRING).toRegex()) ?: false

fun isDateValid(
        dateText: String,
        pattern: String,
        dateFormatConfigurator: ((SimpleDateFormat) -> Unit)? = null
) = parseDate(dateText, pattern, dateFormatConfigurator) != null