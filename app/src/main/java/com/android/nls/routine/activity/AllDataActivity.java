package com.android.nls.routine.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import androidx.core.view.WindowInsetsControllerCompat;
import com.android.nls.routine.R;
import com.android.nls.routine.cardhistory.CardRecordDialog;
import com.android.nls.routine.model.ExpenseRecord;
import com.android.nls.routine.service.AllDataService;
import com.android.nls.routine.service.AllDataRenderer;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;
import com.android.nls.routine.utils.BottomNavHelper;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;

/**
 * Shows every record and setting stored by the user in one single list
 * (without the calendar/scoring view of {@link HistoryActivity}), under a
 * fixed header with the visible filters: a search box over every field of a
 * row, a chip row to restrict the list to one type of record and the Date
 * range / Category / Type buttons above them. The filter state is handed to
 * {@link AllDataRenderer#render} as one {@link AllDataRenderer.Filter}, so new
 * filters only need a new entry here and a rule in the renderer.
 */
public class AllDataActivity extends AppCompatActivity {

    /** Rebuilding the whole list waits for the user to stop typing. */
    private static final long SEARCH_DEBOUNCE_MS = 300L;

    private static final String TAG_DATE_PICKER = "all_data_date_range_picker";

    /** Item id of the "All ..." entry of the Category and Type popups. */
    private static final int MENU_ALL_ID = 0;

    /** The periods offered by the Date range button. */
    private enum DatePreset {
        ALL(R.string.all_data_date_all),
        TODAY(R.string.all_data_date_today),
        LAST_7_DAYS(R.string.all_data_date_last_7_days),
        LAST_30_DAYS(R.string.all_data_date_last_30_days),
        THIS_MONTH(R.string.all_data_date_this_month),
        CUSTOM(R.string.all_data_date_custom);

        @StringRes
        private final int mLabelRes;

        DatePreset(@StringRes int labelRes) {
            mLabelRes = labelRes;
        }
    }

    private AllDataService mAllDataService;
    private AllDataRenderer mRenderer;

    private final Handler mSearchHandler = new Handler(Looper.getMainLooper());
    private Runnable mPendingSearch = () -> { };
    private AllDataRenderer.RowType mSelectedType = AllDataRenderer.RowType.ALL;
    private String mQuery = "";

    private DatePreset mDatePreset = DatePreset.ALL;
    private long mStartMillis = AllDataRenderer.ALL_DATES_START;
    private long mEndMillis = AllDataRenderer.ALL_DATES_END;
    private String mCategory;
    private String mCategoryLabel;
    private AllDataRenderer.RecordStatus mStatus;

    private MaterialCardView mBtnDateRange;
    private MaterialCardView mBtnCategory;
    private MaterialCardView mBtnType;
    private TextView mTxtDateRange;
    private TextView mTxtCategory;
    private TextView mTxtType;

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
        setupFilterButtons();
        setupSummaryCards();
        setupRecordActions();
        applyFilter();

