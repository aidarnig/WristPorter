package io.github.aidarnig.wgs.desktop;

import org.junit.jupiter.api.Test;
import java.util.Locale;
import java.util.ResourceBundle;
import static org.junit.jupiter.api.Assertions.*;

class I18nTest {
    @Test void bothBundlesContainIdenticalKeys() {
        String base = "io.github.aidarnig.wgs.desktop.messages";
        ResourceBundle ru = ResourceBundle.getBundle(base, Locale.forLanguageTag("ru"));
        ResourceBundle en = ResourceBundle.getBundle(base, Locale.forLanguageTag("en"));
        assertEquals(ru.keySet(), en.keySet());
        assertTrue(ru.keySet().size() >= 65);
        assertEquals("Подключение", ru.getString("connection.title"));
        assertEquals("Connection", en.getString("connection.title"));
    }
}
