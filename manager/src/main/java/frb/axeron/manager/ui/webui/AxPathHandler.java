package frb.axeron.manager.ui.webui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.webkit.internal.AssetHelper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import frb.axeron.api.Axeron;
import frb.axeron.api.utils.PathHelper;
import frb.axeron.server.utils.AxWebLoader;

public class AxPathHandler implements AxWebLoader.PathHandler {

    private static final String TAG = "AxPathHandler";
    private static final String[] FORBIDDEN_DATA_DIRS =
            new String[]{"/data/data", "/data/system"};

    private static final String ALLOWED_DATA_DIRS = PathHelper.getShellPath(null).getAbsolutePath();

    @NonNull
    private final File mDirectory;

    private final InsetsSupplier mInsetsSupplier;

    @SuppressLint("RestrictedApi")
    public AxPathHandler(@NonNull File directory, @NonNull InsetsSupplier insetsSupplier) {
        try {
            mInsetsSupplier = insetsSupplier;
            mDirectory = new File(AssetHelper.getCanonicalDirPath(directory));
            if (!isAllowedInternalStorageDir()) {
                throw new IllegalArgumentException("The given directory \"" + directory
                        + "\" doesn't exist under an allowed app internal storage directory");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Failed to resolve the canonical path for the given directory: "
                            + directory.getPath(), e);
        }
    }

    @SuppressLint("RestrictedApi")
    @Nullable
    @Override
    public WebResourceResponse handle(Context context, WebView view, WebResourceRequest request) {
        String path = request.getUrl().getPath();
        if (path == null) return new WebResourceResponse(null, null, null);
        Log.d(TAG, "Raw url: " + request.getUrl());
        Log.d(TAG, "Raw path: " + path);

        if ("/internal/insets.css".equals(path)) {
            String css = mInsetsSupplier.get().getCss();
            return new WebResourceResponse(
                    "text/css",
                    "utf-8",
                    new ByteArrayInputStream(css.getBytes(StandardCharsets.UTF_8))
            );
        }
        if ("/internal/colors.css".equals(path)) {
            String css = MonetColorsProvider.INSTANCE.getColorsCss();
            Log.d(TAG, "Raw css: " + css);
            return new WebResourceResponse(
                    "text/css",
                    "utf-8",
                    new ByteArrayInputStream(css.getBytes(StandardCharsets.UTF_8))
            );
        }
        try {
            File file = AssetHelper.getCanonicalFileIfChild(mDirectory, path);
            if (file != null) {
                Log.d(TAG, "Requested path: " + AssetHelper.getCanonicalDirPath(file));
                FileInputStream is = Axeron.newFileService().setFileInputStream(AssetHelper.getCanonicalDirPath(file));
                if (is == null) {
                    Log.d(TAG, "Requested path is null: " + AssetHelper.getCanonicalDirPath(file));
                    return null;
                }
                String mimeType = AssetHelper.guessMimeType(path);
                Log.d(TAG, "Requested path is: " + AssetHelper.getCanonicalDirPath(file));
                return new WebResourceResponse(mimeType, null, is);
            } else {
                Log.e(TAG, String.format(
                        "The requested file: %s is outside the mounted directory: %s", path,
                        mDirectory));
            }
        } catch (IOException | RemoteException e) {
            Log.e(TAG, "Error opening the requested path: " + path);
        }
        return new WebResourceResponse(null, null, null);
    }

    private boolean isAllowedInternalStorageDir() throws IOException {
        @SuppressLint("RestrictedApi") String dir = AssetHelper.getCanonicalDirPath(mDirectory);

        for (String forbiddenPath : FORBIDDEN_DATA_DIRS) {
            if (dir.startsWith(forbiddenPath) && !dir.startsWith(ALLOWED_DATA_DIRS)) {
                return false;
            }
        }
        return true;
    }

    public interface InsetsSupplier {
        @NonNull
        Insets get();
    }
}
