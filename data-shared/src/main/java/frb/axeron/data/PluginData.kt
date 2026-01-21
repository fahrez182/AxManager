package frb.axeron.shared

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PluginInstaller(
    val uri: Uri,
    var autoEnable: Boolean = true
): Parcelable

class ModuleProp(json: String = "") : ParcelableMapJson(json) {
    val id: String by field("id", "")
    val name: String by field("name", "Unknown")
    val author: String by field("author", "Unknown")
    val version: String by field("version", "Unknown")
    val versionCode: Long by field("versionCode", 0L)
    val description: String by field("description", "")
    val banner: String by field("banner", "")
    val updateJson: String by field("updateJson", "")
    val axeronPlugin: Long by field("axeronPlugin", 0L)

    companion object CREATOR : Parcelable.Creator<ModuleProp> {
        override fun createFromParcel(parcel: Parcel): ModuleProp {
            val json = parcel.readString() ?: ""
            return ModuleProp(json)
        }

        override fun newArray(size: Int): Array<ModuleProp?> = arrayOfNulls(size)
    }
}


class PluginInfo(
    json: String = ""
) : ParcelableMapJson(json) {
    val prop: ModuleProp by field("prop", ModuleProp(), ModuleProp::class.java)
    val update: Boolean by field("update", false)
    val updateInstall: Boolean by field("update_install", false)
    val updateRemove: Boolean by field("update_remove", false)
    val updateEnable: Boolean by field("update_enable", false)
    val updateDisable: Boolean by field("update_disable", false)
    val enabled: Boolean by field("enabled", false)
    val remove: Boolean by field("remove", false)
    val hasActionScript: Boolean by field("action", false)
    val hasWebUi: Boolean by field("web", false)
    val size: Long by field("size", 0L)
    val dirId: String by field("dir_id", "")

    companion object CREATOR : Parcelable.Creator<PluginInfo> {
        override fun createFromParcel(parcel: Parcel): PluginInfo {
            val json = parcel.readString() ?: ""
            return PluginInfo(json)
        }

        override fun newArray(size: Int): Array<PluginInfo?> = arrayOfNulls(size)
    }
}