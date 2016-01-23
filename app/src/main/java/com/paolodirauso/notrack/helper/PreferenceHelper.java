package com.paolodirauso.notrack.helper;

import android.content.Context;
import android.content.SharedPreferences;

import com.paolodirauso.notrack.util.Constants;
import com.paolodirauso.notrack.R;

public class PreferenceHelper {
    public static boolean getUpdateCheck(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME,
                Context.MODE_PRIVATE);
        return prefs.getBoolean(context.getString(R.string.pref_update_check_key),
                Boolean.parseBoolean(context.getString(R.string.pref_update_check_def)));
    }

    public static boolean getUpdateCheckDaily(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME,
                Context.MODE_PRIVATE);
        return prefs.getBoolean(context.getString(R.string.pref_update_check_daily_key),
                Boolean.parseBoolean(context.getString(R.string.pref_update_check_daily_def)));
    }

    public static void setUpdateCheckDaily(Context context, boolean value) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(context.getString(R.string.pref_update_check_daily_key), value);
        editor.commit();
    }

    public static String getApplyMethod(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME,
                Context.MODE_PRIVATE);
        return prefs.getString(context.getString(R.string.pref_apply_method_key),
                context.getString(R.string.pref_apply_method_def));
    }

    public static boolean getDebugEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME,
                Context.MODE_PRIVATE);
        return prefs.getBoolean(context.getString(R.string.pref_enable_debug_key),
                Boolean.parseBoolean(context.getString(R.string.pref_enable_debug_def)));
    }
}
