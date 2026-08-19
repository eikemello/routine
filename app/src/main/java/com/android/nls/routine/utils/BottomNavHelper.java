package com.android.nls.routine.utils;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import com.android.nls.routine.R;
import com.android.nls.routine.activity.ConfigActivity;
import com.android.nls.routine.activity.HistoryActivity;
import com.android.nls.routine.activity.HomeActivity;
import com.android.nls.routine.activity.TrackerConfigActivity;

public class BottomNavHelper {

    public static void setup(Activity activity, int selectedItemId) {
        View bottomNav = activity.findViewById(R.id.bottomNavigation);
        if (bottomNav == null) {
            return;
        }

        View navHome = activity.findViewById(R.id.nav_home);
        View navTrackers = activity.findViewById(R.id.nav_trackers);
        View navHistory = activity.findViewById(R.id.nav_history);
        View navConfig = activity.findViewById(R.id.nav_config);

        setSelected(navHome, selectedItemId == R.id.nav_home);
        setSelected(navTrackers, selectedItemId == R.id.nav_trackers);
        setSelected(navHistory, selectedItemId == R.id.nav_history);
        setSelected(navConfig, selectedItemId == R.id.nav_config);

        navHome.setOnClickListener(v -> navigate(activity, HomeActivity.class, R.id.nav_home, selectedItemId));
        navTrackers.setOnClickListener(v -> navigate(activity, TrackerConfigActivity.class, R.id.nav_trackers, selectedItemId));
        navHistory.setOnClickListener(v -> navigate(activity, HistoryActivity.class, R.id.nav_history, selectedItemId));
        navConfig.setOnClickListener(v -> navigate(activity, ConfigActivity.class, R.id.nav_config, selectedItemId));
    }

    private static void setSelected(View view, boolean selected) {
        if (view != null) {
            view.setSelected(selected);
            // Propagate selected state to child views (icon and text)
            if (view instanceof android.view.ViewGroup group) {
                for (int i = 0; i < group.getChildCount(); i++) {
                    group.getChildAt(i).setSelected(selected);
                }
            }
        }
    }

    private static void navigate(Activity activity, Class<?> targetActivity, int itemId, int selectedItemId) {
        if (itemId == selectedItemId) {
            return;
        }
        Intent intent = new Intent(activity, targetActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
        activity.finish();
    }
}