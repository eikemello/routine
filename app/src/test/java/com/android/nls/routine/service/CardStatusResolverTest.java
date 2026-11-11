package com.android.nls.routine.service;

import static org.junit.Assert.assertEquals;
import com.android.nls.routine.model.DayStatus;
import com.android.nls.routine.utils.Constants;
import org.junit.Test;

public class CardStatusResolverTest {

    private static final String C = Constants.CORRECT_MEAL;
    private static final String W = Constants.WARNING_MEAL;
    private static final String X = Constants.WRONG_MEAL;

    @Test
    public void water_followsTheGoal() {
        assertEquals(DayStatus.NONE, CardStatusResolver.water(0, 3000));
        assertEquals(DayStatus.YELLOW, CardStatusResolver.water(1750, 3000));
        assertEquals(DayStatus.GREEN, CardStatusResolver.water(3000, 3000));
        assertEquals(DayStatus.NONE, CardStatusResolver.water(500, 0));
    }

    @Test
    public void meals_isInProgressUntilTheFourAreLogged() {
        assertEquals(DayStatus.NONE, CardStatusResolver.meals(new String[]{null, null, null, null}));
        assertEquals(DayStatus.YELLOW, CardStatusResolver.meals(new String[]{C, C, C, null}));
    }

    @Test
    public void meals_isScoredLikeTheDayOnceComplete() {
        assertEquals(DayStatus.GREEN, CardStatusResolver.meals(new String[]{C, C, C, C}));
        assertEquals(DayStatus.YELLOW, CardStatusResolver.meals(new String[]{C, C, W, X}));
        assertEquals(DayStatus.RED, CardStatusResolver.meals(new String[]{C, W, X, X}));
    }

    @Test
    public void spending_followsTheLimit() {
        assertEquals(DayStatus.NONE, CardStatusResolver.spending(0, 2000));
        assertEquals(DayStatus.GREEN, CardStatusResolver.spending(1000, 2000));
        assertEquals(DayStatus.YELLOW, CardStatusResolver.spending(1600, 2000));
        assertEquals(DayStatus.RED, CardStatusResolver.spending(2100, 2000));
        assertEquals(DayStatus.NONE, CardStatusResolver.spending(500, 0));
    }
}
