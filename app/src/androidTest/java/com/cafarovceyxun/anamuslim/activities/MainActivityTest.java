package com.cafarovceyxun.anamuslim.activities;

import static org.junit.Assert.assertEquals;

import androidx.lifecycle.Lifecycle;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Smoke test: MainActivity starts and reaches RESUMED.
 *
 * <p>Used to assert a `viewPager` id from the View-era upstream app. The app is fully Compose, so
 * that id stopped existing and this file quietly made the whole androidTest source set
 * uncompilable — no instrumentation test could run at all.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Rule
    public ActivityScenarioRule<MainActivity> scenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void testActivityLaunches() {
        assertEquals(Lifecycle.State.RESUMED, scenarioRule.getScenario().getState());
    }
}
