package com.paolodirauso.notrack.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.paolodirauso.notrack.R;
import com.paolodirauso.notrack.helper.ResultHelper;
import com.paolodirauso.notrack.main.MainActivity;
import com.paolodirauso.notrack.provider.SourceHosts;
import com.paolodirauso.notrack.util.ApplyUtils;
import com.paolodirauso.notrack.util.Constants;
import com.paolodirauso.notrack.util.DateUtils;
import com.paolodirauso.notrack.util.Log;
import com.paolodirauso.notrack.util.StatusCodes;
import com.paolodirauso.notrack.util.Utils;

import wakeful.WakefulIntentService;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

/**
 * CheckUpdateService checks every 24 hours at about 9 am for updates of hosts sources, see
 * UpdateListener for scheduling
 */
public class UpdateService extends WakefulIntentService {
    // Intent extras to define whether to apply after checking or not
    public static final String EXTRA_BACKGROUND_EXECUTION = "notrack.BACKGROUND_EXECUTION";

    private Context mService;
    private NotificationManager mNotificationManager;

    private boolean mApplyAfterCheck;
    private boolean mBackgroundExecution;

    private int mNumberOfFailedDownloads;
    private int mNumberOfDownloads;

    private static final int UPDATE_NOTIFICATION_ID = 10;

    public UpdateService() {
        super("NoTrackUpdateService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mService = this;

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // get from intent extras if this service is executed in the background
        mBackgroundExecution = false;
        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (extras.containsKey(EXTRA_BACKGROUND_EXECUTION)) {
                mBackgroundExecution = extras.getBoolean(EXTRA_BACKGROUND_EXECUTION);
            }
        }

        // UpdateService should apply after checking if enabled in preferences
        mApplyAfterCheck = false;
        if (mBackgroundExecution) {
            mApplyAfterCheck = extras.getBoolean(EXTRA_BACKGROUND_EXECUTION);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Asynchronous background operations of service, with wakelock
     */
    @Override
    public void doWakefulWork(Intent intent) {
        if (!Utils.isInForeground(mService)) {
            showUpdateNotification();
        }

        MainActivity.setStatusBroadcast(mService, mService.getString(R.string.status_checking),
                mService.getString(R.string.status_checking_subtitle), StatusCodes.CHECKING);

        int result = checkForUpdates();

        Log.d(Constants.TAG, "Update Check result: " + result);

        cancelUpdateNotification();

        // If this is run from background and should update after checking...
        if (result == StatusCodes.UPDATE_AVAILABLE && mApplyAfterCheck) {
            // download and apply!
            WakefulIntentService.sendWakefulWork(mService, ApplyService.class);
        } else {
            String successfulDownloads = (mNumberOfDownloads - mNumberOfFailedDownloads) + "/"
                    + mNumberOfDownloads;

            ResultHelper.showNotificationBasedOnResult(mService, result, successfulDownloads);
        }
    }

    /**
     * Check for updates of hosts sources
     *
     * @return return code
     */
    private int checkForUpdates() {
        long currentLastModifiedLocal;
        long currentLastModifiedOnline;
        boolean updateAvailable = false;

        int returnCode = StatusCodes.ENABLED; // default return code

        if (Utils.isAndroidOnline(mService)) {

            mNumberOfFailedDownloads = 0;
            mNumberOfDownloads = 0;

            ArrayList<String> hostsSources = new SourceHosts().getSources();
            //shared preferences lastmodified values
            SharedPreferences hostsPreferences = getSharedPreferences(Constants.HOSTSSOURCES, Context.MODE_PRIVATE);
            SharedPreferences.Editor hostsEdit = hostsPreferences.edit();

            for(String currentUrl: hostsSources) {
                currentLastModifiedLocal = hostsPreferences.getLong(currentUrl, 0);
                mNumberOfDownloads++;

                try {
                    Log.v(Constants.TAG, "Checking hosts file: " + currentUrl);

                        /* build connection */
                    URL mURL = new URL(currentUrl);
                    URLConnection connection = mURL.openConnection();
                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(30000);

                    currentLastModifiedOnline = connection.getLastModified();

                    Log.d(Constants.TAG,
                            "mConnectionLastModified: "
                                    + currentLastModifiedOnline
                                    + " ("
                                    + DateUtils.longToDateString(mService,
                                    currentLastModifiedOnline) + ")"
                    );

                    Log.d(Constants.TAG,
                            "mCurrentLastModified: "
                                    + currentLastModifiedLocal
                                    + " ("
                                    + DateUtils.longToDateString(mService,
                                    currentLastModifiedLocal) + ")"
                    );

                    // check if file is available
                    connection.connect();
                    connection.getInputStream();

                    // check if update available for this hosts file
                    if (currentLastModifiedOnline > currentLastModifiedLocal) {
                        updateAvailable = true;
                    }

                    // update SharedPreferences
                    hostsEdit.putLong(currentUrl, currentLastModifiedOnline);
                    hostsEdit.commit();

                } catch (Exception e) {
                    Log.e(Constants.TAG, "Exception while downloading from " + currentUrl, e);

                    mNumberOfFailedDownloads++;

                    // set last_modified_online of failed download to 0 (not available)
                    hostsEdit.putLong(currentUrl, 0);
                    hostsEdit.commit();
                }
            }

            // if all downloads failed return download_fail error
            if (mNumberOfDownloads == mNumberOfFailedDownloads && mNumberOfDownloads != 0) {
                returnCode = StatusCodes.DOWNLOAD_FAIL;
            }
        } else {
            // only report no connection when not in background
            if (!mBackgroundExecution) {
                returnCode = StatusCodes.NO_CONNECTION;
            } else {
                Log.e(Constants.TAG,
                        "Should not happen! In background execution is no connection available!");
            }
        }

        // set return code if update is available
        if (updateAvailable) {
            returnCode = StatusCodes.UPDATE_AVAILABLE;
        }

        // check if hosts file is applied
        if (!ApplyUtils.isHostsFileCorrect(mService, Constants.ANDROID_SYSTEM_ETC_HOSTS)) {
            returnCode = StatusCodes.DISABLED;
        }

        return returnCode;
    }

    /**
     * Show permanent notification while executing checkForUpdates
     */
    private void showUpdateNotification() {
        int icon = R.mipmap.logo_notrack;
        CharSequence tickerText = getString(R.string.app_name) + ": "
                + getString(R.string.status_checking);
        long when = System.currentTimeMillis();

        Context context = getApplicationContext();
        CharSequence contentTitle = getString(R.string.app_name) + ": "
                + getString(R.string.status_checking);
        CharSequence contentText = getString(R.string.status_checking_subtitle);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
            .setSmallIcon(icon).setContentTitle(contentTitle).setTicker(tickerText)
                .setWhen(when).setOngoing(true).setContentText(contentText);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        mBuilder.setContentIntent(contentIntent);

        mNotificationManager.notify(UPDATE_NOTIFICATION_ID, mBuilder.build());
    }

    /**
     * Cancel Notification
     */
    private void cancelUpdateNotification() {
        mNotificationManager.cancel(UPDATE_NOTIFICATION_ID);
    }

}
