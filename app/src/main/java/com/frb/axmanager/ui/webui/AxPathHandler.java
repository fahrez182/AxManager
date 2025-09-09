package com.frb.axmanager.ui.webui;

import android.annotation.SuppressLint;
import android.os.RemoteException;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.webkit.internal.AssetHelper;

import com.frb.engine.client.Axeron;
import com.frb.engine.utils.AxWebLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class AxPathHandler implements AxWebLoader.AxPathHandler {

    private static final String TAG = "AxPathHandler";
    private static final String[] FORBIDDEN_DATA_DIRS =
            new String[] {"/data/data", "/data/system"};

    @NonNull
    private final File mDirectory;

    @SuppressLint("RestrictedApi")
    public AxPathHandler(@NonNull File directory) {
        try {
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

    private boolean isAllowedInternalStorageDir() throws IOException {
        @SuppressLint("RestrictedApi") String dir = AssetHelper.getCanonicalDirPath(mDirectory);

        for (String forbiddenPath : FORBIDDEN_DATA_DIRS) {
            if (dir.startsWith(forbiddenPath)) {
                return false;
            }
        }
        return true;
    }

    @SuppressLint("RestrictedApi")
    @Nullable
    @Override
    public WebResourceResponse handle(WebView view, WebResourceRequest request) {
        String path = request.getUrl().getPath();
        if (path == null) return new WebResourceResponse(null, null, null);

        try {
            Log.d(TAG, "Raw path: " + path);
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
}
