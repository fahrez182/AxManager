package frb.axeron.manager.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReleaseApkSelectorTest {
    private fun apk(abi: String) = ReleaseApkAsset(
        name = "AxManager_v2.0.1.r442_20100-release-${abi}_2610191329.apk",
        downloadUrl = "https://example.com/${abi}.apk"
    )

    private val assets = listOf(
        apk("universal"),
        apk("armeabi-v7a"),
        apk("x86_64"),
        apk("arm64-v8a")
    )

    @Test
    fun prefersFirstSupportedAbiRegardlessOfReleaseAssetOrder() {
        val selected = selectReleaseApk(assets, listOf("arm64-v8a", "armeabi-v7a"))
        assertEquals("https://example.com/arm64-v8a.apk", selected?.downloadUrl)
        assertEquals(20100, selected?.versionCode)
    }

    @Test
    fun supports32BitAndX86_64Devices() {
        assertEquals("https://example.com/armeabi-v7a.apk",
            selectReleaseApk(assets, listOf("armeabi-v7a"))?.downloadUrl)
        assertEquals("https://example.com/x86_64.apk",
            selectReleaseApk(assets, listOf("x86_64", "x86"))?.downloadUrl)
    }

    @Test
    fun fallsBackToUniversalWhenDeviceAbiIsNotPublished() {
        assertEquals("https://example.com/universal.apk",
            selectReleaseApk(assets, listOf("x86"))?.downloadUrl)
    }

    @Test
    fun doesNotSelectOtherAppsOrUnknownFormats() {
        val unrelated = listOf(
            ReleaseApkAsset("demo-axsh-release.apk", "https://example.com/demo.apk"),
            ReleaseApkAsset("AnotherApp_v2_20100-release-arm64-v8a.apk", "https://example.com/other.apk"),
            ReleaseApkAsset("AxManager_v2_999999999999999999999-release-arm64-v8a.apk", "https://example.com/overflow.apk")
        )
        assertNull(selectReleaseApk(unrelated, listOf("arm64-v8a")))
        assertNull(selectReleaseApk(listOf(apk("arm64-v8a")), listOf("armeabi-v7a")))
    }
}
