package io.github.aidarnig.wgs.desktop;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

/** Single source of truth for the persisted EN/RU UI language. */
public final class I18n {
    private static final Preferences PREFS = Preferences.userNodeForPackage(I18n.class);
    private static final String BASE = "io.github.aidarnig.wgs.desktop.messages";
    private static Locale locale = detect();
    private static ResourceBundle bundle = ResourceBundle.getBundle(BASE, locale);

    private I18n() {}

    private static Locale detect() {
        String defaultLanguage = Locale.getDefault().getLanguage().equals("ru") ? "ru" : "en";
        return Locale.forLanguageTag(PREFS.get("ui.language", defaultLanguage));
    }

    public static String language() { return locale.getLanguage(); }

    public static void setLanguage(String language) {
        if (!language.equals("ru") && !language.equals("en")) {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }
        locale = Locale.forLanguageTag(language);
        bundle = ResourceBundle.getBundle(BASE, locale);
        PREFS.put("ui.language", language);
    }

    public static String t(String key, Object... args) {
        String pattern = bundle.getString(key);
        return args.length == 0 ? pattern : new MessageFormat(pattern, locale).format(args);
    }
}
