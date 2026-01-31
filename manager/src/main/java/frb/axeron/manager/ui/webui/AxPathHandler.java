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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import frb.axeron.api.Axeron;
import frb.axeron.server.util.AxWebLoader;
import frb.axeron.shared.PathHelper;

public class AxPathHandler implements AxWebLoader.PathHandler {

    private static final String TAG = "AxPathHandler";

    public static final String DEFAULT_MIME_TYPE = "text/plain";
    private static final String[] FORBIDDEN_DATA_DIRS =
            new String[]{"/data/data", "/data/system"};

    private static final String ALLOWED_DATA_DIRS = PathHelper.getWorkingPath(false, null).getAbsolutePath();
    private static final String ALLOWED_DATA_DIRS_ROOT = PathHelper.getWorkingPath(true, null).getAbsolutePath();

    @NonNull
    private final File mDirectory;

    private final InsetsSupplier mInsetsSupplier;

    @SuppressLint("RestrictedApi")
    public AxPathHandler(@NonNull File directory, @NonNull InsetsSupplier insetsSupplier) {
        try {
            mInsetsSupplier = insetsSupplier;
            mDirectory = new File(getCanonicalDirPath(directory));
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

    public static String getCanonicalDirPath(@NonNull File file) throws IOException {
        String canonicalPath = file.getCanonicalPath();
        if (!canonicalPath.endsWith("/")) canonicalPath += "/";
        return canonicalPath;
    }

    public static File getCanonicalFileIfChild(@NonNull File parent, @NonNull String child)
            throws IOException {
        String parentCanonicalPath = getCanonicalDirPath(parent);
        String childCanonicalPath = new File(parent, child).getCanonicalPath();
        if (childCanonicalPath.startsWith(parentCanonicalPath)) {
            return new File(childCanonicalPath);
        }
        return null;
    }

    @NonNull
    private static InputStream handleSvgzStream(@NonNull String path,
                                                @NonNull InputStream stream) throws IOException {
        return path.endsWith(".svgz") ? new GZIPInputStream(stream) : stream;
    }

    @NonNull
    public static String guessMimeType(@NonNull String filePath) {
        String mimeType = MimeUtil.getMimeFromFileName(filePath);
        return mimeType == null ? DEFAULT_MIME_TYPE : mimeType;
    }

    //UTIL

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
            File file = getCanonicalFileIfChild(mDirectory, path);
            if (file != null) {
                Log.d(TAG, "Requested path: " + getCanonicalDirPath(file));
                InputStream is = handleSvgzStream(
                        file.getPath(),
                        Axeron.newFileService().setFileInputStream(getCanonicalDirPath(file))
                );
//                if (is == null) {
//                    Log.d(TAG, "Requested path is null: " + getCanonicalDirPath(file));
//                    return null;
//                }
                String mimeType = guessMimeType(path);
                Log.d(TAG, "Requested path is: " + getCanonicalDirPath(file));
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
        String dir = getCanonicalDirPath(mDirectory);

        for (String forbiddenPath : FORBIDDEN_DATA_DIRS) {
            if (dir.startsWith(forbiddenPath) &&
                    !dir.startsWith(ALLOWED_DATA_DIRS) &&
                    !dir.startsWith(ALLOWED_DATA_DIRS_ROOT)) {
                return false;
            }
        }
        return true;
    }

    public interface InsetsSupplier {
        @NonNull
        Insets get();
    }

    public interface OnInsetsRequestedListener {
        void onInsetsRequested(boolean enable);
    }
}
