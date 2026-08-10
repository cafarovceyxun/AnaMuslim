package com.cafarovceyxun.anamuslim.utils.univ;

import android.os.Bundle;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 * Android/JVM-only string helpers that cannot live in the shared (KMP) StringUtils:
 * they depend on {@link Bundle} and {@code java.io} streams.
 */
public final class AndroidStringUtils {
    private AndroidStringUtils() {}

    public static String bundle2String(Bundle bundle) {
        StringBuilder builder = new StringBuilder();
        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            builder.append(key);
            builder.append("=");
            if (value instanceof Object[]) {
                builder.append(Arrays.toString((Object[]) value));
            } else {
                builder.append(value);
            }
            builder.append(", ");
        }
        return builder.toString();
    }

    public static String readInputStream(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();

        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        String str;
        while ((str = br.readLine()) != null) {
            sb.append(str);
        }

        br.close();

        return sb.toString();
    }
}
