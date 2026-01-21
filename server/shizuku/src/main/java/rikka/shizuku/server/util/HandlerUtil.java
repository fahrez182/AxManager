package rikka.shizuku.server.util;

import android.os.Handler;

import java.util.Objects;

public class HandlerUtil {

    private static Handler mainHandler;

    public static Handler getMainHandler() {
        Objects.requireNonNull(mainHandler, "Please call setMainHandler first");
        return HandlerUtil.mainHandler;
    }

    public static void setMainHandler(Handler mainHandler) {
        HandlerUtil.mainHandler = mainHandler;
    }
}
