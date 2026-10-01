package com.android.nls.routine.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import com.android.nls.routine.R;
import com.android.nls.routine.model.Tracker;
import com.android.nls.routine.model.TrackerRecord;
import com.android.nls.routine.model.TrackerType;
import com.android.nls.routine.repository.TrackerRepository;
import com.android.nls.routine.utils.Common;
import java.util.ArrayList;
import java.util.List;

/**
 * Routines home screen widget: one tile per enabled routine tracker (workout,
 * medication and supplement - the trackers with a simple done/not-done state of
 * the day) filling a 2x2 grid, two tiles side by side at most and two rows at
 * most, so the routine of the day can be marked without opening the app.
 * <p>
 * Each tile mirrors the matching home card: the tracker icon in its colored box,
 * the configured name and the Done control under them. While the routine is open
 * the control shows the label; once the record of the day is completed the
 * provider hides the label and shows the green check in its place. Tapping the
 * control marks the routine of the day, and tapping it again unmarks it: the
 * record of the day is updated in place through
 * {@link TrackerRepository#saveTrackerRecord(TrackerType, boolean, String)}
 * (the note already written is kept), the same toggle the card and its history
 * panel offer.
 * <p>
 * The tiles follow the enabled trackers, so the widget only shows the routines
 * the user kept on: the slots without a routine stay hidden and their cells
 * collapse with them. The tiles fill a 2x2 grid, in the order the home screen
 * shows the routines - first on the top left, second on the top right, third
 * under the first -, so two routines sit side by side and the grid never grows
 * past the two rows (with a single routine enabled the lone tile takes the
 * whole card). The visible card (see the layout) hugs the grid of tiles
 * and a small padding
 * instead of filling the whole cell, so a widget with fewer routines draws a
 * smaller card centered in the cell - hidden entirely when no routine is
 * enabled at all. Like the combined widget, the provider is
 * rendered through RemoteViews
 * and every tap is a PendingIntent broadcast back to it - a widget runs in the
 * launcher, where there are no click listeners. {@link #refresh(Context)} is
 * called by HomeActivity (resume, routine marked or removed through the history
 * panel) and by the tracker config screen (a routine enabled or disabled), and
 * updatePeriodMillis (30 min, the smallest interval allowed) is what rolls the
 * tiles over to the new day.
 */
public class WidgetTrackerProvider extends AppWidgetProvider {
    private static final String TAG = Common.generateTag(WidgetTrackerProvider.class);

    /** Broadcast of a tapped Done control, carrying the routine of the tile. */
    public static final String ACTION_TOGGLE_TRACKER =
            "com.android.nls.routine.action.widget.TOGGLE_TRACKER";
    private static final String EXTRA_TRACKER_TYPE = "extra_tracker_type";

    /** The trackers the widget shows: the routines with a done/not-done state. */
    private static final TrackerType[] ROUTINE_TRACKERS = {
            TrackerType.WORKOUT,
            TrackerType.MEDICATION,
            TrackerType.SUPPLEMENT
    };

    /**
     * One tile per routine, filling the grid in order; the slots without one
     * stay hidden. The layout carries one slot per routine, so the count
     * follows the routines.
     */
    private static final int SLOT_COUNT = ROUTINE_TRACKERS.length;
    private static final int[] SLOT_IDS = {
            R.id.layoutWidgetTrackerSlot1,
            R.id.layoutWidgetTrackerSlot2,
            R.id.layoutWidgetTrackerSlot3
    };
    private static final int[] SLOT_ICON_IDS = {
            R.id.imgWidgetTrackerIcon1,
            R.id.imgWidgetTrackerIcon2,
            R.id.imgWidgetTrackerIcon3
    };
    private static final int[] SLOT_NAME_IDS = {
            R.id.txtWidgetTrackerName1,
            R.id.txtWidgetTrackerName2,
            R.id.txtWidgetTrackerName3
    };
    private static final int[] SLOT_DONE_IDS = {
            R.id.btnWidgetTrackerDone1,
            R.id.btnWidgetTrackerDone2,
            R.id.btnWidgetTrackerDone3
    };
    private static final int[] SLOT_LABEL_IDS = {
            R.id.txtWidgetTrackerDone1,
            R.id.txtWidgetTrackerDone2,
            R.id.txtWidgetTrackerDone3
    };
    private static final int[] SLOT_CHECK_IDS = {
            R.id.imgWidgetTrackerCheck1,
            R.id.imgWidgetTrackerCheck2,
            R.id.imgWidgetTrackerCheck3
    };

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        updateWidgets(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        appWidgetManager.updateAppWidget(appWidgetId, buildRemoteViews(context));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_TOGGLE_TRACKER.equals(intent.getAction())) {
            toggleTracker(context, intent.getStringExtra(EXTRA_TRACKER_TYPE));
            return;
        }

