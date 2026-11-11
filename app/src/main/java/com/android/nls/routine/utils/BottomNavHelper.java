package com.android.nls.routine.utils;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import com.android.nls.routine.R;
import com.android.nls.routine.activity.AllDataActivity;
import com.android.nls.routine.activity.ConfigActivity;
import com.android.nls.routine.activity.HistoryActivity;
import com.android.nls.routine.activity.HomeActivity;
import com.android.nls.routine.activity.TrackerConfigActivity;
import java.util.List;

public class BottomNavHelper {

    public static void setup(Activity activity, int selectedItemId) {
        View bottomNav = activity.findViewById(R.id.bottomNavigation);
        if (bottomNav == null) {
            return;
        }

        View navHome = activity.findViewById(R.id.nav_home);
        View navTrackers = activity.findViewById(R.id.nav_trackers);
        View navHistory = activity.findViewById(R.id.nav_history);
        View navMenu = activity.findViewById(R.id.nav_menu);

        setSelected(navHome, selectedItemId == R.id.nav_home);
        setSelected(navTrackers, selectedItemId == R.id.nav_trackers);
        setSelected(navHistory, selectedItemId == R.id.nav_history);
        setSelected(navMenu, selectedItemId == R.id.nav_menu);

        navHome.setOnClickListener(v -> navigate(activity, HomeActivity.class, R.id.nav_home, selectedItemId));
        navTrackers.setOnClickListener(v -> navigate(activity, TrackerConfigActivity.class, R.id.nav_trackers, selectedItemId));
        navHistory.setOnClickListener(v -> navigate(activity, HistoryActivity.class, R.id.nav_history, selectedItemId));
        navMenu.setOnClickListener(v -> showMenu(activity));
    }

    /**
     * Opens the side menu of the menu item: it holds the screens that no
     * longer have a tab of their own - Config and the full data list (All
     * data) - with the screen already open highlighted.
     */
    private static void showMenu(Activity activity) {
        SideMenu.show(activity, List.of(
                new SideMenu.Option(R.drawable.ic_config, R.string.config,
                        activity instanceof ConfigActivity,
                        () -> chooseOption(activity, ConfigActivity.class)),
                new SideMenu.Option(R.drawable.ic_category, R.string.all_data,
                        activity instanceof AllDataActivity,
                        () -> chooseOption(activity, AllDataActivity.class))));
    }

    /**
     * Opens one of the menu options. Unlike the tabs, the "already open" check
     * compares the current screen instead of the selected nav item, because
     * both options share the menu item: on Config, picking Config must only
     * close the popup, while picking All data must still navigate.
     */
    private static boolean chooseOption(Activity activity, Class<?> targetActivity) {
        if (activity.getClass().equals(targetActivity)) {
            return true;
        }

        Intent intent = new Intent(activity, targetActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
        activity.finish();
        return true;
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