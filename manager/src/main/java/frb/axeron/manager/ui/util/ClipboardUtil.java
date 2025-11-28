package frb.axeron.manager.ui.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

public class ClipboardUtil {
    public static boolean put(Context context, CharSequence str) {
        return put(context, ClipData.newPlainText("label", str));
    }

    public static boolean put(Context context, ClipData clipData) {
        try {
            ClipboardManager clipboard = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(clipData);
            return true;
        } catch (Exception var3) {
            return false;
        }
    }
}
