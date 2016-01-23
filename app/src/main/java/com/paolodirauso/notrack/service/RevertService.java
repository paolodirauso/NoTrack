package com.paolodirauso.notrack.service;

import android.content.Context;
import android.content.Intent;

import com.paolodirauso.notrack.R;
import com.paolodirauso.notrack.helper.PreferenceHelper;
import com.paolodirauso.notrack.helper.ResultHelper;
import com.paolodirauso.notrack.main.MainActivity;
import com.paolodirauso.notrack.util.ApplyUtils;
import com.paolodirauso.notrack.util.Constants;
import com.paolodirauso.notrack.util.Log;
import com.paolodirauso.notrack.util.StatusCodes;

import wakeful.WakefulIntentService;

import org.sufficientlysecure.rootcommands.Shell;

import java.io.FileOutputStream;

public class RevertService extends WakefulIntentService {
    private Context mService;

    public RevertService() {
        super("NotrackRevertService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mService = this;

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Asynchronous background operations of service, with wakelock
     */
    @Override
    public void doWakefulWork(Intent intent) {
        // disable buttons
        MainActivity.setButtonsDisabledBroadcast(mService, true);

        try {
            Shell rootShell = Shell.startRootShell();
            int revertResult = revert(rootShell);
            rootShell.close();

            Log.d(Constants.TAG, "revert result: " + revertResult);

            // enable buttons
            MainActivity.setButtonsDisabledBroadcast(mService, false);

            ResultHelper.showNotificationBasedOnResult(mService, revertResult, null);
        } catch (Exception e) {
            Log.e(Constants.TAG, "Problem while reverting!", e);
        }
    }

    /**
     * Reverts to default hosts file
     *
     * @return Status codes REVERT_SUCCESS or REVERT_FAIL
     */
    private int revert(Shell shell) {
        MainActivity.setStatusBroadcast(mService, getString(R.string.status_reverting),
                getString(R.string.status_reverting_subtitle), StatusCodes.CHECKING);

        // build standard hosts file
        try {
            FileOutputStream fos = mService.openFileOutput(Constants.HOSTS_FILENAME,
                    Context.MODE_PRIVATE);

            // default localhost
            String localhost = Constants.LOCALHOST_IPv4 + " " + Constants.LOCALHOST_HOSTNAME
                    + Constants.LINE_SEPERATOR + Constants.LOCALHOST_IPv6 + " "
                    + Constants.LOCALHOST_HOSTNAME;
            fos.write(localhost.getBytes());
            fos.close();

            // copy build hosts file with RootTools, based on target from preferences
            if (PreferenceHelper.getApplyMethod(mService).equals("writeToSystem")) {

                ApplyUtils.copyHostsFile(mService, Constants.ANDROID_SYSTEM_ETC_HOSTS, shell);
            }

            // delete generated hosts file after applying it
            mService.deleteFile(Constants.HOSTS_FILENAME);

            // set status to disabled
            MainActivity.updateStatusDisabled(mService);

            return StatusCodes.REVERT_SUCCESS;
        } catch (Exception e) {
            Log.e(Constants.TAG, "Exception", e);

            return StatusCodes.REVERT_FAIL;
        }
    }
}