        super.onReceive(context, intent);
    }

    /**
     * Repaints every placed widget with the enabled routines and the state of the
     * day, so the tiles match the cards of the app: called on resume and after a
     * routine is marked or removed in the app, and when a tracker is enabled or
     * disabled in the config screen.
     */
    public static void refresh(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new ComponentName(context, WidgetTrackerProvider.class));

        updateWidgets(context, appWidgetManager, appWidgetIds);
    }

    private static void updateWidgets(Context context, AppWidgetManager appWidgetManager,
                                      int[] appWidgetIds) {
        if (appWidgetIds.length == 0) {
            return;
        }

        RemoteViews views = buildRemoteViews(context);
        for (int appWidgetId : appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    /**
     * Builds the widget with the enabled routines filling the slots in order (the
     * order the home screen shows them), each tile carrying the state of its
     * routine today; the slots left without a routine are hidden.
     */
    private static RemoteViews buildRemoteViews(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_tracker);

        TrackerRepository repository = new TrackerRepository(context);
        try {
            List<Tracker> routines = getEnabledRoutines(repository);

            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                if (slot < routines.size()) {
                    paintTile(context, views, slot, routines.get(slot), repository);
                } else {
                    views.setViewVisibility(SLOT_IDS[slot], View.GONE);
                }
            }

            // With no routine enabled there is nothing to show: the card is
            // hidden instead of leaving an empty background in the cell
            views.setViewVisibility(R.id.widgetTrackerCard,
                    routines.isEmpty() ? View.GONE : View.VISIBLE);
        } finally {
            repository.closeDb();
        }

        return views;
    }

    private static void paintTile(Context context, RemoteViews views, int slot, Tracker tracker,
                                  TrackerRepository repository) {
        TrackerType type = tracker.type();
        boolean completed = isCompletedToday(repository, type);
        String name = getRoutineName(context, tracker);

        views.setViewVisibility(SLOT_IDS[slot], View.VISIBLE);
        views.setImageViewResource(SLOT_ICON_IDS[slot], getIconRes(type));
        views.setInt(SLOT_ICON_IDS[slot], "setBackgroundResource", getIconBackgroundRes(type));
        views.setContentDescription(SLOT_ICON_IDS[slot], name);
        views.setTextViewText(SLOT_NAME_IDS[slot], name);

        // The control of the tile: the label while the routine is open, the green
        // check once the day was marked - the same swap the routine card makes on
        // the home screen, which shows the green status and hides the action
        views.setViewVisibility(SLOT_LABEL_IDS[slot], completed ? View.GONE : View.VISIBLE);
        views.setViewVisibility(SLOT_CHECK_IDS[slot], completed ? View.VISIBLE : View.GONE);
        views.setContentDescription(SLOT_DONE_IDS[slot], context.getString(
                completed ? R.string.widget_tracker_mark_undone : R.string.widget_tracker_mark_done,
                name));
        views.setOnClickPendingIntent(SLOT_DONE_IDS[slot], buildTogglePendingIntent(context, type));
    }

    /**
     * Marks the routine of the tile as done for today, or unmarks it when the day
     * was already marked. The record of the day is updated in place and the note
     * already written is kept, so the tap is the same toggle the card and its
     * history panel offer, only without the dialog.
     */
    private static void toggleTracker(Context context, String trackerType) {
        TrackerType type = parseRoutineTracker(trackerType);
        if (type == null) {
            Log.e(TAG, "toggleTracker: ignoring unknown routine tracker " + trackerType);
            return;
        }

        TrackerRepository repository = new TrackerRepository(context);
        try {
            TrackerRecord record = repository.getTrackerRecordForDay(type, System.currentTimeMillis());
            boolean completed = record != null && record.completed();
            repository.saveTrackerRecord(type, !completed, record != null ? record.note() : null);
        } finally {
            repository.closeDb();
        }

        refresh(context);
    }

    private static PendingIntent buildTogglePendingIntent(Context context, TrackerType type) {
        Intent intent = new Intent(context, WidgetTrackerProvider.class)
                .setAction(ACTION_TOGGLE_TRACKER)
                .setData(Uri.parse("routine://widget/tracker/" + type.name()))
                .putExtra(EXTRA_TRACKER_TYPE, type.name());

        // One request code per routine (never the slot, which changes with the
        // enabled trackers), so the intent of a tile always carries its routine
        // and two tiles never share a pending intent
        return PendingIntent.getBroadcast(context, indexOf(type), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * The enabled trackers the widget shows, in the order of the home screen: the
     * routines only, as the other trackers have their own widget or no
     * done/not-done state of the day.
     */
    private static List<Tracker> getEnabledRoutines(TrackerRepository repository) {
        List<Tracker> routines = new ArrayList<>();
        for (Tracker tracker : repository.getEnabledTrackers()) {
            if (isRoutine(tracker.type())) {
                routines.add(tracker);
            }
        }
        return routines;
    }

    private static boolean isRoutine(TrackerType type) {
        for (TrackerType routine : ROUTINE_TRACKERS) {
            if (routine == type) {
                return true;
            }
        }
        return false;
    }

    private static TrackerType parseRoutineTracker(String trackerType) {
        if (trackerType == null) {
            return null;
        }

        for (TrackerType routine : ROUTINE_TRACKERS) {
            if (routine.name().equals(trackerType)) {
                return routine;
            }
        }
        return null;
    }

    private static int indexOf(TrackerType type) {
        for (int index = 0; index < ROUTINE_TRACKERS.length; index++) {
            if (ROUTINE_TRACKERS[index] == type) {
                return index;
            }
        }
        return 0;
    }

    private static boolean isCompletedToday(TrackerRepository repository, TrackerType type) {
        TrackerRecord record = repository.getTrackerRecordForDay(type, System.currentTimeMillis());
        return record != null && record.completed();
    }

    /**
     * The name of the tile: the name configured for the routine (it can be
     * personalized in the tracker config), falling back to the label of its type.
     */
    private static String getRoutineName(Context context, Tracker tracker) {
        String name = tracker.name();
        if (name == null || name.isBlank()) {
            name = context.getString(getDefaultNameRes(tracker.type()));
        }
        return name;
    }

    /** Only the routine trackers reach the widget; anything else falls back to workout. */
    private static int getDefaultNameRes(TrackerType type) {
        return switch (type) {
            case MEDICATION -> R.string.medication;
            case SUPPLEMENT -> R.string.supplement;
            default -> R.string.workout;
        };
    }

    /**
     * The icon of the tracker, the same one the config list shows. Only the
     * routine trackers reach the widget; anything else falls back to workout.
     */
    private static int getIconRes(TrackerType type) {
        return switch (type) {
            case MEDICATION -> R.drawable.ic_medication;
            case SUPPLEMENT -> R.drawable.ic_supplement;
            case WORKOUT -> R.drawable.ic_workout;
            default -> R.drawable.ic_workout;
        };
    }

    /**
     * The icon box of the card of that tracker, so a tile looks like its card.
     * Only the routine trackers reach the widget; anything else falls back to
     * the workout box.
     */
    private static int getIconBackgroundRes(TrackerType type) {
        return switch (type) {
            case MEDICATION -> R.drawable.icon_background_pink;
            case SUPPLEMENT -> R.drawable.icon_background_cyan;
            case WORKOUT -> R.drawable.icon_background_purple;
            default -> R.drawable.icon_background_purple;
        };
    }
}
