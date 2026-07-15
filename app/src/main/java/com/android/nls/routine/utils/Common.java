package com.android.nls.routine.utils;

public class Common {

    public static String generateTag(Class<?> clazz) {
        return Constants.TAG + clazz.getSimpleName();
    }
}
