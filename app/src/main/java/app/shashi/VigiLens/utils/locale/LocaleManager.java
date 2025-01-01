package app.shashi.VigiLens.utils.locale;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

public class LocaleManager {

    public static Context updateBaseContextLocale(Context base) {
        SharedPreferences sharedPreferences = base.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String language = sharedPreferences.getString("language", "System Default");

        Locale locale = getLocaleForLanguage(language);
        return updateResources(base, locale);
    }

    private static Locale getLocaleForLanguage(String language) {
        switch (language) {
            case "en": return new Locale("en");
            case "hi": return new Locale("hi");
            case "es": return new Locale("es");
            case "zh": return new Locale("zh");
            case "ar": return new Locale("ar");
            case "pt": return new Locale("pt");
            case "fr": return new Locale("fr");
            case "ru": return new Locale("ru");
            case "de": return new Locale("de");
            case "ja": return new Locale("ja");
            default: return Locale.getDefault();
        }
    }

    private static Context updateResources(Context context, Locale locale) {
        Locale.setDefault(locale);
        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        configuration.setLocales(new android.os.LocaleList(locale));
        return context.createConfigurationContext(configuration);
    }
}
