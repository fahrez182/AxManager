package com.frb.engine;

import android.annotation.SuppressLint;
import android.app.ActivityThread;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.ContextHidden;
import android.ddm.DdmHandleAppName;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.UserHandleHidden;
import android.util.Pair;

import com.frb.engine.implementation.AxeronService;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import dev.rikka.tools.refine.Refine;

public class UserService {
    public static Pair<IBinder, String> create(int uid, String[] args) {
        int userId = uid / 100000;

        IBinder service;

        System.out.println("AxManager : Creating service for user " + userId);
        try {
            ActivityThread activityThread = ActivityThread.systemMain();
            Context systemContext = activityThread.getSystemContext();

            DdmHandleAppName.setAppName("axeron_server", userId);

            UserHandle userHandle = Refine.unsafeCast(
                    UserHandleHidden.of(userId));
            Context context = Refine.<ContextHidden>unsafeCast(systemContext).createPackageContextAsUser("com.frb.axmanager", Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY, userHandle);
            Field mPackageInfo = context.getClass().getDeclaredField("mPackageInfo");
            mPackageInfo.setAccessible(true);
            Object loadedApk = mPackageInfo.get(context);
            Method makeApplication = loadedApk.getClass().getDeclaredMethod("makeApplication", boolean.class, Instrumentation.class);
            Application application = (Application) makeApplication.invoke(loadedApk, true, null);
            @SuppressLint("DiscouragedPrivateApi") Field mInitialApplication = activityThread.getClass().getDeclaredField("mInitialApplication");
            mInitialApplication.setAccessible(true);
            mInitialApplication.set(activityThread, application);

            ClassLoader classLoader = application.getClassLoader();
            Class<?> serviceClass = classLoader.loadClass(AxeronService.class.getName());
            Constructor<?> constructorWithContext = null;
            try {
                constructorWithContext = serviceClass.getConstructor(Context.class);
            } catch (NoSuchMethodException | SecurityException ignored) {
            }
            if (constructorWithContext != null) {
                service = (IBinder) constructorWithContext.newInstance(application);
            } else {
                service = (IBinder) serviceClass.newInstance();
            }
        } catch (Throwable tr) {
            System.err.println("AxManager : Unable to start service");
            return null;
        }
        return new Pair<>(service, "axmanager");
    }
}
