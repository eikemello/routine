package com.android.nls.routine.utils;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.android.nls.routine.R;
import java.util.List;

/**
 * Side menu opened by the Menu item of the bottom navigation: a floating
 * panel, sized to its options, that slides in from the right edge over a
 * dimmed scrim and rests just above the bottom navigation, next to the Menu
 * item and within reach of the thumb. It is attached on top of the activity content, so it overlays the
 * screen instead of resizing it, and it is closed by tapping the scrim, by
 * the back gesture or by picking an option (the option runs once the panel
 * is out of the way).
 */
public final class SideMenu {

    /** One entry of the menu; {@code current} highlights the screen already open. */
    public record Option(@DrawableRes int iconRes, @StringRes int titleRes, boolean current, Runnable action) {
    }

    private static final long OPEN_DURATION_MS = 260L;
    private static final long CLOSE_DURATION_MS = 200L;
    /** Space between the panel and the bottom navigation (or the screen edges). */
    private static final int PANEL_GAP_DP = 12;

    private final Activity mActivity;
    private final ViewGroup mHost;
    private final View mRoot;
    private final View mScrim;
    private final View mPanel;
    private OnBackPressedCallback mBackCallback;
    private boolean mClosing;

    private SideMenu(Activity activity, List<Option> options) {
        mActivity = activity;
        mHost = activity.findViewById(android.R.id.content);
        mRoot = LayoutInflater.from(activity).inflate(R.layout.side_menu, mHost, false);
        mScrim = mRoot.findViewById(R.id.sideMenuScrim);
        mPanel = mRoot.findViewById(R.id.sideMenuPanel);

        LinearLayout items = mRoot.findViewById(R.id.sideMenuItems);
        LayoutInflater inflater = LayoutInflater.from(activity);
        for (Option option : options) {
            View item = inflater.inflate(R.layout.item_side_menu, items, false);
            ImageView icon = item.findViewById(R.id.imgSideMenuIcon);
            TextView title = item.findViewById(R.id.txtSideMenuTitle);
            icon.setImageResource(option.iconRes());
            title.setText(option.titleRes());
            // Selected state drives the highlight of the background, icon and text
            item.setSelected(option.current());
            icon.setSelected(option.current());
            title.setSelected(option.current());
            item.setOnClickListener(v -> close(option.current() ? null : option.action()));
            items.addView(item);
        }

        mScrim.setOnClickListener(v -> close(null));
    }

    /** Opens the menu over the given activity (ignored while one is already open). */
    public static void show(Activity activity, List<Option> options) {
        ViewGroup host = activity.findViewById(android.R.id.content);
        if (host == null || host.findViewById(R.id.sideMenuPanel) != null) {
            return;
        }
        new SideMenu(activity, options).open();
    }

    private void open() {
        positionPanel();
        mHost.addView(mRoot);

        // The panel wraps its options, so its width is only known after the
        // layout: it starts beyond the right edge of the screen instead
        mPanel.setTranslationX(mActivity.getResources().getDisplayMetrics().widthPixels);
        mPanel.animate()
                .translationX(0f)
                .setDuration(OPEN_DURATION_MS)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        mScrim.animate().alpha(1f).setDuration(OPEN_DURATION_MS).start();

        if (mActivity instanceof ComponentActivity componentActivity) {
            mBackCallback = new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    close(null);
                }
            };
            componentActivity.getOnBackPressedDispatcher().addCallback(componentActivity, mBackCallback);
        }
    }

    /**
     * Rests the panel just above the bottom navigation (the screens draw edge
     * to edge, so the navigation already sits above the system bar), or above
     * the system navigation bar on a screen without it.
     */
    private void positionPanel() {
        int gap = Common.dpToPx(mActivity, PANEL_GAP_DP);
        int bottomMargin;

        View bottomNav = mActivity.findViewById(R.id.bottomNavigation);
        if (bottomNav != null && bottomNav.getHeight() > 0) {
            int[] navLocation = new int[2];
            int[] hostLocation = new int[2];
            bottomNav.getLocationInWindow(navLocation);
            mHost.getLocationInWindow(hostLocation);
            int navTop = navLocation[1] - hostLocation[1];
            bottomMargin = mHost.getHeight() - navTop + gap;
        } else {
            Insets bars = Insets.NONE;
            WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(mActivity.getWindow().getDecorView());
            if (insets != null) {
                bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            }
            bottomMargin = bars.bottom + gap;
        }

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mPanel.getLayoutParams();
        params.bottomMargin = bottomMargin;
        params.setMarginEnd(Common.dpToPx(mActivity, 16));
        mPanel.setLayoutParams(params);
    }

    /** Slides the panel out, removes the overlay and then runs the picked option, if any. */
    private void close(Runnable afterClose) {
        if (mClosing) {
            return;
        }
        mClosing = true;
        if (mBackCallback != null) {
            mBackCallback.remove();
            mBackCallback = null;
        }

        mScrim.animate().alpha(0f).setDuration(CLOSE_DURATION_MS).start();
        int endMargin = ((ViewGroup.MarginLayoutParams) mPanel.getLayoutParams()).getMarginEnd();
        mPanel.animate()
                .translationX(mPanel.getWidth() + endMargin)
                .setDuration(CLOSE_DURATION_MS)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    mHost.removeView(mRoot);
                    if (afterClose != null) {
                        afterClose.run();
                    }
                })
                .start();
    }
}
