package frb.axeron.manager.data

import frb.axeron.manager.data.PackageControl.AppOpState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageControlTest {

    @Test
    fun keepsPackageAndUidModesApart() {
        val output = """
            Uid mode: COARSE_LOCATION: ignore
            Uid mode: CAMERA: default
            Uid mode: RUN_ANY_IN_BACKGROUND: allow
            SYSTEM_ALERT_WINDOW: allow; time=+2d3h ago
            WAKE_LOCK: allow; time=+5m ago; duration=+1s
            COARSE_LOCATION: allow; time=+1h ago
            CAMERA: deny
        """.trimIndent()

        val ops = PackageControl.parseAppOps(output)

        assertEquals(AppOpState("allow", null), ops["SYSTEM_ALERT_WINDOW"])
        assertEquals(AppOpState("allow", "ignore"), ops["COARSE_LOCATION"])
        assertEquals(AppOpState(null, "allow"), ops["RUN_ANY_IN_BACKGROUND"])
        assertNull(ops["MANAGE_EXTERNAL_STORAGE"])
    }

    @Test
    fun uidModeWinsUnlessDefault() {
        val overridden = AppOpState(packageMode = "deny", uidMode = "allow")
        assertTrue(overridden.uidOverrides)
        assertEquals("allow", overridden.effective) // why a package "deny" can look ignored

        val notOverridden = AppOpState(packageMode = "deny", uidMode = "default")
        assertFalse(notOverridden.uidOverrides)
        assertEquals("deny", notOverridden.effective)

        assertEquals("allow", AppOpState(null, "allow").effective)
        assertNull(AppOpState(null, null).effective)
    }

    @Test
    fun readsMiuiOpsByNumber() {
        val output = """
            MIUIOP(10008): allow; time=+3m ago
            Uid mode: MIUIOP(10021): ignore
            MIUIOP(10021): allow
            WAKE_LOCK: allow
        """.trimIndent()

        val ops = PackageControl.parseAppOps(output)

        assertEquals(AppOpState("allow", null), ops["10008"])
        assertEquals(AppOpState("allow", "ignore"), ops["10021"])
        assertNull(ops["MIUIOP(10008)"])
        assertEquals(AppOpState("allow", null), ops["WAKE_LOCK"])
    }

    @Test
    fun ignoresLinesThatAreNotOps() {
        val ops = PackageControl.parseAppOps("No operations.\n\nError: unknown package")
        assertEquals(emptyMap<String, AppOpState>(), ops)
    }
}
