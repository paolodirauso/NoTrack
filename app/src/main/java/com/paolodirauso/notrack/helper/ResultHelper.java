package com.paolodirauso.notrack.helper;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.paolodirauso.notrack.R;
import com.paolodirauso.notrack.main.MainActivity;
import com.paolodirauso.notrack.util.ApplyUtils;
import com.paolodirauso.notrack.util.Constants;
import com.paolodirauso.notrack.util.StatusCodes;
import com.paolodirauso.notrack.util.Utils;

public class ResultHelper {

    private static final int RESULT_NOTIFICATION_ID = 30;

    /**
     * Show notification based on result after ApplyService or RevertService
     *
     * @param context
     * @param result
     * @param numberOfSuccessfulDownloads
     */
    public static void showNotificationBasedOnResult(Context context, int result,
                                                     String numberOfSuccessfulDownloads) {
        if (result == StatusCodes.SUCCESS) {
            String title = context.getString(R.string.apply_success_title);
            String text = context.getString(R.string.apply_success_subtitle) + " "
                    + numberOfSuccessfulDownloads;

            MainActivity.setStatusBroadcast(context, title, text, StatusCodes.ENABLED);
        } else if (result == StatusCodes.REVERT_SUCCESS) {
            String title = context.getString(R.string.revert_successful_title);
            String text = context.getString(R.string.revert_successful);

            MainActivity.setStatusBroadcast(context, title, text, StatusCodes.DISABLED);
        } else if (result == StatusCodes.REVERT_FAIL) {
            String title = context.getString(R.string.revert_problem_title);
            String text = context.getString(R.string.revert_problem);

            // back to old status
            int oldStatus;
            if (ApplyUtils.isHostsFileCorrect(context, Constants.ANDROID_SYSTEM_ETC_HOSTS)) {
                oldStatus = StatusCodes.ENABLED;
            } else {
                oldStatus = StatusCodes.DISABLED;
            }

            processResult(context, title, text, text, result, oldStatus, null, false);
        } else if (result == StatusCodes.UPDATE_AVAILABLE) { // used from UpdateService
            String title = context.getString(R.string.status_update_available);
            String text = context.getString(R.string.status_update_available_subtitle);

            processResult(context, title, text, text, result, StatusCodes.UPDATE_AVAILABLE, null,
                    false);
        } else if (result == StatusCodes.DOWNLOAD_FAIL) { // used from UpdateService and
            // ApplyService
            String title = context.getString(R.string.download_fail_title);
            String text = context.getString(R.string.download_fail_dialog);
            String statusText = context.getString(R.string.status_download_fail_subtitle_new);

            processResult(context, title, text, statusText, result, StatusCodes.DOWNLOAD_FAIL,
                    null, true);
        } else if (result == StatusCodes.NO_CONNECTION) { // used from UpdateService and
            // ApplyService
            String title = context.getString(R.string.no_connection_title);
            String text = context.getString(R.string.no_connection);
            String statusText = context.getString(R.string.status_no_connection_subtitle);

            processResult(context, title, text, statusText, result, StatusCodes.DOWNLOAD_FAIL,
                    null, false);
        } else if (result == StatusCodes.ENABLED) { // used from UpdateService
            MainActivity.updateStatusEnabled(context);
        } else if (result == StatusCodes.DISABLED) { // used from UpdateService
            MainActivity.updateStatusDisabled(context);
        } else {
            String title = "";
            String text = "";
            switch (result) {
                case StatusCodes.APPLY_FAIL:
                    title = context.getString(R.string.apply_fail_title);
                    text = context.getString(R.string.apply_fail);
                    break;
                case StatusCodes.PRIVATE_FILE_FAIL:
                    title = context.getString(R.string.apply_private_file_fail_title);
                    text = context.getString(R.string.apply_private_file_fail);
                    break;
                case StatusCodes.NOT_ENOUGH_SPACE:
                    title = context.getString(R.string.apply_not_enough_space_title);
                    text = context.getString(R.string.apply_not_enough_space);
                    break;
                case StatusCodes.COPY_FAIL:
                    title = context.getString(R.string.apply_copy_fail_title);
                    text = context.getString(R.string.apply_copy_fail);
                    break;
            }

            processResult(context, title, text, text, result, StatusCodes.DISABLED, null, true);
        }
    }

