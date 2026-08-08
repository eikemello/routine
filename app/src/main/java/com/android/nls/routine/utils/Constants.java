package com.android.nls.routine.utils;

import java.util.Arrays;
import java.util.List;

public class Constants {
    public static final String TAG = "RTN_";

    //HomeActivity
    public static final String WATER_INVALID_NUMBER = "Please enter a valid number!";
    public static final List<String> GREETINGS = Arrays.asList("Good morning", "Good afternoon", "Good night");
    public static final List<String> DEFAULT_ADD_WATER_BUTTON_VALUES = Arrays.asList("50", "100", "250");
    public static final String DEFAULT_DAILY_WATER = "2500";

    //ConfigActivity
    public static final String DAILY_WATER = "DAILY_WATER";
    public static final String BTN_DEFAULT_1 = "BTN_DEFAULT_1";
    public static final String BTN_DEFAULT_2 = "BTN_DEFAULT_2";
    public static final String BTN_DEFAULT_3 = "BTN_DEFAULT_3";
    public static final String MONTHLY_LIMIT = "MONTHLY_LIMIT";
    public static final String DEFAULT_BTN_1_VALUE = "50";
    public static final String DEFAULT_BTN_2_VALUE = "100";
    public static final String DEFAULT_BTN_3_VALUE = "250";

    //SQLite
    public static final String TABLE_NAME_WATER = "WATER";
    public static final String COLUMN_NAME_WATER_DRANK = "WATER_DRANK";
    public static final String COLUMN_NAME_TIMESTAMP = "TIMESTAMP";

    public static final String TABLE_NAME_USER_CONFIG = "USER_CONFIG";
    public static final String COLUMN_NAME_DAILY_WATER = "DAILY_WATER";
    public static final String COLUMN_NAME_BTN_1_ADD_WATER = "BTN_1_ADD_WATER";
    public static final String COLUMN_NAME_BTN_2_ADD_WATER = "BTN_2_ADD_WATER";
    public static final String COLUMN_NAME_BTN_3_ADD_WATER = "BTN_3_ADD_WATER";
    public static final String COLUMN_NAME_MONTHLY_LIMIT = "MONTHLY_LIMIT";

    public static final String TABLE_NAME_EXPENSE_TEST = "EXPENSE_TEST";
    public static final String COLUMN_NAME_EXPENSE_TEXT = "EXPENSE_TEXT";
    public static final String COLUMN_NAME_EXPENSE_VALUE = "EXPENSE_VALUE";
    public static final String COLUMN_NAME_BANK_NAME = "BANK";

    //Bank Detector
    public static final String BANK_BRADESCO = "bradesco";
    public static final String BANK_INTER = "inter";
    public static final String BANK_ITAU = "itau";
    public static final String BANK_NUBANK = "nubank";
    public static final String BANK_XP = "xp";
}
