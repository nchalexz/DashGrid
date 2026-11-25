package net.prototux.aare.ui.car

import org.junit.Assert.assertEquals
import org.junit.Test

/** Which app-grid entries step aside when the same app's phone version is enabled on the car screen. */
class StandaloneReplacedTest {
    private fun entry(id: String, pkg: String, kind: CarAppKind) = CarAppEntry(id, pkg, id, null, kind)

    @Test
    fun theCarAppGoesAndThePhoneAppStays() {
        val entries = listOf(
            entry("ru.yandex.yandexnavi", "ru.yandex.yandexnavi", CarAppKind.NAVIGATION),
            entry("standalone:ru.yandex.yandexnavi", "ru.yandex.yandexnavi", CarAppKind.STANDALONE),
        )
        assertEquals(setOf("ru.yandex.yandexnavi"), replacedByStandalone(entries, hide = true))
    }

    @Test
    fun mediaAndMessagingEntriesAreReplacedToo() {
        val entries = listOf(
            entry("media:app", "com.example.app", CarAppKind.MEDIA),
            entry("messaging:app", "com.example.app", CarAppKind.MESSAGING),
            entry("app", "com.example.app", CarAppKind.STANDALONE),
        )
        assertEquals(setOf("media:app", "messaging:app"), replacedByStandalone(entries, hide = true))
    }

    @Test
    fun nothingHappensWithoutThePhoneVersion() {
        val entries = listOf(entry("maps", "com.maps", CarAppKind.TEMPLATED))
        assertEquals(emptySet<String>(), replacedByStandalone(entries, hide = true))
    }

    @Test
    fun onlyTheEnabledPackageIsReplaced() {
        val entries = listOf(
            entry("maps", "com.maps", CarAppKind.TEMPLATED),
            entry("other", "com.other", CarAppKind.TEMPLATED),
            entry("maps-phone", "com.maps", CarAppKind.STANDALONE),
        )
        assertEquals(setOf("maps"), replacedByStandalone(entries, hide = true))
    }

    @Test
    fun offMeansNoReplacement() {
        val entries = listOf(
            entry("maps", "com.maps", CarAppKind.TEMPLATED),
            entry("maps-phone", "com.maps", CarAppKind.STANDALONE),
        )
        assertEquals(emptySet<String>(), replacedByStandalone(entries, hide = false))
    }
}
