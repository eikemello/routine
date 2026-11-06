package com.android.nls.routine.service;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import com.android.nls.routine.R;
import com.android.nls.routine.cardhistory.CardRecordDialog;
import com.android.nls.routine.model.AllDataSnapshot;
import com.android.nls.routine.model.CreditCard;
import com.android.nls.routine.model.ExpenseRecord;
import com.android.nls.routine.model.MealRecord;
import com.android.nls.routine.model.Tracker;
import com.android.nls.routine.model.TrackerRecord;
import com.android.nls.routine.model.TrackerType;
import com.android.nls.routine.model.WaterRecord;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the single table of the All data screen: every record (water, meals,
 * expenses and routines) merged into one timeline, newest first and grouped
 * under day headers, followed by the tracker definitions and the settings
 * under their own headers. Nothing is split into cards. Everything is first
 * converted to one row model ({@link RowSpec}), and a filter - a type from
 * the chip row plus a free-text query from the search box - is applied over
 * that model, so new filters only need to plug into
 * {@link #matches(RowSpec, RowType, String)}.
 */
public class AllDataRenderer {

    /** The rows the list can hold. ALL is only a filter selection. */
    public enum RowType {
        ALL, WATER, MEAL, EXPENSE, ROUTINE, TRACKER, CONFIG
    }

    /**
     * One cell of the table, whatever its origin (settings cells have no time).
     * The optional amount is the raw number behind the formatted value - the
     * ml of a water record and the money of an expense - so day headers can
     * total it up; the other rows leave it null. The optional observation is
     * the second line of the row, between the detail and the time - the meal
     * note and the expense description. The category key (see
     * {@link #getCategoryOptions}) and the status feed the Category and Type
     * filters; rows without them (settings, trackers, water category) leave
     * them null.
     */
    public record RowSpec(RowType type, long timestamp, String detail, String value,
                           Integer valueColor, String note, RowIcon icon, Double amount,
                           String observation, Long recordId, String category,
                           RecordStatus status) {
        /** The rows whose icon follows their type skip the icon and the amount. */
        RowSpec(RowType type, long timestamp, String detail, String value,
                Integer valueColor, String note) {
            this(type, timestamp, detail, value, valueColor, note, null, null, null, null, null, null);
        }

        /** The rows that carry an icon but no amount (trackers). */
        RowSpec(RowType type, long timestamp, String detail, String value,
                Integer valueColor, String note, RowIcon icon) {
            this(type, timestamp, detail, value, valueColor, note, icon, null, null, null, null, null);
        }
    }

    /** The status a record can be filtered by in the Type button. */
    public enum RecordStatus {
        CORRECT(R.string.correct_meal),
        WARNING(R.string.warning_meal),
        WRONG(R.string.wrong_meal),
        OTHER(R.string.other),
        COMPLETED(R.string.completed),
        NOT_COMPLETED(R.string.not_completed);

        private final int mLabelRes;

        RecordStatus(int labelRes) {
            mLabelRes = labelRes;
        }

        public int labelRes() {
            return mLabelRes;
        }
    }

    /** One entry of the Category popup: the key matched against the rows and its label. */
    public record CategoryOption(RowType type, String key, String label) {
    }

    /**
     * Every filter of the screen at once. The period is inclusive on both
     * ends; {@link #ALL_DATES_START}/{@link #ALL_DATES_END} mean no period.
     * A null category or status means "any".
     */
    public record Filter(RowType type, String query, long startMillis, long endMillis,
                         String category, RecordStatus status) {

        /** True when a filter that only applies to records (date, category, status) is set. */
        boolean narrowsRecords() {
            return startMillis > ALL_DATES_START || endMillis < ALL_DATES_END
                    || category != null || status != null;
        }
    }

    public static final long ALL_DATES_START = 0L;
    public static final long ALL_DATES_END = Long.MAX_VALUE;

    /** Category keys are prefixed by their row type so a bank never clashes with a meal. */
    private static final String CATEGORY_SEPARATOR = ":";
    private static final String OTHER_MEALS_KEY = "__OTHER__";
    private static final List<String> REGULAR_MEALS =
            List.of(Constants.BREAKFAST, Constants.LUNCH, Constants.TEA, Constants.DINNER);

    /**
     * The type icon of a row, shaped like the home cards: the glyph, the
     * colored box behind it and the tint of the glyph. Routine and tracker
     * rows carry their own (the tracker decides all three); the other rows
     * fall back to the icon of their row type.
     */
    private record RowIcon(@DrawableRes int imageRes, @DrawableRes int backgroundRes,
                           @ColorRes int tintRes) {
    }

    /** The status label (string) and color of a meal record. */
    private record MealStatus(int label, int color) {
    }

    private final Context mContext;
    private final LinearLayout mContainer;
    private AllDataSnapshot mSnapshot;
    private final LayoutInflater mInflater;

    public AllDataRenderer(Context context, LinearLayout container, AllDataSnapshot snapshot) {
        mContext = context;
        mContainer = container;
        mSnapshot = snapshot;
        mInflater = LayoutInflater.from(context);
    }

    /** Callback fired when the user taps a cell holding a record that can be edited. */
    public interface OnRecordAction {
        void on(RowSpec spec);
    }

    private OnRecordAction mOnEditRecord;
    private OnRecordAction mOnRemoveRecord;

    public void setOnEditRecord(OnRecordAction onEditRecord) {
        mOnEditRecord = onEditRecord;
    }

    public void setOnRemoveRecord(OnRecordAction onRemoveRecord) {
        mOnRemoveRecord = onRemoveRecord;
    }

    /** Replaces the snapshot the renderer draws from, so edits and removals are reflected. */
    public void setSnapshot(AllDataSnapshot snapshot) {
        mSnapshot = snapshot;
    }

    /**
     * Two cells per table row. Rows wrap their content, so a day with six
     * items shows as two columns by three rows, a day with five as two
     * columns with a lone cell last, and so on.
     */
    private static final int GRID_COLUMNS = 2;

    /**
     * Rebuilds the whole list for the given filters and returns how many rows
     * were shown (the screen puts it in the result count of the header).
     */
    public int render(Filter filter) {
        mContainer.removeAllViews();

        String query = filter.query();
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.getDefault());
        Filter normalized = new Filter(filter.type(), normalizedQuery, filter.startMillis(),
                filter.endMillis(), filter.category(), filter.status());

        List<RowSpec> recordSpecs = collectRecordSpecs();

        List<RowSpec> trackerSpecs = new ArrayList<>();
        collectTrackerSpecs(trackerSpecs);

        List<RowSpec> configSpecs = new ArrayList<>();
        collectConfigSpecs(configSpecs);

        List<RowSpec> records = filter(recordSpecs, normalized);
        List<RowSpec> trackers = filter(trackerSpecs, normalized);
        List<RowSpec> config = filter(configSpecs, normalized);
        RowType selectedType = filter.type();

        records.sort(Comparator.comparingLong(RowSpec::timestamp).reversed());

        int shown = 0;
        long lastDay = Long.MIN_VALUE;
        List<RowSpec> pending = new ArrayList<>();
        for (RowSpec spec : records) {
            long day = Common.getStartOfDayInMillis(spec.timestamp());
            if (day != lastDay) {
                addGridRows(pending);
                pending.clear();
                String dailySummary = computeDailySummary(day, records);
                addHeader(Common.getDateWithYearFromTimestamp(day), dailySummary);
                lastDay = day;
            }
            pending.add(spec);
            shown++;
        }
        addGridRows(pending);
        pending.clear();

        if (!trackers.isEmpty()) {
            addHeader(getString(R.string.trackers), null);
            shown += addGridRows(trackers);
        }
        if (!config.isEmpty()) {
            addHeader(getString(R.string.config), null);
            shown += addGridRows(config);
        }

        if (shown == 0) {
            addEmptyView(normalizedQuery.isEmpty() && selectedType == RowType.ALL
                    && !normalized.narrowsRecords());
        }
        return shown;
    }

    /**
     * The categories the Category button offers for the selected chip: the
     * meal slots (regular ones in slot order, then the irregular meals as one
     * "Other meals" entry), the banks of the expenses and the routines - only
     * the ones that have at least one record, so the popup never offers an
     * empty result.
     */
    public List<CategoryOption> getCategoryOptions(RowType selectedType) {
        Map<String, CategoryOption> byKey = new LinkedHashMap<>();
        for (RowSpec spec : collectRecordSpecs()) {
            if (spec.category() == null
                    || (selectedType != RowType.ALL && spec.type() != selectedType)) {
                continue;
            }
            byKey.putIfAbsent(spec.category(),
                    new CategoryOption(spec.type(), spec.category(), categoryLabel(spec)));
        }

        List<CategoryOption> options = new ArrayList<>(byKey.values());
        options.sort(Comparator
                .comparingInt((CategoryOption option) -> option.type().ordinal())
                .thenComparingInt(this::categoryOrder)
                .thenComparing(option -> option.label().toLowerCase(Locale.getDefault())));
        return options;
    }

    /** The statuses the Type button offers for the selected chip (expenses have none). */
    public List<RecordStatus> getStatusOptions(RowType selectedType) {
        List<RecordStatus> meal = List.of(RecordStatus.CORRECT, RecordStatus.WARNING,
                RecordStatus.WRONG, RecordStatus.OTHER);
        List<RecordStatus> done = List.of(RecordStatus.COMPLETED, RecordStatus.NOT_COMPLETED);
        return switch (selectedType) {
            case MEAL -> meal;
            case WATER, ROUTINE -> done;
            case ALL -> {
                List<RecordStatus> all = new ArrayList<>(meal);
                all.addAll(done);
                yield all;
            }
            default -> List.of();
        };
    }

    /** Regular meal slots keep their order of the day; every other category sorts by label. */
    private int categoryOrder(CategoryOption option) {
        if (option.type() != RowType.MEAL) {
            return 0;
        }
        String slot = option.key().substring(option.key().indexOf(CATEGORY_SEPARATOR) + 1);
        int index = REGULAR_MEALS.indexOf(slot);
        return index >= 0 ? index : REGULAR_MEALS.size();
    }

    private String categoryLabel(RowSpec spec) {
        if (spec.type() == RowType.MEAL && spec.category().endsWith(OTHER_MEALS_KEY)) {
            return getString(R.string.all_data_category_other_meals);
        }
        if (spec.type() == RowType.EXPENSE && (spec.detail() == null || spec.detail().isBlank()
                || spec.category().endsWith(CATEGORY_SEPARATOR))) {
            return getString(R.string.all_data_category_no_bank);
        }
        return spec.detail();
    }

    private static String categoryKey(RowType type, String value) {
        return type.name() + CATEGORY_SEPARATOR + (value == null ? "" : value);
    }

    private List<RowSpec> collectRecordSpecs() {
        List<RowSpec> recordSpecs = new ArrayList<>();
        collectWaterSpecs(recordSpecs);
        collectMealSpecs(recordSpecs);
        collectExpenseSpecs(recordSpecs);
        collectRoutineSpecs(recordSpecs);
        return recordSpecs;
    }

    /**
     * The one-line summary at the right of a day header: the totals of the
     * rows of that day that survived the filters - water in ml, meals logged,
     * money spent and routines completed - joined by " | ", skipping the
     * types not present. Null hides the summary line.
     */
    private String computeDailySummary(long day, List<RowSpec> records) {
        double waterMl = 0;
        int meals = 0;
        double spent = 0;
        int routinesDone = 0;
        int routines = 0;

        for (RowSpec spec : records) {
            if (Common.getStartOfDayInMillis(spec.timestamp()) != day) {
                continue;
            }
            switch (spec.type()) {
                case WATER -> waterMl += spec.amount() != null ? spec.amount() : 0;
                case MEAL -> meals++;
                case EXPENSE -> spent += spec.amount() != null ? spec.amount() : 0;
                case ROUTINE -> {
                    routines++;
                    if (getString(R.string.completed).equals(spec.value())) {
                        routinesDone++;
                    }
                }
                default -> { }
            }
        }

        List<String> parts = new ArrayList<>();
        if (waterMl > 0) {
            parts.add(getString(R.string.daily_water_ml, waterMl));
        }
        if (meals > 0) {
            parts.add(getString(R.string.meals) + " " + meals);
        }
        if (spent > 0) {
            parts.add(getString(R.string.expense_sum, spent));
        }
        if (routines > 0) {
            parts.add(getString(R.string.routines) + " " + routinesDone + "/" + routines);
        }
        return parts.isEmpty() ? null : String.join(" | ", parts);
    }

    // ==================== ROW COLLECTION ====================

    /**
     * One row per day, not one per input: the water records are summed by
     * day, so a day with five adds shows a single "1250 ml" line instead of
     * "+100", "+250", ... The row carries the start of the day as its
     * timestamp, so it groups under its own day header.
     */
    private void collectWaterSpecs(List<RowSpec> specs) {
        Map<Long, Integer> totalByDay = new LinkedHashMap<>();
        for (WaterRecord record : mSnapshot.waterRecords()) {
            long day = Common.getStartOfDayInMillis(record.timestamp());
            totalByDay.merge(day, record.amount(), Integer::sum);
        }
        for (Map.Entry<Long, Integer> entry : totalByDay.entrySet()) {
            int total = entry.getValue();
            boolean completed = mSnapshot.dailyWaterGoal() > 0 && total >= mSnapshot.dailyWaterGoal();
            String value = getString(completed ? R.string.completed : R.string.not_completed);
            int color = mContext.getColor(completed ? R.color.green_dark : R.color.text_secondary);
            specs.add(new RowSpec(RowType.WATER, entry.getKey(),
                    getString(R.string.daily_water_ml, (double) total), value, color, null,
                    null, (double) total, null, null, null,
                    completed ? RecordStatus.COMPLETED : RecordStatus.NOT_COMPLETED));
        }
    }

    private void collectMealSpecs(List<RowSpec> specs) {
        for (MealRecord record : mSnapshot.mealRecords()) {
            MealStatus status = mealStatus(record.status());
            boolean regular = !Constants.OTHER_MEAL.equals(record.status())
                    && REGULAR_MEALS.contains(record.meal());
            String category = categoryKey(RowType.MEAL, regular ? record.meal() : OTHER_MEALS_KEY);
            specs.add(new RowSpec(RowType.MEAL, record.timestamp(), record.meal(),
                    getString(status.label()), mContext.getColor(status.color()), null,
                    null, null, observationOf(record), record.id(), category,
                    recordStatusOfMeal(record.status())));
        }
    }

    private void collectExpenseSpecs(List<RowSpec> specs) {
        for (ExpenseRecord record : mSnapshot.expenseRecords()) {
            String description = expenseDescriptionOf(record);
            boolean hasDescription = description != null && !description.isBlank();
            boolean hasBank = record.bank() != null && !record.bank().isBlank();
            // The bank is the detail and the description is the observation;
            // when the bank is missing the description stays the detail, so it
            // is not repeated as the observation.
            String detail = hasBank ? record.bank() : (hasDescription ? description : "");
            String observation = hasBank && hasDescription ? description : null;
            specs.add(new RowSpec(RowType.EXPENSE, record.timestamp(), detail,
                    getString(R.string.expense_sum, record.amount()), null, null, null,
                    record.amount(), observation, (long) record.id(),
                    categoryKey(RowType.EXPENSE, hasBank ? record.bank().trim() : null), null));
        }
    }

    private void collectRoutineSpecs(List<RowSpec> specs) {
        for (TrackerRecord record : mSnapshot.routineRecords()) {
            Tracker tracker = trackerFor(record.type());
            String detail = tracker != null ? tracker.name() : record.type().name();
            String value = getString(record.completed() ? R.string.completed : R.string.not_completed);
            int color = mContext.getColor(record.completed() ? R.color.green_dark : R.color.text_secondary);
            String note = record.note() != null && !record.note().isBlank() ? record.note() : null;
            specs.add(new RowSpec(RowType.ROUTINE, record.timestamp(), detail, value, color, note,
                    iconForTracker(record.type()), null, null, record.id(),
                    categoryKey(RowType.ROUTINE, record.type().name()),
                    record.completed() ? RecordStatus.COMPLETED : RecordStatus.NOT_COMPLETED));
        }
    }

    private void collectTrackerSpecs(List<RowSpec> specs) {
        for (Tracker tracker : mSnapshot.trackers()) {
            String value = getString(tracker.enabled() ? R.string.all_data_enabled : R.string.all_data_disabled);
            int color = mContext.getColor(tracker.enabled() ? R.color.green_dark : R.color.text_tertiary);
            String note = tracker.description() != null && !tracker.description().isBlank()
                    ? tracker.description() : null;
            specs.add(new RowSpec(RowType.TRACKER, 0L, tracker.name(), value, color, note,
                    iconForTracker(tracker.type())));
        }
    }

    private void collectConfigSpecs(List<RowSpec> specs) {
        specs.add(new RowSpec(RowType.CONFIG, 0L, getString(R.string.daily_goal),
                getString(R.string.daily_water_ml, mSnapshot.dailyWaterGoal()), null, null));
        specs.add(new RowSpec(RowType.CONFIG, 0L, getString(R.string.button_1),
                getString(R.string.daily_water_ml, mSnapshot.btn1Value()), null, null));
        specs.add(new RowSpec(RowType.CONFIG, 0L, getString(R.string.button_2),
                getString(R.string.daily_water_ml, mSnapshot.btn2Value()), null, null));
        specs.add(new RowSpec(RowType.CONFIG, 0L, getString(R.string.button_3),
                getString(R.string.daily_water_ml, mSnapshot.btn3Value()), null, null));
        specs.add(new RowSpec(RowType.CONFIG, 0L, getString(R.string.limit),
                getString(R.string.expense_sum, mSnapshot.monthlyLimit()), null, null));
        specs.add(new RowSpec(RowType.CONFIG, 0L, getString(R.string.card_statement_closing),
                getString(R.string.card_statement_closing_date_init_value, mSnapshot.statementClosingDay()),
                null, null));

        if (mSnapshot.cards().isEmpty()) {
            specs.add(new RowSpec(RowType.CONFIG, 0L, getString(R.string.all_data_no_cards), null, null, null));
            return;
        }
        for (CreditCard card : mSnapshot.cards()) {
            String bank = card.bankName() != null ? card.bankName() : "";
            String lastFour = card.lastFour() != null && !card.lastFour().isBlank()
                    ? " •••• " + card.lastFour() : "";
            String note = getString(R.string.statement_closing_day) + " "
                    + getString(R.string.card_closing_day_value, card.closingDay());
            specs.add(new RowSpec(RowType.CONFIG, 0L, bank + lastFour, null, null, note));
        }
    }
    // ==================== FILTERING ====================

    private List<RowSpec> filter(List<RowSpec> specs, Filter filter) {
        List<RowSpec> result = new ArrayList<>();
        for (RowSpec spec : specs) {
            if (matches(spec, filter)) {
                result.add(spec);
            }
        }
        return result;
    }

    /**
     * The single place a filter decision is made: the type chip (ALL accepts
     * every row - the tracker definitions and settings only show up under it),
     * the period, category and status of the record (the tracker definitions
     * and settings have none, so they hide while any of them is set) and the
     * free-text query over the detail, the value, the observation and the note
     * of the row. Future filters (amount range, ...) plug in here.
     */
    private boolean matches(RowSpec spec, Filter filter) {
        if (filter.type() != RowType.ALL && spec.type() != filter.type()) {
            return false;
        }
        boolean isRecord = spec.type() != RowType.TRACKER && spec.type() != RowType.CONFIG;
        if (!isRecord && filter.narrowsRecords()) {
            return false;
        }
        if (isRecord) {
            if (spec.timestamp() < filter.startMillis() || spec.timestamp() > filter.endMillis()) {
                return false;
            }
            if (filter.category() != null && !filter.category().equals(spec.category())) {
                return false;
            }
            if (filter.status() != null && filter.status() != spec.status()) {
                return false;
            }
        }
        String query = filter.query();
        if (query.isEmpty()) {
            return true;
        }
        return contains(spec.detail(), query) || contains(spec.value(), query)
                || contains(spec.observation(), query) || contains(spec.note(), query);
    }

    private boolean contains(String text, String query) {
        return text != null && text.toLowerCase(Locale.getDefault()).contains(query);
    }

    // ==================== ROWS ====================

    /** The plain group header (day, trackers or config), same style for all. */
    private void addHeader(String title, String summary) {
        View header = android.view.LayoutInflater.from(mContext).inflate(R.layout.item_all_data_day_header, mContainer, false);
        TextView txtDate = header.findViewById(R.id.txtDayDate);
        TextView txtSummary = header.findViewById(R.id.txtDaySummary);

        txtDate.setText(title);
        if (summary != null) {
            txtSummary.setText(summary);
            txtSummary.setVisibility(View.VISIBLE);
        } else {
            txtSummary.setVisibility(View.GONE);
        }
        mContainer.addView(header);
    }

    /** The items of one group as a two-column table: pairs of RowSpecs side by
     * side, rows sized by their content (wrap_content), a lone last item
     * taking the left cell only. Returns how many items were shown.
     */
    private int addGridRows(List<RowSpec> specs) {
        if (specs.isEmpty()) {
            return 0;
        }
        TableLayout table = new TableLayout(mContext);
        table.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        table.setStretchAllColumns(true);
        table.setShrinkAllColumns(true);
        int gapPx = Common.dpToPx(mContext, 8);

        TableRow current = null;
        for (int i = 0; i < specs.size(); i++) {
            if (i % GRID_COLUMNS == 0) {
                current = new TableRow(mContext);
                current.setLayoutParams(new TableLayout.LayoutParams(
                        TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
                if (i > 0) {
                    current.setPadding(0, gapPx, 0, 0);
                }
                table.addView(current);
            }
            View cell = buildCell(specs.get(i));
            TableRow.LayoutParams cellParams = new TableRow.LayoutParams(0,
                    TableRow.LayoutParams.WRAP_CONTENT, 1f);
            if (i % GRID_COLUMNS == 1) {
                cellParams.leftMargin = gapPx;
            }
            current.addView(cell, cellParams);
        }
        // A lone last item leaves the right cell empty: fill it so the left
        // cell keeps half the width instead of stretching full width.
        if (specs.size() % GRID_COLUMNS == 1 && current != null) {
            View filler = new View(mContext);
            TableRow.LayoutParams fillerParams = new TableRow.LayoutParams(0,
                    TableRow.LayoutParams.WRAP_CONTENT, 1f);
            fillerParams.leftMargin = gapPx;
            current.addView(filler, fillerParams);
        }

        mContainer.addView(table);
        // Cells of the same row share the height of the tallest one, so a short
        // water/routine card is not stranded with blank space next to a tall
        // meal/expense card. Measured after layout so it adapts to content -
        // never a hardcoded size (descriptions are free-form text).
        table.post(() -> equalizeRowHeights(table));
        return specs.size();
    }

    /** Makes every cell of each table row as tall as the tallest cell of that
     *  row, measured at layout time. Rows keep adapting to their content; only
     *  the cells inside one row are leveled to each other. */
    private void equalizeRowHeights(TableLayout table) {
        int rowCount = table.getChildCount();
        for (int r = 0; r < rowCount; r++) {
            View row = table.getChildAt(r);
            if (!(row instanceof TableRow)) {
                continue;
            }
            TableRow tableRow = (TableRow) row;
            int maxHeight = 0;
            int cellCount = tableRow.getChildCount();
            for (int c = 0; c < cellCount; c++) {
                View cell = tableRow.getChildAt(c);
                int h = cell.getMeasuredHeight();
                if (h > maxHeight) {
                    maxHeight = h;
                }
            }
            if (maxHeight <= 0) {
                continue;
            }
            for (int c = 0; c < cellCount; c++) {
                View cell = tableRow.getChildAt(c);
                TableRow.LayoutParams params = (TableRow.LayoutParams) cell.getLayoutParams();
                if (params == null) {
                    continue;
                }
                params.height = maxHeight;
                cell.setLayoutParams(params);
            }
        }
    }

    /**
     * One grid cell: the same content as the old full-width row (type icon,
     * detail, observation, time/note, value chip) but without the trailing
     * arrow and divider, packed for half the width.
     */
    private View buildCell(RowSpec spec) {
        View cell = mInflater.inflate(R.layout.item_all_data_grid_cell, null, false);
        ImageView imgIcon = cell.findViewById(R.id.imgCellIcon);
        TextView txtTime = cell.findViewById(R.id.txtCellTime);
        TextView txtDetail = cell.findViewById(R.id.txtCellDetail);
        TextView txtObservation = cell.findViewById(R.id.txtCellObservation);
        TextView txtStatus = cell.findViewById(R.id.txtCellStatus);
        TextView txtNote = cell.findViewById(R.id.txtCellNote);

        RowIcon icon = iconFor(spec);
        imgIcon.setImageResource(icon.imageRes());
        imgIcon.setBackgroundResource(icon.backgroundRes());
        imgIcon.setImageTintList(ColorStateList.valueOf(mContext.getColor(icon.tintRes())));

        boolean hasTime = spec.type() != RowType.TRACKER && spec.type() != RowType.CONFIG;
        if (hasTime) {
            txtTime.setText(Common.getTimeFromTimestamp(spec.timestamp()));
        } else {
            txtTime.setVisibility(View.GONE);
        }

        txtDetail.setText(spec.detail() == null ? "" : spec.detail());

        if (spec.observation() == null || spec.observation().isBlank()) {
            txtObservation.setVisibility(View.GONE);
        } else {
            txtObservation.setText(spec.observation());
            txtObservation.setVisibility(View.VISIBLE);
        }

        if (spec.value() == null) {
            txtStatus.setVisibility(View.GONE);
        } else {
            txtStatus.setText(spec.value());
            int valueColor = spec.valueColor() != null
                    ? spec.valueColor()
                    : mContext.getColor(R.color.text_secondary);
            txtStatus.setTextColor(valueColor);
            txtStatus.setBackgroundTintList(ColorStateList.valueOf(
                    (valueColor & 0x00FFFFFF) | 0x33000000));
            txtStatus.setVisibility(View.VISIBLE);
        }

        if (spec.note() == null) {
            txtNote.setVisibility(View.GONE);
        } else {
            txtNote.setText(spec.note());
            txtNote.setVisibility(View.VISIBLE);
        }

        // Tapping a cell that holds a single record opens its full data (with the
        // optional Edit / Remove actions). Water, trackers and config have no
        // single underlying record, so they stay non-clickable.
        final boolean canEdit = spec.recordId() != null;
        cell.setClickable(canEdit);
        cell.setFocusable(canEdit);
        cell.setOnClickListener(v -> {
            if (canEdit) {
                showRecordDetails(spec);
            }
        });

        return cell;
    }

    /** Shows the full data of the tapped record: its fields in full plus the
     *  optional Edit / Remove actions that fall back to the callbacks wired by
     *  the activity. The description/note that is ellipsized in the card is
     *  shown here in full. */
    private void showRecordDetails(RowSpec spec) {
        List<CardRecordDialog.DetailField> fields = new ArrayList<>();
        fields.add(new CardRecordDialog.DetailField(
                mContext.getString(R.string.date), Common.getDateWithYearFromTimestamp(spec.timestamp())));

        boolean hasTime = spec.type() != RowType.TRACKER && spec.type() != RowType.CONFIG;
        if (hasTime) {
            fields.add(new CardRecordDialog.DetailField(
                    mContext.getString(R.string.time), Common.getTimeFromTimestamp(spec.timestamp())));
        }

        if (spec.detail() != null && !spec.detail().isBlank()) {
            fields.add(new CardRecordDialog.DetailField(
                    mContext.getString(R.string.detail), spec.detail()));
        }

        if (spec.value() != null) {
            fields.add(new CardRecordDialog.DetailField(
                    mContext.getString(R.string.value), spec.value()));
        }

        if (spec.observation() != null && !spec.observation().isBlank()) {
            fields.add(new CardRecordDialog.DetailField(
                    mContext.getString(R.string.description), spec.observation()));
        }

        if (spec.note() != null && !spec.note().isBlank()) {
            fields.add(new CardRecordDialog.DetailField(
                    mContext.getString(R.string.note), spec.note()));
        }

        CardRecordDialog.showRecordDetails(
                mContext,
                R.string.record_details_title,
                fields,
                mOnEditRecord != null ? () -> mOnEditRecord.on(spec) : null,
                mOnRemoveRecord != null ? () -> mOnRemoveRecord.on(spec) : null);
    }

    private void addEmptyView(boolean noFilters) {
        TextView empty = new TextView(mContext);
        empty.setText(noFilters ? R.string.all_data_no_records : R.string.all_data_no_matches);
        empty.setTextColor(mContext.getColor(R.color.text_tertiary));
        empty.setTextSize(14f);
        empty.setPadding(0, Common.dpToPx(mContext, 24), 0, 0);
        mContainer.addView(empty);
    }

    /**
     * The icon of a row: the one it carries when it has one (routine records
     * and tracker definitions take the identity of their tracker), or the
     * icon of the row type otherwise - the same glyph, box and tint as the
     * matching home card.
     */
    private RowIcon iconFor(RowSpec spec) {
        if (spec.icon() != null) {
            return spec.icon();
        }
        return switch (spec.type()) {
            case WATER -> new RowIcon(R.drawable.ic_water, R.drawable.icon_background_blue, R.color.neon_blue);
            case MEAL -> new RowIcon(R.drawable.ic_meal, R.drawable.icon_background_green, R.color.green_dark);
            case EXPENSE -> new RowIcon(R.drawable.ic_credit_card, R.drawable.icon_background_orange,
                    R.color.neon_orange);
            case CONFIG -> new RowIcon(R.drawable.ic_config, R.drawable.icon_background, R.color.text_secondary);
            default -> new RowIcon(R.drawable.ic_card_track, R.drawable.icon_background, R.color.text_secondary);
        };
    }

    /** The identity of a tracker (glyph, colored box and tint) as its card shows it. */
    private RowIcon iconForTracker(TrackerType type) {
        return switch (type) {
            case WATER -> new RowIcon(R.drawable.ic_water, R.drawable.icon_background_blue, R.color.neon_blue);
            case MEALS -> new RowIcon(R.drawable.ic_meal, R.drawable.icon_background_green, R.color.green_dark);
            case EXPENSES -> new RowIcon(R.drawable.ic_credit_card, R.drawable.icon_background_orange,
                    R.color.neon_orange);
            case WORKOUT -> new RowIcon(R.drawable.ic_workout, R.drawable.icon_background_purple, R.color.neon_purple);
            case MEDICATION -> new RowIcon(R.drawable.ic_medication, R.drawable.icon_background_pink, R.color.neon_pink);
            case SUPPLEMENT -> new RowIcon(R.drawable.ic_supplement, R.drawable.icon_background_cyan, R.color.neon_cyan);
        };
    }
    // ==================== HELPERS ====================

    private String getString(int resId, Object... args) {
        return mContext.getString(resId, args);
    }

    private Tracker trackerFor(TrackerType type) {
        for (Tracker tracker : mSnapshot.trackers()) {
            if (tracker.type() == type) {
                return tracker;
            }
        }
        return null;
    }

    private MealStatus mealStatus(String status) {
        if (Constants.CORRECT_MEAL.equals(status)) {
            return new MealStatus(R.string.correct_meal, R.color.green_dark);
        }
        if (Constants.WARNING_MEAL.equals(status)) {
            return new MealStatus(R.string.warning_meal, R.color.yellow_dark);
        }
        if (Constants.WRONG_MEAL.equals(status)) {
            return new MealStatus(R.string.wrong_meal, R.color.red_dark);
        }
        if (Constants.OTHER_MEAL.equals(status)) {
            return new MealStatus(R.string.other, R.color.text_secondary);
        }
        return new MealStatus(R.string.other, R.color.text_tertiary);
    }

    private RecordStatus recordStatusOfMeal(String status) {
        if (Constants.CORRECT_MEAL.equals(status)) {
            return RecordStatus.CORRECT;
        }
        if (Constants.WARNING_MEAL.equals(status)) {
            return RecordStatus.WARNING;
        }
        if (Constants.WRONG_MEAL.equals(status)) {
            return RecordStatus.WRONG;
        }
        return RecordStatus.OTHER;
    }

    /**
     * The observation of the meal, or null when there is none: the widget
     * marker is never shown as a note - it reads as an empty observation
     * everywhere else too.
     */
    private String observationOf(MealRecord record) {
        String observation = record.observation();
        if (observation == null || observation.isBlank()
                || Constants.WIDGET_MEAL_OBSERVATION.equals(observation)) {
            return null;
        }
        return observation;
    }

    /**
     * The merchant description of an expense. EXPENSE_TEXT stores the whole
     * {@code Expense{...}} dump (see {@code ExpenseRepository.insertExpense}),
     * so only the {@code description='...'} part is shown - never the amount,
     * the bank or the timestamp. Anything that is not in that shape (a null
     * text, a null description or a manually added expense) reads as no
     * description.
     */
    private String expenseDescriptionOf(ExpenseRecord record) {
        String raw = record.description();
        if (raw == null) {
            return null;
        }
        int start = raw.indexOf("description='");
        if (start < 0) {
            return null;
        }
        start += "description='".length();
        int end = raw.indexOf("'", start);
        if (end < 0) {
            return null;
        }
        String description = raw.substring(start, end).trim();
        if (description.isEmpty() || "null".equals(description)) {
            return null;
        }
        return description;
    }

}