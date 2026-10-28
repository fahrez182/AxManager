package frb.axeron.manager.ui

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import frb.axeron.api.Axeron

/**
 * Where an app asks for AxManager (AxRuntime.REQUEST_ACCESS, started for a result): nothing else
 * tells an app AxManager is here. The caller is who the system says started us (callingPackage,
 * so for a result only), never anything the intent claims.
 *
 * Allowed already: its capability is pushed again, RESULT_OK. Denied in the manager: RESULT_CANCELED,
 * no dialog. Otherwise the permission dialog answers the caller, and a grant pushes its capability.
 */
class RequestAccessActivity : ComponentActivity() {

    companion object {
        private const val TAG = "RequestAccess"
        private const val BINDER_WAIT_MS = 5_000L

        // ConfigManager's, as in PrivilegeViewModel.
        private const val FLAG_ALLOWED = 1 shl 1
        private const val FLAG_DENIED = 1 shl 2
        private const val MASK_PERMISSION = FLAG_ALLOWED or FLAG_DENIED
    }

    private val main = Handler(Looper.getMainLooper())
    private var decided = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val caller = callingPackage
        if (caller == null || caller == packageName) {
            Log.w(TAG, "not started for a result (or by us): no caller to answer")
            done(RESULT_CANCELED)
            return
        }
        val ai = try {
            packageManager.getApplicationInfo(caller, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            done(RESULT_CANCELED)
            return
        }

        // A request can wake us before the server's binder push arrives.
        val listener = object : Axeron.OnBinderReceivedListener {
            override fun onBinderReceived() {
                Axeron.removeBinderReceivedListener(this)
                if (!decided) decide(ai)
            }
        }
        Axeron.addBinderReceivedListenerSticky(listener, main)
        main.postDelayed({
            Axeron.removeBinderReceivedListener(listener)
            if (!decided) {
                Log.w(TAG, "no binder from the server within ${BINDER_WAIT_MS}ms")
                done(RESULT_CANCELED)
            }
        }, BINDER_WAIT_MS)
    }

    private fun decide(ai: ApplicationInfo) {
        val flags = Axeron.getFlagsForUid(ai.uid, MASK_PERMISSION)
        when {
            flags and FLAG_ALLOWED != 0 -> {
                // Setting it again has the server push its capability again.
                Axeron.updateFlagsForUid(ai.uid, MASK_PERMISSION, FLAG_ALLOWED)
                done(RESULT_OK)
            }
            flags and FLAG_DENIED != 0 -> done(RESULT_CANCELED)
            else -> {
                decided = true
                // The dialog sets the result itself, straight to the caller.
                startActivity(
                    Intent(this, RequestPermissionActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                        .putExtra("uid", ai.uid)
                        // No process of the caller is waiting on the server for this answer.
                        .putExtra("pid", 0)
                        .putExtra("requestCode", 0)
                        .putExtra("applicationInfo", ai)
                        .putExtra(RequestPermissionActivity.EXTRA_FOR_GOOD_ONLY, true)
                )
                finish()
            }
        }
    }

    private fun done(result: Int) {
        decided = true
        setResult(result)
        finish()
    }
}
