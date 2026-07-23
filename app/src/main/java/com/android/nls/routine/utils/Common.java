package com.android.nls.routine.utils;

import android.content.Context;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class Common {

    public static String generateTag(Class<?> clazz) {
        return Constants.TAG + clazz.getSimpleName();
    }

    public static long getStartOfDayInMillis() {
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static long getEndOfDayInMillis() {
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    public static String getHourFromTimestamp(String timestamp) {
        try {
            long timestampMillis = Long.parseLong(timestamp);
            Date date = new Date(timestampMillis);
            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return outputFormat.format(date);
        } catch (NumberFormatException ignored) {
        }
        return timestamp;
    }

    public static String getWeekDay() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d", Locale.ENGLISH);
        return dateFormat.format(new Date());
    }

    public static String getCurrentGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        if (hour >= 5 && hour < 12) {
            return Constants.GREETINGS.get(0);
        } else if (hour >= 12 && hour < 18) {
            return Constants.GREETINGS.get(1);
        } else {
            return Constants.GREETINGS.get(2);
        }
    }

    public static void generateToastMessageShortWaterDrank(Context context, int amount, String message) {
        Toast.makeText(context, amount + message, Toast.LENGTH_SHORT).show();
    }

    public static void generateToastMessageShortInvalidNumber(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
