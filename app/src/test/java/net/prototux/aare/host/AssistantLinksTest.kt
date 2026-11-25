package net.prototux.aare.host

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantLinksTest {
    @Test
    fun navigateTakesTheQueryParameter() {
        assertEquals(AssistantLink.Navigate("132 rue de Rivoli"), AssistantLinks.parse("dashgrid://navigate?q=132%20rue%20de%20Rivoli"))
        assertEquals(AssistantLink.Navigate("the airport"), AssistantLinks.parse("dashgrid://navigate?q=the+airport"))
    }

    @Test
    fun navigateWithoutOrWithABlankDestinationIsRejected() {
        assertNull(AssistantLinks.parse("dashgrid://navigate"))
        assertNull(AssistantLinks.parse("dashgrid://navigate?q="))
        assertNull(AssistantLinks.parse("dashgrid://navigate?q=%20"))
    }

    @Test
    fun playDefaultsToPlayAndAcceptsTheFourTransportActions() {
        assertEquals(AssistantLink.Play("play"), AssistantLinks.parse("dashgrid://play"))
        assertEquals(AssistantLink.Play("pause"), AssistantLinks.parse("dashgrid://play?action=pause"))
        assertEquals(AssistantLink.Play("next"), AssistantLinks.parse("dashgrid://play?action=NEXT"))
        assertEquals(AssistantLink.Play("previous"), AssistantLinks.parse("dashgrid://play?q=previous"))
    }

    @Test
    fun playRejectsAnythingNotOnTheTransportAllowlist() {
        assertNull(AssistantLinks.parse("dashgrid://play?action=shuffle"))
        assertNull(AssistantLinks.parse("dashgrid://play?action=rm%20-rf"))
    }

    @Test
    fun openTakesTheAppOrQParameter() {
        assertEquals(AssistantLink.Open("CoMaps"), AssistantLinks.parse("dashgrid://open?app=CoMaps"))
        assertEquals(AssistantLink.Open("Spotify"), AssistantLinks.parse("dashgrid://open?q=Spotify"))
        assertNull(AssistantLinks.parse("dashgrid://open"))
    }

    @Test
    fun callKeepsOnlyDigitsAndALeadingPlus() {
        assertEquals(AssistantLink.Call("+15551234567"), AssistantLinks.parse("dashgrid://call?n=%2B1%20(555)%20123-4567"))
        assertEquals(AssistantLink.Call("112"), AssistantLinks.parse("dashgrid://call?n=112"))
    }

    @Test
    fun callWithNoDigitsOrACodeIsRejected() {
        assertNull(AssistantLinks.parse("dashgrid://call?n=%23"))
        assertNull(AssistantLinks.parse("dashgrid://call?n="))
        assertNull(AssistantLinks.parse("dashgrid://call"))
    }

    @Test
    fun onlyTheFourKnownHostsAreAllowed() {
        assertNull(AssistantLinks.parse("dashgrid://settings"))
        assertNull(AssistantLinks.parse("dashgrid://run?cmd=rm"))
        assertNull(AssistantLinks.parse("dashgrid://"))
    }

    @Test
    fun anythingNotTheAareSchemeIsRejected() {
        assertNull(AssistantLinks.parse("https://navigate?q=home"))
        assertNull(AssistantLinks.parse("not a uri at all"))
        assertNull(AssistantLinks.parse("javascript:alert(1)"))
    }

    @Test
    fun callerIsTheCallingPackageFirstThenTheSystemReferrer() {
        assertEquals("app.lapin", AssistantLinks.callerPackage("app.lapin", "android-app://other.app", callerSuppliedReferrer = false))
        assertEquals("app.lapin", AssistantLinks.callerPackage(null, "android-app://app.lapin", callerSuppliedReferrer = false))
        assertEquals("com.my_assistant", AssistantLinks.callerPackage(null, "android-app://com.my_assistant/path", callerSuppliedReferrer = false))
    }

    @Test
    fun aReferrerTheCallerSuppliedItselfIsNotTrusted() {
        assertNull(AssistantLinks.callerPackage(null, "android-app://app.lapin", callerSuppliedReferrer = true))
    }

    @Test
    fun aMissingOrNonAppReferrerGivesNoCaller() {
        assertNull(AssistantLinks.callerPackage(null, null, callerSuppliedReferrer = false))
        assertNull(AssistantLinks.callerPackage(null, "https://app.lapin", callerSuppliedReferrer = false))
        assertNull(AssistantLinks.callerPackage(null, "android-app://", callerSuppliedReferrer = false))
        assertNull(AssistantLinks.callerPackage("", null, callerSuppliedReferrer = false))
    }

    @Test
    fun onlyTheChosenAssistantMayActThroughALink() {
        assertTrue(AssistantLinks.callerAllowed("app.lapin", "app.lapin", OWN))
        assertFalse(AssistantLinks.callerAllowed("com.android.chrome", "app.lapin", OWN))
        assertFalse(AssistantLinks.callerAllowed(null, "app.lapin", OWN))
        assertFalse(AssistantLinks.callerAllowed("app.lapin", null, OWN))
        assertFalse(AssistantLinks.callerAllowed(null, null, OWN))
    }

    @Test
    fun aareItselfNeverCountsAsTheAssistant() {
        // Its browser would otherwise relay a web page's dashgrid:// link.
        assertFalse(AssistantLinks.callerAllowed(OWN, OWN, OWN))
        assertFalse(AssistantLinks.callerAllowed(OWN, "app.lapin", OWN))
    }

    private companion object {
        const val OWN = "net.prototux.aare"
    }
}
