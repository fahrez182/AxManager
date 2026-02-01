package frb.axeron.server.util;

import android.content.AttributionSource;
import android.content.IContentProvider;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.system.Os;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class IContentProviderCompat {

    public static Bundle callCompat(@NonNull IContentProvider provider, @Nullable String callingPkg, @Nullable String authority, @Nullable String method, @Nullable String arg, @Nullable Bundle extras) throws RemoteException {
        Bundle result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            result = provider.call((new AttributionSource.Builder(OsUtils.getUid())).setPackageName(callingPkg).build(), authority, method, arg, extras);
        } else if (Build.VERSION.SDK_INT == 30) {
            result = provider.call(callingPkg, null, authority, method, arg, extras);
        } else if (Build.VERSION.SDK_INT == 29) {
            result = provider.call(callingPkg, authority, method, arg, extras);
        } else {
            result = provider.call(callingPkg, method, arg, extras);
        }

        return result;
    }

    @Nullable
    public static Bundle call(@NonNull IContentProvider provider, @Nullable String attributeTag, @Nullable String callingPkg, @Nullable String authority, @Nullable String method, @Nullable String arg, @Nullable Bundle extras) throws RemoteException {
        Bundle reply;
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                reply = provider.call((new AttributionSource.Builder(Os.getuid())).setAttributionTag(attributeTag).setPackageName(callingPkg).build(), authority, method, arg, extras);
            } catch (LinkageError e) {
                reply = provider.call(callingPkg, attributeTag, authority, method, arg, extras);
            }
        } else if (Build.VERSION.SDK_INT == 30) {
            reply = provider.call(callingPkg, attributeTag, authority, method, arg, extras);
        } else if (Build.VERSION.SDK_INT == 29) {
            reply = provider.call(callingPkg, authority, method, arg, extras);
        } else {
            reply = provider.call(callingPkg, method, arg, extras);
        }

        return reply;
    }
}
