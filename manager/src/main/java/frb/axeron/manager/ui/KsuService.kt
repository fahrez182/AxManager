package frb.axeron.manager.ui.screen // این خط را مطابق پکیج AxManager خودتان تغییر دهید

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.UserManager
import android.util.Log
import com.topjohnwu.superuser.ipc.RootService
import me.weishu.kernelsu.IKsuInterface // اگر اینترفیس در پروژه شما نام دیگری دارد، اصلاح کنید
import rikka.parcelablelist.ParcelableListSlice

/**
 * کلاسی برای مدیریت سرویس‌های روت در AxManager
 * بر پایه ساختار KernelSU
 */
class KsuService : RootService() {

    companion object {
        private const val TAG = "AxManagerService"
    }

    override fun onBind(intent: Intent): IBinder {
        return super.onBind(intent)
    }

    private fun getAllUserIds(): IntArray {
        return try {
            val um = getSystemService(USAGE_STATS_SERVICE) as UserManager // یا USER_SERVICE
            // متد getAliveUsers در API 31 اضافه شده است
            val method = um.javaClass.getMethod("getAliveUsers")
            val users = method.invoke(um) as List<*>
            extractUserIds(users)
        } catch (e: Exception) {
            Log.e(TAG, "getAliveUsers reflection failed", e)
            intArrayOf(0)
        }
    }

    private fun extractUserIds(users: List<*>?): IntArray {
        return try {
            users?.map { user ->
                user!!.javaClass.getField("id").getInt(user)
            }?.toIntArray() ?: intArrayOf(0)
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting ID from UserInfo", e)
            intArrayOf(0)
        }
    }

    private fun getInstalledPackagesAll(flags: Int): ArrayList<PackageInfo> {
        val packages = ArrayList<PackageInfo>()
        // در اینجا فرض شده که userId از متد getInstalledPackagesAsUser مدیریت می‌شود
        // برای سادگی، از اولین یوزر استفاده می‌کنیم یا منطق مدیریت یوزر را اضافه کنید
        val userId = getAllUserIds().firstOrNull() ?: 0
        Log.i(TAG, "getInstalledPackagesAll: $userId")
        packages.addAll(getInstalledPackagesAsUser(flags, userId))
        return packages
    }

    @Suppress("UNCHECKED_CAST")
    private fun getInstalledPackagesAsUser(flags: Int, userId: Int): List<PackageInfo> {
        val pm: PackageManager = packageManager
        return try {
            val method = pm.javaClass.getDeclaredMethod(
                "getInstalledPackagesAsUser",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )
            method.invoke(pm, flags, userId) as List<PackageInfo>
        } catch (e: Throwable) {
            Log.e(TAG, "err: ${e.message}", e)
            emptyList()
        }
    }

    private inner class Stub : IKsuInterface.Stub() {
        override fun getPackages(flags: Int): ParcelableListSlice<PackageInfo> {
            val list = getInstalledPackagesAll(flags)
            Log.i(TAG, "getPackages: ${list.size}")
            return ParcelableListSlice(list)
        }

        override fun getUserIds(): IntArray {
            val ids = getAllUserIds()
            Log.i(TAG, "getUserIds: ${ids.contentToString()}")
            return ids
        }
    }

    override fun newBinder(): IKsuInterface {
        return Stub()
    }
}
