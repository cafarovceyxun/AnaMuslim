/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 30/12/2022.
 * All rights reserved.
 */

package com.cafarovceyxun.anamuslim.utils;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cafarovceyxun.anamuslim.utils.univ.AndroidStringUtils;

public abstract class Logger {
    private static final String TAG = "QuranAppLogs";

    public static void reportError(@NonNull Throwable throwable) {
    }


    public static void logMsg(String msg) {
    }

    public static void i(@Nullable Object... msgs) {
        if (com.cafarovceyxun.anamuslim.BuildConfig.DEBUG) {
            Log.i(TAG, prepareLogMsg(msgs));
        }
    }

    public static void d(@Nullable Object... msgs) {
        if (com.cafarovceyxun.anamuslim.BuildConfig.DEBUG) {
            Log.d(TAG, prepareLogMsg(msgs));
        }
    }

    public static void print(@Nullable Object... msgs) {
        if (com.cafarovceyxun.anamuslim.BuildConfig.DEBUG) {
            String msg = prepareLogMsg(msgs);
            System.out.println("TRACKING: " + msg);
        }
    }

    private static String prepareLogMsg(@Nullable Object... msgs) {
        StringBuilder sb = new StringBuilder();

        StackTraceElement trc = Thread.currentThread().getStackTrace()[4];
        String className = trc.getClassName();
        className = className.substring(className.lastIndexOf(".") + 1);
        sb.append("(").append(className).append("=>").append(trc.getMethodName()).append(":").append(
            trc.getLineNumber()).append(
            "): ");

        if (msgs != null) {
            int len = msgs.length;
            for (int i = 0; i < len; i++) {
                Object msg = msgs[i];
                if (msg != null) {
                    if (msg instanceof Bundle) {
                        sb.append(AndroidStringUtils.bundle2String((Bundle) msg));
                    } else {
                        sb.append(msg);
                    }
                } else sb.append("null");
                if (i < len - 1) sb.append(", ");
            }
        } else sb.append("null");
        return sb.toString();
    }
}
