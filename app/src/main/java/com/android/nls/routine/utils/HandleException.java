package com.android.nls.routine.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

/**
 * Global crash handler
 * <p>
 * Every uncaught exception on any thread (main thread, worker threads and the
 * binder threads of the {@link com.android.nls.routine.listener.NotificationListener})
 * passes through here before the process dies. The handler:
 * <ol>
 *   <li>Writes a crash report file to the app's private storage (keeps the
 *       last {@value #MAX_CRASH_FILES} reports), with app/device/thread
 *       context and the full stack trace;</li>
 *   <li>Logs the crash to Logcat;</li>
 *   <li>Delegates to the previous default handler, preserving the standard
 *       Android crash behavior (process death + system dialog).</li>
 * </ol>
 * It deliberately does NOT swallow exceptions or restart the UI: a crash
 * handler cannot recover a broken code path, and silently hiding bugs makes
 * them invisible. The crash report files are the debugging trail.
 */
public class HandleException implements Thread.UncaughtExceptionHandler {

    private static final String TAG = Common.generateTag(HandleException.class);
    private static final String CRASH_DIR_NAME = "crash_reports";
    private static final String FILE_PREFIX = "crash_";
    private static final String FILE_SUFFIX = ".txt";
    private static final int MAX_CRASH_FILES = 5;
    private final Context mContext;
    private final Thread.UncaughtExceptionHandler mDefaultHandler;

    private HandleException(Context context) {
        mContext = context.getApplicationContext();
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    /**
     * Installs this handler as the process-wide uncaught exception handler.
     * Must be called as early as possible (Application.onCreate), before any
     * thread that can crash is started. Safe to call more than once.
     */
    public static void install(Context context) {
        Thread.UncaughtExceptionHandler current = Thread.getDefaultUncaughtExceptionHandler();
        if (current instanceof HandleException) {
            return; // already installed
        }
        Thread.setDefaultUncaughtExceptionHandler(new HandleException(context));
        Log.d(TAG, "Global crash handler installed");
    }

    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
        try {
            saveCrashReport(thread, throwable);
        } catch (Exception e) {
            Log.e(TAG, "Failed to save crash report", e);
        }

        Log.e(TAG, "Uncaught exception on thread '" + thread.getName() + "'", throwable);

        if (mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(thread, throwable);
        }
    }

    /**
     * Writes the crash report file with the context needed to reproduce the
     * crash later (when, where, on what device and app version).
     */
    private synchronized void saveCrashReport(Thread thread, Throwable throwable) {

        //TODO: Change for some CLOUD storage
        File filesDir = mContext.getFilesDir();
        if (filesDir == null) {
            Log.e(TAG, "Files dir not available, crash report skipped");
            return;
        }

        File crashDir = new File(filesDir, CRASH_DIR_NAME);
        if (!crashDir.exists() && !crashDir.mkdirs()) {
            Log.e(TAG, "Could not create crash report directory");
            return;
        }

        Date now = new Date();
        String fileName = FILE_PREFIX
                + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(now)
                + FILE_SUFFIX;
        File reportFile = new File(crashDir, fileName);

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(reportFile), StandardCharsets.UTF_8))) {
            writer.println("=== Routine crash report ===");
            writer.println("Date    : " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(now));
            writer.println("Thread  : " + thread.getName() + " (id " + thread.getId() + ")");
            writer.println("App     : version " + getVersionName() + " (" + getVersionCode() + ")");
            writer.println("Device  : " + Build.MANUFACTURER + " " + Build.MODEL
                    + " (Android " + Build.VERSION.RELEASE + ", API " + Build.VERSION.SDK_INT + ")");
            writer.println();
            writer.println("--- Stack trace ---");
            writer.println(getStackTrace(throwable));
            Log.d(TAG, "Crash report saved: " + reportFile.getAbsolutePath());
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Could not write crash report file", e);
        }

        deleteOldCrashReports(crashDir);
    }

    private String getVersionName() {
        try {
            PackageInfo info = mContext.getPackageManager()
                    .getPackageInfo(mContext.getPackageName(), 0);
            return info.versionName != null ? info.versionName : "unknown";
        } catch (PackageManager.NameNotFoundException e) {
            return "unknown";
        }
    }

    private long getVersionCode() {
        try {
            PackageInfo info = mContext.getPackageManager()
                    .getPackageInfo(mContext.getPackageName(), 0);
            return info.getLongVersionCode();
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }

    private String getStackTrace(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    /**
     * Keeps only the most recent {@value #MAX_CRASH_FILES} reports so the
     * private storage cannot grow indefinitely.
     */
    private void deleteOldCrashReports(File crashDir) {
        File[] files = crashDir.listFiles(
                (dir, name) -> name.startsWith(FILE_PREFIX) && name.endsWith(FILE_SUFFIX));
        if (files == null || files.length <= MAX_CRASH_FILES) {
            return;
        }

        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        for (int i = 0; i < files.length - MAX_CRASH_FILES; i++) {
            if (!files[i].delete()) {
                Log.w(TAG, "Could not delete old crash report: " + files[i].getName());
            }
        }
    }
}