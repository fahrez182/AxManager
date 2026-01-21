package frb.axeron.shared;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.OpenableColumns;

import androidx.annotation.Nullable;

import java.io.File;

public class PathHelper {
    public static File getPath(String folderName) {
        return new File(Environment.getExternalStorageDirectory(), folderName);
    }

    public static File getShellPath(String folderName) {
        if (folderName == null) return new File(AxeronConstant.folder.SHELL_DE);
        return new File(AxeronConstant.folder.SHELL_DE, folderName);
    }

    public static File getTmpPath(String folderName) {
        return new File(AxeronConstant.folder.TMP, folderName);
    }

    public static String getRelativePath(String rootPath, String fullPath) {
        return new File(rootPath).toURI().relativize(new File(fullPath).toURI()).getPath();
    }

    @Nullable
    public static String getFileNameFromUri(Context context, Uri uri) {
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            try {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (cursor.moveToFirst()) {
                    return cursor.getString(nameIndex);
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }


}
