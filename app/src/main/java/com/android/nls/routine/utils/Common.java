package com.android.nls.routine.utils;

import android.content.Context;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

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

    /**
     * Returns the start (00:00:00.000) of the most recent occurrence of the given
     * day of the month. Used to reset the expense total on the card statement
     * closing day: if today's day is >= closingDay, it returns that day of the
     * current month; otherwise, that day of the previous month.
     */
    public static long getStartOfExpenseCycleInMillis(double closingDay) {
        Calendar now = new GregorianCalendar();
        Calendar start = new GregorianCalendar();

        if (now.get(Calendar.DAY_OF_MONTH) >= closingDay) {
            start.set(Calendar.MONTH, now.get(Calendar.MONTH));
        } else {
            start.set(Calendar.MONTH, now.get(Calendar.MONTH) - 1);
        }
        start.set(Calendar.DAY_OF_MONTH, (int) Math.min(closingDay, start.getActualMaximum(Calendar.DAY_OF_MONTH)));
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        return start.getTimeInMillis();
    }

    public static long getStartOfWeekInMillis() {
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // Calculate the Monday of the current week explicitly.
        // Do NOT use set(DAY_OF_WEEK, MONDAY): it depends on the locale's
        // firstDayOfWeek (e.g. SUNDAY in en-US), which can yield the wrong Monday
        // (e.g. on a Sunday it would return the NEXT week's Monday).
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int daysFromMonday = (dayOfWeek + 5) % 7; // Sunday=1 -> 6, Monday=2 -> 0, ..., Saturday=7 -> 5
        calendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday);
        return calendar.getTimeInMillis();
    }

    public static long getEndOfWeekInMillis() {
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(getStartOfWeekInMillis());
        calendar.add(Calendar.DAY_OF_MONTH, 6);
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
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
        return dateFormat.format(new Date(timestamp));
    }

    public static String getMonthYearFromTimestamp(long timestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        return dateFormat.format(new Date(timestamp));
    }

    public static String getAbbreviatedMonthFromTimestamp(long timestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM", Locale.getDefault());
        return dateFormat.format(new Date(timestamp));
    }

    public static String getAbbreviatedMonthYearFromTimestamp(long timestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
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

    /**
     * Returns the "HH:mm" time of a timestamp in epoch millis.
     */
    public static String getTimeFromTimestamp(long timestamp) {
        SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return outputFormat.format(new Date(timestamp));
    }

    /**
     * Returns the short day label ("10/06/2026"), used by the All data screen
     * to group records of every year by day.
     */
    public static String getDateWithYearFromTimestamp(long timestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return dateFormat.format(new Date(timestamp));
    }

    public static String getWeekDay() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
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

    /**
     * Start of the local day {@code days} days before today (0 is today),
     * moving by calendar days so a DST change does not shift the result.
     */
    public static long getStartOfDayDaysAgoInMillis(int days) {
        Calendar calendar = new GregorianCalendar();
        calendar.add(Calendar.DAY_OF_MONTH, -days);
        return getStartOfDayInMillis(calendar.getTimeInMillis());
    }

    /** Start of the first local day of the current month. */
    public static long getStartOfMonthInMillis() {
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        return getStartOfDayInMillis(calendar.getTimeInMillis());
    }

    /**
     * Converts a date picked in a MaterialDatePicker - which answers with the
     * UTC midnight of the chosen day - into the start of that same calendar
     * day in the local time zone.
     */
    public static long getLocalStartOfDayFromUtcDate(long utcMillis) {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(utcMillis);
        Calendar local = new GregorianCalendar(
                utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH));
        return local.getTimeInMillis();
    }

    /** Returns the short "dd/MM" label of a timestamp, used by date range buttons. */
    public static String getDayMonthFromTimestamp(long timestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
        return dateFormat.format(new Date(timestamp));
    }

    public static int dpToPx(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}
