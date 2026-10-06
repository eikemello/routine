package com.android.nls.routine.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;
import com.android.nls.routine.R;
import com.android.nls.routine.service.AllDataService;
import com.android.nls.routine.service.AllDataRenderer;
import com.android.nls.routine.utils.BottomNavHelper;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Shows every record and setting stored by the user in one single list
 * (without the calendar/scoring view of {@link HistoryActivity}), under a
 * fixed header with the visible filters: a search box over every field of a
 * row and a chip row to restrict the list to one type of record. The filter
 * state (type + query) is handed to {@link AllDataRenderer#render}, so new
 * filters only need a new entry here and a rule in the renderer.
 */
public class AllDataActivity extends AppCompatActivity {

    /** Rebuilding the whole list waits for the user to stop typing. */
    private static final long SEARCH_DEBOUNCE_MS = 300L;

    private AllDataService mAllDataService;
    private AllDataRenderer mRenderer;

    private final Handler mSearchHandler = new Handler(Looper.getMainLooper());
    private Runnable mPendingSearch = () -> { };
    private AllDataRenderer.RowType mSelectedType = AllDataRenderer.RowType.ALL;
    private String mQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_all_data);
        WindowInsetsControllerCompat windowInsetsController = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        mAllDataService = new AllDataService(this);

        LinearLayout listContainer = findViewById(R.id.allDataList);
        mRenderer = new AllDataRenderer(this, listContainer, mAllDataService.load(0L, Long.MAX_VALUE));

        setupFilters();
        setupSummaryCards();
        applyFilter();

        BottomNavHelper.setup(this, R.id.nav_menu);
    }

    private void setupFilters() {
        ChipGroup chipGroup = findViewById(R.id.filterChipGroup);
        TextInputEditText etSearch = findViewById(R.id.etDataSearch);

        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            mSelectedType = typeOf(checkedId);
            applyFilter();
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Debounced: rebuilding the list on every keystroke would be
                // wasteful with years of records.
                mSearchHandler.removeCallbacks(mPendingSearch);
                mPendingSearch = () -> {
                    mQuery = s.toString();
                    applyFilter();
                };
                mSearchHandler.postDelayed(mPendingSearch, SEARCH_DEBOUNCE_MS);
            }
        });
    }

    private void setupSummaryCards() {
        LinearLayout summaryList = findViewById(R.id.summaryCardsList);
        summaryList.removeAllViews();

        // Summary data from snapshot
        double water = mAllDataService.load(0L, Long.MAX_VALUE).totalWater();
        int meals = mAllDataService.load(0L, Long.MAX_VALUE).totalMeals();
        double expenses = mAllDataService.load(0L, Long.MAX_VALUE).totalExpenses();
        int routines = mAllDataService.load(0L, Long.MAX_VALUE).totalRoutines();

        addSummaryCard(summaryList, R.drawable.ic_water, "Water", String.format("%.2f L", water / 1000.0), "8 entries", R.drawable.icon_background_blue);
        addSummaryCard(summaryList, R.drawable.ic_meal, "Meals", String.valueOf(meals), meals + " entries", R.drawable.icon_background_green);
        addSummaryCard(summaryList, R.drawable.ic_credit_card, "Expenses", String.format("$%.2f", expenses), "3 entries", R.drawable.icon_background_orange);
        addSummaryCard(summaryList, R.drawable.ic_card_track, "Workout", String.valueOf(routines), routines + " entries", R.drawable.icon_background_purple);
    }

    private void addSummaryCard(LinearLayout container, int iconRes, String title, String value, String count, int bgRes) {
        View card = android.view.LayoutInflater.from(this).inflate(R.layout.item_all_data_summary_card, container, false);
        ImageView imgIcon = card.findViewById(R.id.imgSummaryIcon);
        TextView txtTitle = card.findViewById(R.id.txtSummaryTitle);
        TextView txtValue = card.findViewById(R.id.txtSummaryValue);
        TextView txtCount = card.findViewById(R.id.txtSummaryCount);

        imgIcon.setImageResource(iconRes);
        imgIcon.setBackgroundResource(bgRes);
        txtTitle.setText(title);
        txtValue.setText(value);
        txtCount.setText(count);

        // Space between cards: a start margin on every card but the first, so
        // the row stays centered without a trailing gap on the last one.
        if (container.getChildCount() > 0) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) card.getLayoutParams();
            if (params == null) {
                params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            }
            params.setMarginStart(com.android.nls.routine.utils.Common.dpToPx(this, 8));
            card.setLayoutParams(params);
        }

        container.addView(card);
    }

    private AllDataRenderer.RowType typeOf(int checkedId) {
        if (checkedId == R.id.chipWater) {
            return AllDataRenderer.RowType.WATER;
        }
        if (checkedId == R.id.chipMeals) {
            return AllDataRenderer.RowType.MEAL;
        }
        if (checkedId == R.id.chipExpenses) {
            return AllDataRenderer.RowType.EXPENSE;
        }
        if (checkedId == R.id.chipWorkout || checkedId == R.id.chipSupplements) {
            return AllDataRenderer.RowType.ROUTINE;
        }
        return AllDataRenderer.RowType.ALL;
    }

    private void applyFilter() {
        int shown = mRenderer.render(mSelectedType, mQuery);
    }

    @Override
    protected void onDestroy() {
        mSearchHandler.removeCallbacksAndMessages(null);
        mAllDataService.closeDb();
        super.onDestroy();
    }
}