package com.paolodirauso.notrack.util;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;

import com.paolodirauso.notrack.R;

import org.sufficientlysecure.rootcommands.RootCommands;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class Utils {

    /**
     * Check if Android is rooted, check for su binary and busybox and display possible solutions if
     * they are not available
     *
     * @param activity
     * @return true if phone is rooted
     */
    public static boolean isAndroidRooted(final Activity activity) {
        boolean rootAccess = false;

        // root check can be disabled for debugging in emulator
        if (Constants.DEBUG_DISABLE_ROOT_CHECK) {
            rootAccess = true;
        } else {
            if (RootCommands.rootAccessGiven()) {
                rootAccess = true;
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setCancelable(false);
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setTitle(activity.getString(R.string.no_root_title));

                // build view from layout
                LayoutInflater factory = LayoutInflater.from(activity);
                final View dialogView = factory.inflate(R.layout.no_root_dialog, null);
                builder.setView(dialogView);

                builder.setNeutralButton(activity.getResources().getString(R.string.button_exit),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                activity.finish(); // finish current activity, means exiting app
                            }
                        }
                );

                AlertDialog alert = builder.create();
                alert.show();
            }
        }

        return rootAccess;
    }

    /**
     * Checks if Android is online
     *
     * @param context
     * @return returns true if online
     */
    public static boolean isAndroidOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        }
        return false;
    }

    /**
     * Checks if NoTrack is in foreground, see
     * http://stackoverflow.com/questions/8489993/check-android-application-is-in-foreground-or-not
     *
     * @param context
     * @return
     */
    public static boolean isInForeground(Context context) {
        AsyncTask<Context, Void, Boolean> foregroundCheckTask = new AsyncTask<Context, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Context... params) {
                final Context context = params[0].getApplicationContext();
                return isAppOnForeground(context);
            }

            private boolean isAppOnForeground(Context context) {
                ActivityManager activityManager = (ActivityManager) context
                        .getSystemService(Context.ACTIVITY_SERVICE);
                List<RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
                if (appProcesses == null) {
                    return false;
                }
                final String packageName = context.getPackageName();
                for (RunningAppProcessInfo appProcess : appProcesses) {
                    if (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                            && appProcess.processName.equals(packageName)) {
                        return true;
                    }
                }
                return false;
            }
        };

        boolean foreground = false;
        try {
            foreground = foregroundCheckTask.execute(context).get();
        } catch (InterruptedException e) {
            Log.e(Constants.TAG, "IsInForeground InterruptedException", e);
        } catch (ExecutionException e) {
            Log.e(Constants.TAG, "IsInForeground ExecutionException", e);
        }

        return foreground;
    }
}
