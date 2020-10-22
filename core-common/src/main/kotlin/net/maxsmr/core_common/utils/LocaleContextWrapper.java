package net.maxsmr.core_common.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import java.util.Locale;

/**
 * ContextWrapper для использования заданного языка локализации
 */
public class LocaleContextWrapper extends ContextWrapper {

    public LocaleContextWrapper(Context base) {
        super(base);
    }

    /**
     * Использование ContextWrapper - основной рекомендованный способ локализации,
     * начиная с API 24. Использование updateConfiguration в API 25 deprecated.
     * Но есть недостаток: нельзя повторно сделать attachBaseContext для Application.
     * Соответственно, все строки, зависящие от Application, до перезапуска
     * приложения останутся на прежнем языке.
     */
    @TargetApi(Build.VERSION_CODES.N)
    public static ContextWrapper wrap(Context context, Locale newLocale) {
        context = context.getApplicationContext();
        Resources res = context.getResources();

        Configuration configuration = new Configuration(res.getConfiguration());
        Locale.setDefault(newLocale);
        configuration.uiMode &= ~Configuration.UI_MODE_NIGHT_MASK;
        configuration.setLocale(newLocale);

        res.updateConfiguration(configuration, res.getDisplayMetrics());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context = context.createConfigurationContext(configuration);
        }
        return new ContextWrapper(context);
    }


    /**
     * Костыль для старых версий API, которые используют updateConfiguration
     */
    public static void updateConfiguration(Context context, Locale newLocale) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Resources res = context.getResources();
            Configuration configuration = res.getConfiguration();
            configuration.locale = newLocale;
            res.updateConfiguration(configuration, res.getDisplayMetrics());
        }
    }
}
