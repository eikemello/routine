package com.android.nls.routine.parser;

import android.service.notification.StatusBarNotification;
import android.util.Log;
import com.android.nls.routine.model.Expense;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;

public class BankDetector {
    private static final String TAG = Common.generateTag(BankDetector.class);

    public Expense detect(StatusBarNotification sbn) {
        String pkg = sbn.getOpPkg();

        if (pkg.contains(Constants.BANK_NUBANK)) {
            Log.d(TAG, "nubank pkg detect for > " + pkg);
            return new NubankParser().parse(sbn);
        }
        if (pkg.contains(Constants.BANK_ITAU)) {
            Log.d(TAG, "itau pkg detect for > " + pkg);
            return new ItauParser().parse(sbn);
        }
        if (pkg.contains(Constants.BANK_BRADESCO)) {
            Log.d(TAG, "bradesco pkg detect for > " + pkg);
            return new BradescoParser().parse(sbn);
        }
        if (pkg.contains(Constants.BANK_XP)) {
            Log.d(TAG, "xp pkg detect for > " + pkg);
            return new XpParser().parse(sbn);
        }
        if (pkg.contains(Constants.BANK_INTER)) {
            Log.d(TAG, "inter pkg detect for > " + pkg);
            return new InterParser().parse(sbn);
        }

        return null;
    }
}