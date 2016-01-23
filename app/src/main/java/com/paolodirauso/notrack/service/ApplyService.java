package com.paolodirauso.notrack.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;

import com.paolodirauso.notrack.R;
import com.paolodirauso.notrack.helper.PreferenceHelper;
import com.paolodirauso.notrack.helper.ResultHelper;
import com.paolodirauso.notrack.main.MainActivity;
import com.paolodirauso.notrack.provider.SourceHosts;
import com.paolodirauso.notrack.util.ApplyUtils;
import com.paolodirauso.notrack.util.CommandException;
import com.paolodirauso.notrack.util.Constants;
import com.paolodirauso.notrack.util.Log;
import com.paolodirauso.notrack.util.NotEnoughSpaceException;
import com.paolodirauso.notrack.util.StatusCodes;
import com.paolodirauso.notrack.util.Utils;

import wakeful.WakefulIntentService;

import org.sufficientlysecure.rootcommands.Shell;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class ApplyService extends WakefulIntentService {
    private Context mService;
    private NotificationManager mNotificationManager;

    private int mNumberOfFailedDownloads;
    private int mNumberOfDownloads;

    private static final int APPLY_NOTIFICATION_ID = 20;

    public ApplyService() {
        super("NoTrackApplyService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mService = this;

        mNotificationManager = (NotificationManager) mService.getApplicationContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Asynchronous background operations of service, with wakelock
     */
    @Override
    public void doWakefulWork(Intent intent) {
        // disable buttons
        MainActivity.setButtonsDisabledBroadcast(mService, true);

        // download files with download method
        int downloadResult = download();
        Log.d(Constants.TAG, "Download result: " + downloadResult);

        if (downloadResult == StatusCodes.SUCCESS) {
            // Apply files by apply method
            int applyResult = apply();

            cancelApplyNotification();
            // enable buttons
            MainActivity.setButtonsDisabledBroadcast(mService, false);
            Log.d(Constants.TAG, "Apply result: " + applyResult);

            String successfulDownloads = (mNumberOfDownloads - mNumberOfFailedDownloads) + "/"
                    + mNumberOfDownloads;

            ResultHelper.showNotificationBasedOnResult(mService, applyResult, successfulDownloads);
        } else if (downloadResult == StatusCodes.DOWNLOAD_FAIL) {
            cancelApplyNotification();
            // enable buttons
            MainActivity.setButtonsDisabledBroadcast(mService, false);
            // extra information is current url, to show it when it fails
            ResultHelper.showNotificationBasedOnResult(mService, downloadResult, null);
        } else {
            cancelApplyNotification();
            // enable buttons
            MainActivity.setButtonsDisabledBroadcast(mService, false);
            ResultHelper.showNotificationBasedOnResult(mService, downloadResult, null);
        }
    }

    /**
     * Downloads files from hosts sources
     *
     * @return return code
     */
    private int download() {
        byte data[];
        int count;
        long currentLastModifiedOnline;

        int returnCode = StatusCodes.SUCCESS; // default return code

        if (Utils.isAndroidOnline(mService)) {

            showApplyNotification(mService, mService.getString(R.string.download_dialog),
                    mService.getString(R.string.download_dialog),
                    mService.getString(R.string.download_dialog));

            // output to write into
            FileOutputStream out = null;

            try {
                out = mService.openFileOutput(Constants.DOWNLOADED_HOSTS_FILENAME,
                        Context.MODE_PRIVATE);

                mNumberOfFailedDownloads = 0;
                mNumberOfDownloads = 0;

                ArrayList<String> hostsSources = new SourceHosts().getSources();
                //shared preferences lastmodified values
                SharedPreferences hostsPreferences = getSharedPreferences(Constants.HOSTSSOURCES, Context.MODE_PRIVATE);
                SharedPreferences.Editor hostsEdit = hostsPreferences.edit();

                for(String currentUrl: hostsSources){
                    mNumberOfDownloads++;

                    InputStream is = null;
                    BufferedInputStream bis = null;

                    try {
                        Log.v(Constants.TAG, "Downloading hosts file: " + currentUrl);

                            /* change URL in download dialog */
                        updateApplyNotification(mService,
                                mService.getString(R.string.download_dialog), currentUrl);

                            /* build connection */
                        URL mURL = new URL(currentUrl);
                        URLConnection connection = mURL.openConnection();
                        connection.setConnectTimeout(15000);
                        connection.setReadTimeout(30000);

                            /* connect */
                        connection.connect();
                        is = connection.getInputStream();

                        bis = new BufferedInputStream(is);
                        if (is == null) {
                            Log.e(Constants.TAG, "Stream is null");
                        }

                            /* download with progress */
                        data = new byte[1024];
                        count = 0;

                        // run while only when thread is not cancelled
                        while ((count = bis.read(data)) != -1) {
                            out.write(data, 0, count);
                        }

                        // add line separator to add files together in one file
                        out.write(Constants.LINE_SEPERATOR.getBytes());

                        // save last modified online for later use
                        currentLastModifiedOnline = connection.getLastModified();

                        hostsEdit.putLong(currentUrl,currentLastModifiedOnline);
                    } catch (IOException e) {
                        Log.e(Constants.TAG, "Exception while downloading from " + currentUrl,
                                e);

                        mNumberOfFailedDownloads++;

                        // set last_modified_online of failed download to 0 (not available)
                        hostsEdit.putLong(currentUrl, 0);
                    } finally {
                        // flush and close streams
                        try {
                            if (out != null) {
                                out.flush();
                            }
                            if (bis != null) {
                                bis.close();
                            }
                            if (is != null) {
                                is.close();
                            }
                        } catch (Exception e) {
                            Log.e(Constants.TAG, "Exception on flush and closing streams.", e);
                        }
                    }
                }

                // if all downloads failed return download_fail error
                if (mNumberOfDownloads == mNumberOfFailedDownloads && mNumberOfDownloads != 0) {
                    returnCode = StatusCodes.DOWNLOAD_FAIL;
                }
            } catch (Exception e) {
                Log.e(Constants.TAG, "Private File can not be created, Exception: " + e);
                returnCode = StatusCodes.PRIVATE_FILE_FAIL;
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (Exception e) {
                    Log.e(Constants.TAG, "Exception on close of out.", e);
                }
            }
        } else {
            returnCode = StatusCodes.NO_CONNECTION;
        }

        return returnCode;
    }

    /**
     * Apply hosts file
     *
     * @return return code
     */
    int apply() {
        showApplyNotification(mService, mService.getString(R.string.apply_dialog),
                mService.getString(R.string.apply_dialog),
                mService.getString(R.string.apply_dialog_hostnames));

        int returnCode = StatusCodes.SUCCESS; // default return code
        BufferedOutputStream bos = null;

        try {
            /* PARSE: parse hosts files to sets of hostnames and comments */

            FileInputStream fis = mService.openFileInput(Constants.DOWNLOADED_HOSTS_FILENAME);

            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String nextLine;
            // get hosts sources list
            ArrayList<String> enabledHostsSources = new SourceHosts().getSources();
            Log.d(Constants.TAG, "Enabled hosts sources list: " + enabledHostsSources.toString());

            FileOutputStream fos = mService.openFileOutput(Constants.HOSTS_FILENAME,
                    Context.MODE_PRIVATE);

            bos = new BufferedOutputStream(fos);

            // add notrack header
            String header = Constants.HEADER1 + Constants.LINE_SEPERATOR + Constants.HEADER2
                    + Constants.LINE_SEPERATOR + Constants.HEADER_SOURCES;
            bos.write(header.getBytes());

            // write sources into header
            String source = null;
            for (String host : enabledHostsSources) {
                source = Constants.LINE_SEPERATOR + "# " + host;
                bos.write(source.getBytes());
            }

            bos.write(Constants.LINE_SEPERATOR.getBytes());

            // add "127.0.0.1 localhost" entry
            String localhost = Constants.LINE_SEPERATOR + Constants.LOCALHOST_IPv4 + " "
                    + Constants.LOCALHOST_HOSTNAME + Constants.LINE_SEPERATOR
                    + Constants.LOCALHOST_IPv6 + " " + Constants.LOCALHOST_HOSTNAME;
            bos.write(localhost.getBytes());

            bos.write(Constants.LINE_SEPERATOR.getBytes());

            updateApplyNotification(mService, mService.getString(R.string.apply_dialog),
                    mService.getString(R.string.apply_dialog_hosts));

            while ((nextLine = reader.readLine()) != null) {
                // write hostnames
                String line = Constants.LINE_SEPERATOR + nextLine;
                bos.write(line.getBytes());
            }
            fis.close();
            // hosts file has to end with new line, when not done last entry won't be
            // recognized
            bos.write(Constants.LINE_SEPERATOR.getBytes());
        } catch (FileNotFoundException e) {
            Log.e(Constants.TAG, "file to read or file to write could not be found", e);

            returnCode = StatusCodes.PRIVATE_FILE_FAIL;
        } catch (IOException e) {
            Log.e(Constants.TAG, "files can not be written or read", e);

            returnCode = StatusCodes.PRIVATE_FILE_FAIL;
        }
        finally {
            try {
                if (bos != null) {
                    bos.flush();
                    bos.close();
                }
            }
            catch (Exception e) {
                Log.e(Constants.TAG, "Error closing output streams", e);
            }
        }

        // delete downloaded hosts file from private storage
        mService.deleteFile(Constants.DOWNLOADED_HOSTS_FILENAME);

    /* APPLY: apply hosts file using RootTools in copyHostsFile() */
        updateApplyNotification(mService, mService.getString(R.string.apply_dialog),
                mService.getString(R.string.apply_dialog_apply));

        Shell rootShell = null;
        try {
            rootShell = Shell.startRootShell();
        } catch (Exception e) {
            Log.e(Constants.TAG, "Problem opening a root shell!", e);
        }

        // copy build hosts file with RootTools, based on target from preferences
        try {
            if (PreferenceHelper.getApplyMethod(mService).equals("writeToSystem")) {

                ApplyUtils.copyHostsFile(mService, Constants.ANDROID_SYSTEM_ETC_HOSTS, rootShell);
            }
        } catch (NotEnoughSpaceException e) {
            Log.e(Constants.TAG, "Exception: ", e);

            returnCode = StatusCodes.NOT_ENOUGH_SPACE;
        } catch (CommandException e) {
            Log.e(Constants.TAG, "Exception: ", e);

            returnCode = StatusCodes.COPY_FAIL;
        }

        // delete generated hosts file from private storage
        mService.deleteFile(Constants.HOSTS_FILENAME);

        /* check if hosts file is applied with chosen method */
        // check only if everything before was successful
        if (returnCode == StatusCodes.SUCCESS) {
            if (PreferenceHelper.getApplyMethod(mService).equals("writeToSystem")) {

                /* /system/etc/hosts */

                if (!ApplyUtils.isHostsFileCorrect(mService, Constants.ANDROID_SYSTEM_ETC_HOSTS)) {
                    returnCode = StatusCodes.APPLY_FAIL;
                }
            }
        }

        try {
            rootShell.close();
        } catch (Exception e) {
            Log.e(Constants.TAG, "Problem closing the root shell!", e);
        }

        return returnCode;
    }

    /**
     * Creates custom made notification with progress
     */
    private void showApplyNotification(Context context, String tickerText, String contentTitle,
                                       String contentText) {
        // configure the intent
        Intent intent = new Intent(mService, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(mService.getApplicationContext(),
                0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        // add app name to notificationText
        tickerText = mService.getString(R.string.app_name) + ": " + tickerText;
        int icon = R.mipmap.logo_notrack;
        long when = System.currentTimeMillis();

        // add app name to title
        String contentTitleWithAppName = mService.getString(R.string.app_name) + ": "
                + contentTitle;

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(icon).setContentTitle(contentTitleWithAppName).setTicker(tickerText)
                .setWhen(when).setOngoing(true).setOnlyAlertOnce(true).setContentText(contentText);

        mNotificationManager.notify(APPLY_NOTIFICATION_ID, mBuilder.build());

        mBuilder.setContentIntent(contentIntent);

        // update status in BaseActivity with Broadcast
        MainActivity.setStatusBroadcast(mService, contentTitle, contentText, StatusCodes.CHECKING);
    }

    private void updateApplyNotification(Context context, String contentTitle, String contentText) {
        // configure the intent
        Intent intent = new Intent(mService, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(mService.getApplicationContext(),
                0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        int icon = R.mipmap.logo_notrack;

        // add app name to title
        String contentTitleWithAppName = mService.getString(R.string.app_name) + ": "
                + contentTitle;

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(icon).setContentTitle(contentTitleWithAppName)
                .setContentText(contentText);

        mNotificationManager.notify(APPLY_NOTIFICATION_ID, mBuilder.build());

        mBuilder.setContentIntent(contentIntent);

        // update status in BaseActivity with Broadcast
        MainActivity.setStatusBroadcast(mService, contentTitle, contentText, StatusCodes.CHECKING);
    }

    private void cancelApplyNotification() {
        mNotificationManager.cancel(APPLY_NOTIFICATION_ID);
    }

}
