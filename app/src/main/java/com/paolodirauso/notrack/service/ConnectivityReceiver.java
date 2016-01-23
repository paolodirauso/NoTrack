package com.paolodirauso.notrack.service;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.paolodirauso.notrack.helper.PreferenceHelper;
import com.paolodirauso.notrack.util.Constants;
import com.paolodirauso.notrack.util.Log;

import wakeful.WakefulIntentService;

public class ConnectivityReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            Log.d(Constants.TAG, "ConnectivityReceiver invoked...");

            // only when background update is enabled in prefs
            if (PreferenceHelper.getUpdateCheckDaily(context)) {
                Log.d(Constants.TAG, "Update check daily is enabled!");

                boolean noConnectivity = intent.getBooleanExtra(
                        ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);

                if (!noConnectivity) {

                    ConnectivityManager cm = (ConnectivityManager) context
                            .getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo netInfo = cm.getActiveNetworkInfo();

                    // only when connected or while connecting...
                    if (netInfo != null && netInfo.isConnectedOrConnecting()) {

                        // if we have mobile or wifi/ethernet connectivity...
                        if ((netInfo.getType() == ConnectivityManager.TYPE_MOBILE)
                                || (netInfo.getType() == ConnectivityManager.TYPE_WIFI)
                                || (netInfo.getType() == ConnectivityManager.TYPE_ETHERNET)) {
                            Log.d(Constants.TAG, "We have internet, start update check and disable receiver!");

                            // Start service with wakelock by using WakefulIntentService
                            Intent updateIntent = new Intent(context, UpdateService.class);
                            updateIntent.putExtra(UpdateService.EXTRA_BACKGROUND_EXECUTION, true);
                            WakefulIntentService.sendWakefulWork(context, updateIntent);

                            // disable receiver after we started UpdateService
                            disableReceiver(context);
                        }
                    }
                }
            }
        }
    }

    /**
     * Enables ConnectivityReceiver
     *
     * @param context
     */
    public static void enableReceiver(Context context) {
        ComponentName component = new ComponentName(context, ConnectivityReceiver.class);

        context.getPackageManager().setComponentEnabledSetting(component,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    /**
     * Disables ConnectivityReceiver
     *
     * @param context
     */
    public static void disableReceiver(Context context) {
        ComponentName component = new ComponentName(context, ConnectivityReceiver.class);

        context.getPackageManager().setComponentEnabledSetting(component,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }
}
