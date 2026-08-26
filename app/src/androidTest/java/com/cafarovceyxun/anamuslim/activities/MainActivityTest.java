package com.cafarovceyxun.anamuslim.activities;

import static org.junit.Assert.assertNotNull;

import android.app.Instrumentation;
import android.content.Intent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Smoke test: MainActivity starts and stays up.
 *
 * <p>Two earlier versions of this file were broken in ways nothing else caught. It first asserted a
 * `viewPager` id from the View-era upstream app; the app is fully Compose, so that id stopped
 * existing and the whole androidTest source set quietly became uncompilable — no instrumentation
 * test could run at all. It was then rewritten on `ActivityScenarioRule`, which asserts RESUMED but
 * does not support non-standard launch modes: `MainActivity` is `singleTask` (the S Pen remote
 * action and the widget intents need it), so the scenario never got past CREATED and the test
 * failed on a healthy app.
 *
 * <p>Launching through the real intent path is what a `singleTask` activity supports, so that is
 * what this does. The assertion stays deliberately narrow: with `singleTask`, if an instance is
 * already running — which it is whenever the suite runs after someone used the app — the framework
 * routes the intent to that instance and immediately finishes the new one, so `isFinishing()` says
 * nothing about health. What this test really guards is that `onCreate` completes without throwing,
 * because `startActivitySync` propagates a crash there.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Test
    public void testActivityLaunches() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();

        Intent intent = new Intent(instrumentation.getTargetContext(), MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        MainActivity activity = (MainActivity) instrumentation.startActivitySync(intent);
        try {
            assertNotNull(activity);
        } finally {
            instrumentation.runOnMainSync(activity::finish);
        }
    }
}
