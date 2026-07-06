package com.android.nls.Routine.utils;

public class Common {

    public static String generateTag(Object className) {
        return Constants.TAG + className.getClass().getName();
    }
}