        BottomNavHelper.setup(this, R.id.nav_menu);
    }

    private void setupFilters() {
        ChipGroup chipGroup = findViewById(R.id.filterChipGroup);
        TextInputEditText etSearch = findViewById(R.id.etDataSearch);

        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            mSelectedType = typeOf(checkedId);
            dropFiltersNotOffered();
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
        int shown = mRenderer.render(new AllDataRenderer.Filter(
                mSelectedType, mQuery, mStartMillis, mEndMillis, mCategory, mStatus));
    }

    // ==================== DATE RANGE / CATEGORY / TYPE ====================

    private void setupFilterButtons() {
        mBtnDateRange = findViewById(R.id.btnDateRange);
        mBtnCategory = findViewById(R.id.btnCategory);
        mBtnType = findViewById(R.id.btnType);
        mTxtDateRange = findViewById(R.id.txtDateRange);
        mTxtCategory = findViewById(R.id.txtCategory);
        mTxtType = findViewById(R.id.txtType);

        mBtnDateRange.setOnClickListener(this::showDateRangeMenu);
        mBtnCategory.setOnClickListener(this::showCategoryMenu);
        mBtnType.setOnClickListener(this::showTypeMenu);
        updateFilterButtons();
    }

    private void showDateRangeMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        Menu menu = popup.getMenu();
        for (DatePreset preset : DatePreset.values()) {
            menu.add(Menu.NONE, preset.ordinal(), preset.ordinal(), preset.mLabelRes);
        }
        menu.setGroupCheckable(Menu.NONE, true, true);
        menu.findItem(mDatePreset.ordinal()).setChecked(true);

        popup.setOnMenuItemClickListener(item -> {
            DatePreset preset = DatePreset.values()[item.getItemId()];
            if (preset == DatePreset.CUSTOM) {
                showCustomDateRangePicker();
            } else {
                applyDatePreset(preset);
            }
            return true;
        });
        popup.show();
    }

    private void applyDatePreset(DatePreset preset) {
        long endOfToday = Common.getEndOfDayInMillis();
        switch (preset) {
            case TODAY -> setDateRange(preset, Common.getStartOfDayDaysAgoInMillis(0), endOfToday);
            case LAST_7_DAYS -> setDateRange(preset, Common.getStartOfDayDaysAgoInMillis(6), endOfToday);
            case LAST_30_DAYS -> setDateRange(preset, Common.getStartOfDayDaysAgoInMillis(29), endOfToday);
            case THIS_MONTH -> setDateRange(preset, Common.getStartOfMonthInMillis(), endOfToday);
            default -> setDateRange(DatePreset.ALL,
                    AllDataRenderer.ALL_DATES_START, AllDataRenderer.ALL_DATES_END);
        }
    }

    /** Opens the Material range picker; future days cannot be picked. */
    private void showCustomDateRangePicker() {
        if (getSupportFragmentManager().findFragmentByTag(TAG_DATE_PICKER) != null) {
            return;
        }
        MaterialDatePicker<Pair<Long, Long>> picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText(R.string.all_data_date_pick_title)
                .setCalendarConstraints(new CalendarConstraints.Builder()
                        .setValidator(DateValidatorPointBackward.now())
                        .build())
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null || selection.first == null || selection.second == null) {
                return;
            }
            // The picker answers with the UTC midnight of each picked day.
            long start = Common.getLocalStartOfDayFromUtcDate(selection.first);
            long end = Common.getEndOfDayInMillis(Common.getLocalStartOfDayFromUtcDate(selection.second));
            setDateRange(DatePreset.CUSTOM, start, end);
        });
        picker.show(getSupportFragmentManager(), TAG_DATE_PICKER);
    }

    private void setDateRange(DatePreset preset, long startMillis, long endMillis) {
        mDatePreset = preset;
        mStartMillis = startMillis;
        mEndMillis = endMillis;
        updateFilterButtons();
        applyFilter();
    }

    /** Lists the categories with records for the selected chip, grouped by record type. */
    private void showCategoryMenu(View anchor) {
        List<AllDataRenderer.CategoryOption> options = mRenderer.getCategoryOptions(mSelectedType);

        PopupMenu popup = new PopupMenu(this, anchor);
        Menu menu = popup.getMenu();
        menu.setGroupDividerEnabled(true);
        menu.add(Menu.NONE, MENU_ALL_ID, 0, R.string.all_data_category_all);
        menu.setGroupCheckable(Menu.NONE, true, true);
        int checkedId = MENU_ALL_ID;
        for (int i = 0; i < options.size(); i++) {
            AllDataRenderer.CategoryOption option = options.get(i);
            // One menu group per record type, so the popup draws a divider between them.
            int groupId = option.type().ordinal() + 1;
            menu.add(groupId, i + 1, i + 1, option.label());
            menu.setGroupCheckable(groupId, true, true);
            if (option.key().equals(mCategory)) {
                checkedId = i + 1;
            }
        }
        menu.findItem(checkedId).setChecked(true);

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == MENU_ALL_ID) {
                mCategory = null;
                mCategoryLabel = null;
            } else {
                AllDataRenderer.CategoryOption option = options.get(item.getItemId() - 1);
                mCategory = option.key();
                mCategoryLabel = option.label();
            }
            updateFilterButtons();
            applyFilter();
            return true;
        });
        popup.show();
    }

    /** Lists the statuses that exist for the selected chip (none for expenses). */
    private void showTypeMenu(View anchor) {
        List<AllDataRenderer.RecordStatus> statuses = mRenderer.getStatusOptions(mSelectedType);

        PopupMenu popup = new PopupMenu(this, anchor);
        Menu menu = popup.getMenu();
        menu.add(Menu.NONE, MENU_ALL_ID, 0, R.string.all_data_type_all);
        for (int i = 0; i < statuses.size(); i++) {
            menu.add(Menu.NONE, i + 1, i + 1, statuses.get(i).labelRes());
        }
        menu.setGroupCheckable(Menu.NONE, true, true);
        int checkedIndex = mStatus == null ? -1 : statuses.indexOf(mStatus);
        menu.findItem(checkedIndex + 1).setChecked(true);

        popup.setOnMenuItemClickListener(item -> {
            mStatus = item.getItemId() == MENU_ALL_ID ? null : statuses.get(item.getItemId() - 1);
            updateFilterButtons();
            applyFilter();
            return true;
        });
        popup.show();
    }

    /**
     * A chip change can leave a category or status that the new type does not
     * have (a bank while Meals is selected, for instance): those are cleared
     * instead of silently emptying the list.
     */
    private void dropFiltersNotOffered() {
        if (mCategory != null) {
            boolean offered = false;
            for (AllDataRenderer.CategoryOption option : mRenderer.getCategoryOptions(mSelectedType)) {
                if (option.key().equals(mCategory)) {
                    offered = true;
                    break;
                }
            }
            if (!offered) {
                mCategory = null;
                mCategoryLabel = null;
            }
        }
        if (mStatus != null && !mRenderer.getStatusOptions(mSelectedType).contains(mStatus)) {
            mStatus = null;
        }
        updateFilterButtons();
    }

    /** Each button shows the option picked, or its own name while nothing is picked. */
    private void updateFilterButtons() {
        if (mDatePreset == DatePreset.ALL) {
            mTxtDateRange.setText(R.string.all_data_date_range);
        } else if (mDatePreset == DatePreset.CUSTOM) {
            mTxtDateRange.setText(getString(R.string.all_data_date_custom_range,
                    Common.getDayMonthFromTimestamp(mStartMillis),
                    Common.getDayMonthFromTimestamp(mEndMillis)));
        } else {
            mTxtDateRange.setText(mDatePreset.mLabelRes);
        }
        mTxtCategory.setText(mCategory == null ? getString(R.string.all_data_category) : mCategoryLabel);
        mTxtType.setText(mStatus == null ? R.string.all_data_type : mStatus.labelRes());

        setFilterButtonActive(mBtnDateRange, mDatePreset != DatePreset.ALL);
        setFilterButtonActive(mBtnCategory, mCategory != null);
        setFilterButtonActive(mBtnType, mStatus != null);
    }

    private void setFilterButtonActive(MaterialCardView button, boolean active) {
        button.setCardBackgroundColor(getColor(active ? R.color.neon_blue_20 : R.color.white_10));
    }

    /** Re-reads the data from the database and repaints the filtered list, so a
     *  record that was just edited or removed disappears from the UI. */
    private void refreshData() {
        mRenderer.setSnapshot(mAllDataService.load(0L, Long.MAX_VALUE));
        applyFilter();
    }

    /** Wires the Edit / Remove actions of the full-data popup to the repositories. */
    private void setupRecordActions() {
        mRenderer.setOnEditRecord(this::editRecord);
        mRenderer.setOnRemoveRecord(this::removeRecord);
    }

    /** Opens the editor of the tapped record, specific to its type. */
    private void editRecord(AllDataRenderer.RowSpec spec) {
        switch (spec.type()) {
            case MEAL -> editMeal(spec);
            case EXPENSE -> editExpense(spec);
            case ROUTINE -> editRoutine(spec);
            default -> { }
        }
    }

    private void editMeal(AllDataRenderer.RowSpec spec) {
        List<String> options = List.of(
                getString(R.string.meal_correct_short),
                getString(R.string.meal_warning_short),
                getString(R.string.meal_wrong_short));
        String currentLabel = spec.value() != null ? spec.value() : "";
        int checkedIndex = 0;
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).equals(currentLabel)) {
                checkedIndex = i;
                break;
            }
        }

        CardRecordDialog.showEditOptionTextDialog(
                this,
                R.string.meal_record_edit_title,
                options,
                checkedIndex,
                R.string.add_an_observation,
                spec.observation() == null ? "" : spec.observation(),
                (statusLabel, observation) -> {
                    if (statusLabel == null) {
                        return Constants.MEAL_SELECTION_REQUIRED;
                    }
                    String status = labelToMealStatus(statusLabel);
                    long id = spec.recordId() != null ? spec.recordId() : -1L;
                    mAllDataService.updateMeal(id, status, observation, spec.timestamp());
                    refreshData();
                    return null;
                });
    }

    private String labelToMealStatus(String label) {
        if (getString(R.string.correct_meal).equals(label)) {
            return Constants.CORRECT_MEAL;
        }
        if (getString(R.string.warning_meal).equals(label)) {
            return Constants.WARNING_MEAL;
        }
        if (getString(R.string.wrong_meal).equals(label)) {
            return Constants.WRONG_MEAL;
        }
        return Constants.OTHER_MEAL;
    }

    private void editExpense(AllDataRenderer.RowSpec spec) {
        // Re-read the expense to get its exact amount (the card shows a formatted value).
        ExpenseRecord expense = findExpenseById(spec.recordId() != null ? spec.recordId().intValue() : -1);
        List<String> banks = getBankOptions();
        int checkedIndex = expense != null && expense.bank() != null
                ? Math.max(0, banks.indexOf(expense.bank())) : 0;

        CardRecordDialog.showEditOptionAmountDialog(
                this,
                R.string.expense_record_edit_title,
                banks,
                checkedIndex,
                R.string.expense_record_amount_hint,
                expense != null ? String.valueOf(expense.amount()) : "",
                android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL,
                (bank, amount) -> {
                    if (bank == null || bank.isBlank()) {
                        return Constants.EXPENSE_BANK_REQUIRED;
                    }
                    double value = parseAmount(amount);
                    if (value <= 0) {
                        return Constants.EXPENSE_INVALID_VALUE;
                    }
                    long id = spec.recordId() != null ? spec.recordId() : -1L;
                    mAllDataService.updateExpense((int) id, value, bank);
                    refreshData();
                    return null;
                });
    }

    private List<String> getBankOptions() {
        List<String> banks = new ArrayList<>();
        var snapshot = mAllDataService.load(0L, Long.MAX_VALUE);
        for (var card : snapshot.cards()) {
            String bank = card.bankName();
            if (bank != null && !bank.isBlank() && !banks.contains(bank)) {
                banks.add(bank);
            }
        }
        if (banks.isEmpty()) {
            banks.addAll(Constants.KNOWN_BANK_LABELS);
        }
        return banks;
    }

    private double parseAmount(String text) {
        if (text == null || !text.matches("\\d{1,7}([.,]\\d{1,2})?")) {
            return -1;
        }
        return Double.parseDouble(text.replace(',', '.'));
    }

    private ExpenseRecord findExpenseById(int id) {
        if (id < 0) {
            return null;
        }
        var snapshot = mAllDataService.load(0L, Long.MAX_VALUE);
        for (ExpenseRecord e : snapshot.expenseRecords()) {
            if (e.id() == id) {
                return e;
            }
        }
        return null;
    }

    private void editRoutine(AllDataRenderer.RowSpec spec) {
        boolean completed = spec.value() != null
                && getString(R.string.completed).equals(spec.value());
        String note = spec.note() == null ? "" : spec.note();
        long id = spec.recordId() != null ? spec.recordId() : -1L;
        mAllDataService.updateTrackerRecord(id, !completed, note);
        refreshData();
    }

    private void removeRecord(AllDataRenderer.RowSpec spec) {
        long id = spec.recordId() != null ? spec.recordId() : -1L;
        CardRecordDialog.confirmRemove(
                this,
                R.string.record_remove_title,
                getString(R.string.record_remove_message, spec.detail() != null ? spec.detail() : ""),
                () -> {
                    switch (spec.type()) {
                        case MEAL -> mAllDataService.deleteMeal(id);
                        case EXPENSE -> mAllDataService.deleteExpense((int) id);
                        case ROUTINE -> mAllDataService.deleteTrackerRecord(id);
                        default -> { }
                    }
                    refreshData();
                });
    }

    @Override
    protected void onDestroy() {
        mSearchHandler.removeCallbacksAndMessages(null);
        mAllDataService.closeDb();
        super.onDestroy();
    }
}