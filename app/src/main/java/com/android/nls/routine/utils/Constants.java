package com.android.nls.routine.utils;

import java.util.Arrays;
import java.util.List;

public class Constants {
    public static final String TAG = "RTN_";

    //HomeActivity
    public static final String WATER_INVALID_NUMBER = "Please enter a valid number!";
    public static final String MEAL_INVALID_TEXT = "Please enter a valid text!";
    public static final List<String> GREETINGS = Arrays.asList("Good morning", "Good afternoon", "Good night");
    public static final String DEFAULT_DAILY_WATER = "2500";

    //ConfigActivity
    public static final String DAILY_WATER = "DAILY_WATER";
    public static final String BTN_DEFAULT_1 = "BTN_DEFAULT_1";
    public static final String BTN_DEFAULT_2 = "BTN_DEFAULT_2";
    public static final String BTN_DEFAULT_3 = "BTN_DEFAULT_3";
    public static final String MONTHLY_LIMIT = "MONTHLY_LIMIT";
    public static final String CARD_STATEMENT_CLOSING = "CARD_STATEMENT_CLOSING";
    public static final String DEFAULT_BTN_1_VALUE = "50";
    public static final String DEFAULT_BTN_2_VALUE = "100";
    public static final String DEFAULT_BTN_3_VALUE = "250";
    public static final String DEFAULT_MONTHLY_LIMIT_VALUE = "1000";
    public static final String DEFAULT_CARD_STATEMENT_CLOSING = "01/xx";

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
    public static final String COLUMN_NAME_CARD_STATEMENT_CLOSING = "CARD_STATEMENT_CLOSING";

    public static final String TABLE_NAME_EXPENSE_TEST = "EXPENSE_TEST";
    public static final String COLUMN_NAME_EXPENSE_TEXT = "EXPENSE_TEXT";
    public static final String COLUMN_NAME_EXPENSE_VALUE = "EXPENSE_VALUE";
    public static final String COLUMN_NAME_BANK_NAME = "BANK";

    public static final String TABLE_NAME_MEAL = "MEAL";
    public static final String COLUMN_NAME_MEAL = "MEAL";
    public static final String COLUMN_NAME_MEAL_OBS = "MEAL_OBS";
    public static final String COLUMN_NAME_MEAL_STATUS = "MEAL_STATUS";
    public static final String CORRECT_MEAL = "CORRECT_MEAL";
    public static final String WARNING_MEAL = "WARNING_MEAL";
    public static final String WRONG_MEAL = "WRONG_MEAL";
    public static final String BREAKFAST = "Breakfast";
    public static final String LUNCH = "Lunch";
    public static final String TEA = "Tea";
    public static final String DINNER = "Dinner";

    //Bank Detector
    public static final String BANK_BRADESCO = "bradesco";
    public static final String BANK_INTER = "inter";
    public static final String BANK_ITAU = "itau";
    public static final String BANK_NUBANK = "nubank";
    public static final String BANK_XP = "xp";

    //Trackers
    public static final String TABLE_NAME_TRACKERS = "TRACKERS";
    public static final String COLUMN_NAME_TRACKER_TYPE = "TRACKER_TYPE";
    public static final String COLUMN_NAME_TRACKER_NAME = "TRACKER_NAME";
    public static final String COLUMN_NAME_TRACKER_ICON = "TRACKER_ICON";
    public static final String COLUMN_NAME_TRACKER_ENABLED = "TRACKER_ENABLED";
    public static final String COLUMN_NAME_TRACKER_DESCRIPTION = "TRACKER_DESCRIPTION";

    public static final String TABLE_NAME_TRACKER_RECORDS = "TRACKER_RECORDS";
    public static final String COLUMN_NAME_TRACKER_RECORD_TYPE = "TRACKER_RECORD_TYPE";
    public static final String COLUMN_NAME_TRACKER_RECORD_COMPLETED = "TRACKER_RECORD_COMPLETED";
    public static final String COLUMN_NAME_TRACKER_RECORD_NOTE = "TRACKER_RECORD_NOTE";
    public static final String COLUMN_NAME_TRACKER_RECORD_TIMESTAMP = "TRACKER_RECORD_TIMESTAMP";

    public static final String TRACKER_NAME_WATER = "Water";
    public static final String TRACKER_NAME_MEALS = "Meals";
    public static final String TRACKER_NAME_EXPENSES = "Expenses";
    public static final String TRACKER_NAME_WORKOUT = "Workout";
    public static final String TRACKER_NAME_MEDICATION = "Medication";
    public static final String TRACKER_NAME_SUPPLEMENT = "Supplement";

    public static final String TRACKER_ICON_WATER = "water";
    public static final String TRACKER_ICON_MEALS = "meal";
    public static final String TRACKER_ICON_EXPENSES = "expenses";
    public static final String TRACKER_ICON_WORKOUT = "workout";
    public static final String TRACKER_ICON_MEDICATION = "medication";
    public static final String TRACKER_ICON_SUPPLEMENT = "supplement";
}
