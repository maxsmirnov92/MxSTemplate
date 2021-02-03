package net.maxsmr.core_common

import android.annotation.TargetApi
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import java.util.*

/**
 * ContextWrapper для использования заданного языка локализации
 */
object LocaleContextWrapper {

    /**
     * Использование ContextWrapper - основной рекомендованный способ локализации,
     * начиная с API 24. Использование updateConfiguration в API 25 deprecated.
     * Но есть недостаток: нельзя повторно сделать attachBaseContext для Application.
     * Соответственно, все строки, зависящие от Application, до перезапуска
     * приложения останутся на прежнем языке.
     */
    @TargetApi(Build.VERSION_CODES.N)
    @JvmStatic
    fun wrap(context: Context, newLocale: Locale): ContextWrapper {
        Locale.setDefault(newLocale)
        val appContext = context.applicationContext
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            wrapConfigurationLocale(appContext, newLocale)
        } else {
            wrapConfigurationLocaleLegacy(appContext, newLocale)
        }
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    @JvmStatic
    private fun wrapConfigurationLocale(context: Context, locale: Locale): ContextWrapper {
        val configuration = Configuration(context.resources.configuration)
        configuration.uiMode = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()
        configuration.setLocale(locale)
        return ContextWrapper(context.createConfigurationContext(configuration))
    }

    /**
     * Для старых версий API, которые используют updateConfiguration
     */
    @SuppressWarnings("deprecation")
    @JvmStatic
    fun wrapConfigurationLocaleLegacy(context: Context, locale: Locale): ContextWrapper {
        updateConfigurationLocaleLegacy(context, locale)
        return ContextWrapper(context)
    }


    @SuppressWarnings("deprecation")
    @JvmStatic
    fun updateConfigurationLocaleLegacy(context: Context, locale: Locale) {
        val resources = context.resources
        val configuration = resources.configuration
        configuration.locale = locale
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }
}