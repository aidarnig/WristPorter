package io.github.aidarnig.wristporter.bridge

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellLocalizationTest {
    private fun strings(path: String): Map<String, String> {
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(File(path))
        val nodes = document.getElementsByTagName("string")
        return (0 until nodes.length).associate { index ->
            val node = nodes.item(index)
            node.attributes.getNamedItem("name").nodeValue to node.textContent.trim()
        }
    }

    @Test
    fun englishAndRussianHaveMatchingNonemptyKeys() {
        val english = strings("src/main/res/values/strings.xml")
        val russian = strings("src/main/res/values-ru/strings.xml")
        assertEquals(english.keys, russian.keys)
        assertTrue(english.values.all { it.isNotBlank() })
        assertTrue(russian.values.all { it.isNotBlank() })
        assertEquals("Not connected", english["not_connected"])
        assertEquals("Не подключено", russian["not_connected"])
        assertTrue(english["bluetooth_not_implemented"]!!.contains("not implemented"))
        assertTrue(russian["bluetooth_not_implemented"]!!.contains("не реализован"))
    }

    @Test
    fun manifestRequestsNoSensitivePermissions() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertFalseContainsPermissions(manifest)
        assertTrue(manifest.contains("android.hardware.type.watch"))
    }

    private fun assertFalseContainsPermissions(manifest: String) {
        assertTrue(!manifest.contains("<uses-permission"))
    }
}