    /**
     * Shows dialog and further information how to proceed after the applying process has ended and
     * the user clicked on the notification. This is based on the result from the apply process.
     *
     * @param result
     */
    public static void showDialogBasedOnResult(final Context context, int result,
                                               String numberOfSuccessfulDownloads) {
        if (result == StatusCodes.SUCCESS) {
            if (numberOfSuccessfulDownloads != null) {
                String title = context.getString(R.string.apply_success_title);
                String text = context.getString(R.string.apply_success_subtitle) + " "
                        + numberOfSuccessfulDownloads;

                MainActivity.setStatusBroadcast(context, title, text, StatusCodes.ENABLED);
            } else {
                MainActivity.updateStatusEnabled(context);
            }

        } else if (result == StatusCodes.REVERT_SUCCESS) {
            MainActivity.updateStatusDisabled(context);
        } else if (result == StatusCodes.ENABLED) {
            MainActivity.updateStatusEnabled(context);
        } else if (result == StatusCodes.DISABLED) {
            MainActivity.updateStatusDisabled(context);
        } else if (result == StatusCodes.UPDATE_AVAILABLE) {
            String title = context.getString(R.string.status_update_available);
            String text = context.getString(R.string.status_update_available_subtitle);

            MainActivity.setStatusBroadcast(context, title, text, StatusCodes.UPDATE_AVAILABLE);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setPositiveButton(context.getString(R.string.button_close),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    }
            );

            String title = "";
            String text = "";
            String statusText = "";
            switch (result) {
                case StatusCodes.NO_CONNECTION:
                    title = context.getString(R.string.no_connection_title);
                    text = context.getString(R.string.no_connection);
                    statusText = context.getString(R.string.status_no_connection_subtitle);

                    MainActivity.setStatusBroadcast(context, title, statusText,
                            StatusCodes.DOWNLOAD_FAIL);
                    break;
                case StatusCodes.DOWNLOAD_FAIL:
                    title = context.getString(R.string.download_fail_title);
                    text = context.getString(R.string.download_fail_dialog);
                    statusText = context.getString(R.string.status_download_fail_subtitle_new);

                    MainActivity.setStatusBroadcast(context, title, statusText,
                            StatusCodes.DOWNLOAD_FAIL);
                    break;
                case StatusCodes.APPLY_FAIL:
                    title = context.getString(R.string.apply_fail_title);
                    text = context.getString(R.string.apply_fail);

                    MainActivity.updateStatusDisabled(context);
                    break;
                case StatusCodes.PRIVATE_FILE_FAIL:
                    title = context.getString(R.string.apply_private_file_fail_title);
                    text = context.getString(R.string.apply_private_file_fail);

                    MainActivity.updateStatusDisabled(context);
                    break;
                case StatusCodes.NOT_ENOUGH_SPACE:
                    title = context.getString(R.string.apply_not_enough_space_title);
                    text = context.getString(R.string.apply_not_enough_space);

                    MainActivity.updateStatusDisabled(context);
                    break;
                case StatusCodes.COPY_FAIL:
                    title = context.getString(R.string.apply_copy_fail_title);
                    text = context.getString(R.string.apply_copy_fail);

                    MainActivity.updateStatusDisabled(context);
                    break;
                case StatusCodes.REVERT_FAIL:
                    title = context.getString(R.string.revert_problem_title);
                    text = context.getString(R.string.revert_problem);

                    // back to old status
                    if (ApplyUtils.isHostsFileCorrect(context, Constants.ANDROID_SYSTEM_ETC_HOSTS)) {
                        MainActivity.updateStatusEnabled(context);
                    } else {
                        MainActivity.updateStatusDisabled(context);
                    }
                    break;
            }
            builder.setTitle(title);
            builder.setMessage(text);

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    }

    /**
     * Private helper used in showNotificationBasedOnResult
     *
     * @param context
     * @param title
     * @param text
     * @param statusText
     * @param result
     * @param iconStatus
     * @param numberOfSuccessfulDownloads
     * @param showDialog
     */
    private static void processResult(Context context, String title, String text,
                                      String statusText, int result, int iconStatus, String numberOfSuccessfulDownloads,
                                      boolean showDialog) {
        if (Utils.isInForeground(context)) {
            if (showDialog) {
                // start BaseActivity with result
                Intent resultIntent = new Intent(context, MainActivity.class);
                resultIntent.putExtra(MainActivity.EXTRA_APPLYING_RESULT, result);
                if (numberOfSuccessfulDownloads != null) {
                    resultIntent.putExtra(MainActivity.EXTRA_NUMBER_OF_SUCCESSFUL_DOWNLOADS,
                            numberOfSuccessfulDownloads);
                }
                resultIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);
                resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(resultIntent);
            }
        } else {
            // show notification
            showResultNotification(context, title, text, result, numberOfSuccessfulDownloads);
        }

        if (numberOfSuccessfulDownloads != null) {
            MainActivity.setStatusBroadcast(context, title, statusText + " "
                    + numberOfSuccessfulDownloads, iconStatus);
        } else {
            MainActivity.setStatusBroadcast(context, title, statusText, iconStatus);
        }
    }

    /**
     * Show notification with result defined in params
     *
     * @param contentTitle
     * @param contentText
     */
    private static void showResultNotification(Context context, String contentTitle,
                                        String contentText, int applyingResult, String failingUrl) {
        NotificationManager notificationManager = (NotificationManager) context
                .getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        int icon = R.mipmap.logo_notrack;
        long when = System.currentTimeMillis();

        // add app name to title
        contentTitle = context.getString(R.string.app_name) + ": " + contentTitle;

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
            .setSmallIcon(icon).setWhen(when).setAutoCancel(true).setContentTitle(contentTitle)
                .setContentText(contentText);

        Intent notificationIntent = new Intent(context, MainActivity.class);

        // give postApplyingStatus with intent
        notificationIntent.putExtra(MainActivity.EXTRA_APPLYING_RESULT, applyingResult);
        notificationIntent.putExtra(MainActivity.EXTRA_NUMBER_OF_SUCCESSFUL_DOWNLOADS, failingUrl);

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(contentIntent);

        notificationManager.notify(RESULT_NOTIFICATION_ID, mBuilder.build());
    }
}
