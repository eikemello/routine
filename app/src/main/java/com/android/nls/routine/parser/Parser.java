package com.android.nls.routine.parser;

import android.service.notification.StatusBarNotification;
import com.android.nls.routine.model.Expense;

public interface Parser {

    Expense parse(StatusBarNotification sbn);
}