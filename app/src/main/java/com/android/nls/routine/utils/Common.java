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

    public static long getStartOfMonthInMillis() {
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static long getEndOfMonthInMillis() {
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    /**
     * Returns the start (00:00:00.000) of the most recent occurrence of the given
     * day of the month. Used to reset the expense total on the card statement
     * closing day: if today's day is >= closingDay, it returns that day of the
     * current month; otherwise, that day of the previous month.
     */
    public static long getStartOfExpenseCycleInMillis(int closingDay) {
        Calendar now = new GregorianCalendar();
        Calendar start = new GregorianCalendar();

        if (now.get(Calendar.DAY_OF_MONTH) >= closingDay) {
            start.set(Calendar.MONTH, now.get(Calendar.MONTH));
        } else {
            start.set(Calendar.MONTH, now.get(Calendar.MONTH) - 1);
        }
        start.set(Calendar.DAY_OF_MONTH, Math.min(closingDay, start.getActualMaximum(Calendar.DAY_OF_MONTH)));
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        return start.getTimeInMillis();
    }

    public static long getStartOfWeekInMillis() {
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static long getEndOfWeekInMillis() {
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        calendar.add(Calendar.DAY_OF_WEEK, 6);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    public static long getStartOfDayInMillis(long timestamp) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static long getEndOfDayInMillis(long timestamp) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    public static String getDateFromTimestamp(long timestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d", Locale.ENGLISH);
        return dateFormat.format(new Date(timestamp));
    }

    public static String getMonthYearFromTimestamp(long timestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH);
        return dateFormat.format(new Date(timestamp));
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
