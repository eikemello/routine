package com.android.nls.routine;

import android.app.Application;
import com.android.nls.routine.utils.HandleException;

/**
 * Application entry point. Installs the global crash handler as early as
 * possible, so any uncaught exception (UI thread or background threads such
 * as the notification listener) is recorded in a crash report file before
 * the process dies.
 */
public class RoutineApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        HandleException.install(this);
    }
}
