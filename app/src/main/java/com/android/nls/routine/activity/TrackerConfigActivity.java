package com.android.nls.routine.activity;

import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.WindowInsetsControllerCompat;
import com.android.nls.routine.R;
import com.android.nls.routine.model.Tracker;
import com.android.nls.routine.model.TrackerType;
import com.android.nls.routine.repository.TrackerRepository;
import com.android.nls.routine.utils.BottomNavHelper;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.widget.WidgetTrackerProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.List;

public class TrackerConfigActivity extends AppCompatActivity {
    private TrackerRepository mTrackerRepository;
    private LinearLayout mTrackerListContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tracker_config);
        WindowInsetsControllerCompat windowInsetsController = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        mTrackerRepository = new TrackerRepository(this);
        mTrackerListContainer = findViewById(R.id.trackerListContainer);

        renderTrackerList();
        BottomNavHelper.setup(this, R.id.nav_trackers);
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderTrackerList();
    }

    private void renderTrackerList() {
        mTrackerListContainer.removeAllViews();
        List<Tracker> trackers = mTrackerRepository.getAllTrackers();

        for (int i = 0; i < trackers.size(); i++) {
            Tracker tracker = trackers.get(i);
            mTrackerListContainer.addView(createTrackerRow(tracker));

            if (i < trackers.size() - 1) {
                // Inset divider: starts after the icon box, like a grouped list
                View divider = new View(this);
                LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, Common.dpToPx(this, 1));
                dividerParams.setMarginStart(Common.dpToPx(this, 68));
                divider.setLayoutParams(dividerParams);
                divider.setBackgroundColor(getColor(R.color.outline_subtle));
                mTrackerListContainer.addView(divider);
            }
        }
    }

    private View createTrackerRow(Tracker tracker) {
        LinearLayout row = new LinearLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Common.dpToPx(this, 68)));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(Common.dpToPx(this, 16), 0, Common.dpToPx(this,16), 0);
        row.setClickable(true);
        row.setFocusable(true);

        // Resolve selectableItemBackground from theme (attribute, not resource ID)
        try (TypedArray typedArray = getTheme().obtainStyledAttributes(new int[]{android.R.attr.selectableItemBackground})) {
            android.graphics.drawable.Drawable selectableBackground = typedArray.getDrawable(0);
            row.setForeground(selectableBackground);
        }

        // Tracker icon, in the same tonal box and tint as its home card
        ImageView icon = new ImageView(this);
        icon.setLayoutParams(new LinearLayout.LayoutParams(Common.dpToPx(this, 40), Common.dpToPx(this, 40)));
        int iconPadding = Common.dpToPx(this, 9);
        icon.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
        // The same neutral icon box every home card uses: trackers are told
        // apart by icon and name, color is reserved for status.
        icon.setBackgroundResource(R.drawable.icon_background);
        icon.setImageTintList(ColorStateList.valueOf(getColor(R.color.accent)));
        icon.setContentDescription(tracker.name());
        icon.setImageResource(getTrackerIconRes(tracker.type()));

        // Tracker name
        TextView name = new TextView(this);
        name.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        name.setPadding(Common.dpToPx(this, 12), 0, Common.dpToPx(this,12), 0);
        name.setText(tracker.name());
        name.setTextSize(15);
        name.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        name.setTextColor(getColor(R.color.text_primary));

        // Enable/disable switch
        MaterialSwitch checkBox = getEnabledSwitch(tracker);

        row.addView(icon);
        row.addView(name);
        row.addView(checkBox);

        return row;
    }

    private MaterialSwitch getEnabledSwitch(Tracker tracker) {
        MaterialSwitch checkBox = new MaterialSwitch(this);
        checkBox.setChecked(tracker.enabled());
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && requiresConfig(tracker.type())) {

                // Revert checkbox immediately - the tracker should only be enabled
                // and saved when the user clicks "Save" in the config dialog
                buttonView.setChecked(false);
                showTrackerConfigDialog(tracker);
            } else {
                mTrackerRepository.setTrackerEnabled(tracker.type(), isChecked);

                // The routines widget shows one tile per enabled routine, so it is
                // repainted every time a tracker is turned on or off
                WidgetTrackerProvider.refresh(this);
            }
        });
        return checkBox;
    }

    private boolean requiresConfig(TrackerType type) {
        return type == TrackerType.MEDICATION || type == TrackerType.SUPPLEMENT;
    }

    private void showTrackerConfigDialog(Tracker tracker) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_tracker_config, new FrameLayout(this), false);
        TextInputEditText etName = view.findViewById(R.id.etTrackerName);
        TextInputEditText etDescription = view.findViewById(R.id.etTrackerDescription);
        TextInputLayout txtInputName = view.findViewById(R.id.txtInputName);
        TextInputLayout txtInputDescription = view.findViewById(R.id.txtInputDescription);

        // Pre-fill existing config if available
        if (tracker.name() != null && !tracker.type().name().equalsIgnoreCase(tracker.name())) {
            etName.setText(tracker.name());
        }
        if (tracker.description() != null) {
            etDescription.setText(tracker.description());
        }

        String title = tracker.type() == TrackerType.MEDICATION
                ? getString(R.string.configure_medication)
                : getString(R.string.configure_supplement);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(view)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .setCancelable(true)
                .show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(AppCompatResources.getDrawable(this, R.drawable.dialog_background));
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";

            if (name.isEmpty()) {
                txtInputName.setError(getString(R.string.tracker_name_required));
                return;
            }

            mTrackerRepository.updateTrackerConfig(tracker.type(), name, description.isEmpty() ? null : description);
            mTrackerRepository.setTrackerEnabled(tracker.type(), true);
            dialog.dismiss();
            renderTrackerList();

            // A medication/supplement just configured and enabled becomes a tile
            WidgetTrackerProvider.refresh(this);
        });
    }

    private int getTrackerIconRes(TrackerType type) {
        return switch (type) {
            case WATER -> R.drawable.ic_water;
            case MEALS -> R.drawable.ic_meal;
            case EXPENSES -> R.drawable.ic_credit_card;
            case WORKOUT -> R.drawable.ic_workout;
            case MEDICATION -> R.drawable.ic_medication;
            case SUPPLEMENT -> R.drawable.ic_supplement;
        };
    }

    @Override
    protected void onDestroy() {
        mTrackerRepository.closeDb();
        super.onDestroy();
    }
}