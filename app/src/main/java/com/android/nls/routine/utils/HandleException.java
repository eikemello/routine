package com.android.nls.routine.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.crashlytics.CustomKeysAndValues;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class HandleException implements Thread.UncaughtExceptionHandler {

    private static final String TAG = Common.generateTag(HandleException.class);
    private static final String KEY_CRASH_THREAD = "crash_thread";
    private static final String KEY_CRASH_THREAD_ID = "crash_thread_id";
    private static final String KEY_APP_VERSION = "app_version";
    private static final String KEY_DEVICE = "device";
    private static final String KEY_OS_VERSION = "os_version";
    private static final String NO_VERSION_NAME = "NO_VERSION_NAME";
    private final Thread.UncaughtExceptionHandler mDefaultHandler;

    private HandleException(Thread.UncaughtExceptionHandler defaultHandler) {
        mDefaultHandler = defaultHandler;
    }

    /**
     * Installs this handler as the process-wide uncaught exception handler.
     * Must be called as early as possible (Application.onCreate), before any
     * thread that can crash is started. Safe to call more than once.
     */
    public static void install(Context context) {
        Thread.UncaughtExceptionHandler current = Thread.getDefaultUncaughtExceptionHandler();
        if (current instanceof HandleException) {
            return;
        }
        Thread.setDefaultUncaughtExceptionHandler(new HandleException(current));
        seedDeviceInfo(context);
        Log.d(TAG, "Global crash handler installed");
    }

    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
        try {
            FirebaseCrashlytics crashlytics = getCrashlytics();
            if (crashlytics != null) {
                crashlytics.setCustomKeys(new CustomKeysAndValues.Builder()
                        .putString(KEY_CRASH_THREAD, thread.getName())
                        .putLong(KEY_CRASH_THREAD_ID, thread.getId())
                        .build());
                crashlytics.log("Uncaught exception on thread '" + thread.getName() + "'");
            }
        } catch (Exception e) {
            // Never let the enrichment of the report stop the report itself (or the
            // standard crash flow) from happening.
            Log.e(TAG, "Could not attach crash context to Crashlytics", e);
        }

        Log.e(TAG, "Uncaught exception on thread '" + thread.getName() + "'", throwable);

        if (mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(thread, throwable);
        }
    }

    private static void seedDeviceInfo(Context context) {
        try {
            FirebaseCrashlytics crashlytics = getCrashlytics();
            if (crashlytics == null) {
                return;
            }
            crashlytics.setCustomKeys(new CustomKeysAndValues.Builder()
                    .putString(KEY_APP_VERSION, getVersionName(context))
                    .putString(KEY_DEVICE, Build.MANUFACTURER + " " + Build.MODEL)
                    .putString(KEY_OS_VERSION,
                            "Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")")
                    .build());
        } catch (Exception e) {
            Log.e(TAG, "Could not seed device info into Crashlytics", e);
        }
    }

    private static String getVersionName(Context context) {
        try {
            PackageInfo info = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return info.versionName != null ? info.versionName : NO_VERSION_NAME;
        } catch (PackageManager.NameNotFoundException e) {
            return NO_VERSION_NAME + e.getMessage();
        }
    }

    public static void logNonFatal(String tag, String where, Throwable throwable) {
        if (throwable == null) {
            return;
        }
        Log.e(tag, "Non-fatal exception at " + where, throwable);
        try {
            FirebaseCrashlytics crashlytics = getCrashlytics();
            if (crashlytics == null) {
                return;
            }
            crashlytics.setCustomKey(KEY_CRASH_THREAD, Thread.currentThread().getName());
            crashlytics.log("Non-fatal exception at " + where);
            crashlytics.recordException(throwable);
        } catch (Exception e) {
            Log.e(TAG, "Could not report non-fatal exception to Crashlytics", e);
        }
    }

    private static FirebaseCrashlytics getCrashlytics() {
        try {
            return FirebaseCrashlytics.getInstance();
        } catch (Exception e) {
            Log.w(TAG, "Crashlytics is not available", e);
            return null;
        }
    }

}