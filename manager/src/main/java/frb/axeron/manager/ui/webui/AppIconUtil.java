package frb.axeron.manager.ui.webui;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import frb.axeron.api.core.Engine;


/**
 * By FahrezONE
 */
public class AppIconUtil {

    private static final Map<String, WeakReference<Bitmap>> iconCache = new HashMap<>();
    private static final Map<String, Result> resultListeners = new HashMap<>();

    public static Bitmap loadAppIconSync(String packageName, int sizePx) {
        Bitmap cached = getFromCache(packageName);
        if (cached != null) return cached;

        try {
            PackageManager pm = Engine.getApplication().getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            Drawable drawable = pm.getApplicationIcon(appInfo);
            Bitmap raw = drawableToBitmap(drawable, sizePx);
            Bitmap icon = Bitmap.createScaledBitmap(raw, sizePx, sizePx, true);
            putToCache(icon, packageName);
            return icon;
        } catch (Exception e) {
            return null;
        }
    }

    public static void loadAppIcon(String packageName, int sizePx, Result result) {

        Bitmap cached = getFromCache(packageName);
        if (cached != null) {
            result.onIconReady(cached);
            return;
        }

        //masukan kedalam waiting
        if (resultListeners.containsKey(packageName)) return;
        resultListeners.put(packageName, result);

        new Thread(() -> {
            Bitmap icon = null;
            try {
                PackageManager pm = Engine.getApplication().getPackageManager();
                ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                Drawable drawable = pm.getApplicationIcon(appInfo);
                Bitmap raw = drawableToBitmap(drawable, sizePx);
                icon = Bitmap.createScaledBitmap(raw, sizePx, sizePx, true);
                putToCache(icon, packageName);
            } catch (Exception ignored) {
            }

            Bitmap finalIcon = icon;
            Result resultListener = resultListeners.get(packageName);

            if (resultListener != null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    resultListener.onIconReady(finalIcon);
                });
                resultListeners.remove(packageName);
            }
        }).start();
    }

    private static Bitmap getFromCache(String packageName) {
        WeakReference<Bitmap> ref = iconCache.get(packageName);
        return ref != null ? ref.get() : null;
    }

    private static void putToCache(Bitmap bmp, String packageName) {
        if (bmp != null) {
            iconCache.put(packageName, new WeakReference<>(bmp));
        }
    }

    private static Bitmap drawableToBitmap(Drawable drawable, int size) {
        if (drawable instanceof BitmapDrawable) return ((BitmapDrawable) drawable).getBitmap();
        Bitmap bmp = Bitmap.createBitmap(
                drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : size,
                drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : size,
                Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(bmp);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bmp;
    }

    public interface Result {
        void onIconReady(Bitmap icon);
    }
}
