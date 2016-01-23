package com.paolodirauso.notrack.main;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.paolodirauso.notrack.R;
import com.paolodirauso.notrack.helper.PreferenceHelper;
import com.paolodirauso.notrack.helper.ResultHelper;
import com.paolodirauso.notrack.service.ApplyService;
import com.paolodirauso.notrack.service.DailyListener;
import com.paolodirauso.notrack.service.RevertService;
import com.paolodirauso.notrack.service.UpdateService;
import com.paolodirauso.notrack.util.ApplyUtils;
import com.paolodirauso.notrack.util.Constants;
import com.paolodirauso.notrack.util.Log;
import com.paolodirauso.notrack.util.StatusCodes;
import com.paolodirauso.notrack.util.Utils;

import wakeful.WakefulIntentService;

import org.sufficientlysecure.rootcommands.RootCommands;

public class MainActivity extends AppCompatActivity {
    // Intent extras to give result of applying process to base activity
    public static final String EXTRA_APPLYING_RESULT = "notrack.APPLYING_RESULT";
    public static final String EXTRA_NUMBER_OF_SUCCESSFUL_DOWNLOADS = "notrack.NUMBER_OF_SUCCESSFUL_DOWNLOADS";

    // Intent definitions for LocalBroadcastManager to update status from other threads
    static final String ACTION_UPDATE_STATUS = "notrack.UPDATE_STATUS";
    public static final String EXTRA_UPDATE_STATUS_TITLE = "notrack.UPDATE_STATUS.TITLE";
    public static final String EXTRA_UPDATE_STATUS_TEXT = "notrack.UPDATE_STATUS.TEXT";
    public static final String EXTRA_UPDATE_STATUS_ICON = "notrack.UPDATE_STATUS.ICON";
    static final String ACTION_BUTTONS = "notrack.BUTTONS";
    public static final String EXTRA_BUTTONS_DISABLED = "notrack.BUTTONS.DISABLED";

    private CheckBox checkBoxUpdateDaily;
    private Button buttonActive;
    private Button buttonDisable;
    private TextView status_title;
    private TextView status_text;
    private ImageView status_icon;
    private ProgressBar status_progress;
    private Activity mActivity;
    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mReceiver;

    /**
     * Handle result from applying when clicked on notification
     * http://stackoverflow.com/questions/1198558
     * /how-to-send-parameters-from-a-notification-click-to-an-activity MainActivity launchMode is
     * set to SingleTop for this in AndroidManifest
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(Constants.TAG, "onNewIntent");

        // if a notification is clicked after applying was done, the following is processed
        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (extras.containsKey(EXTRA_APPLYING_RESULT)) {
                int result = extras.getInt(EXTRA_APPLYING_RESULT);
                Log.d(Constants.TAG, "Result from intent extras: " + result);

                // download failed because of url
                String numberOfSuccessfulDownloads = null;
                if (extras.containsKey(EXTRA_NUMBER_OF_SUCCESSFUL_DOWNLOADS)) {
                    numberOfSuccessfulDownloads = extras
                            .getString(EXTRA_NUMBER_OF_SUCCESSFUL_DOWNLOADS);
                    Log.d(Constants.TAG, "Applying information from intent extras: "
                            + numberOfSuccessfulDownloads);
                }

                ResultHelper
                        .showDialogBasedOnResult(mActivity, result, numberOfSuccessfulDownloads);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mActivity = this;

        buttonActive = (Button) findViewById(R.id.buttonActive);
        buttonDisable = (Button) findViewById(R.id.buttonDisable);
        status_title = (TextView) findViewById(R.id.status_title);
        status_text = (TextView) findViewById(R.id.status_text);
        status_icon = (ImageView) findViewById(R.id.status_icon);
        status_progress = (ProgressBar) findViewById(R.id.status_progress);
        checkBoxUpdateDaily = (CheckBox) findViewById(R.id.checkDaily);

        checkBoxUpdateDaily.setChecked(PreferenceHelper.getUpdateCheckDaily(mActivity));

        checkBoxUpdateDaily.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    PreferenceHelper.setUpdateCheckDaily(mActivity, true);
                } else{
                    PreferenceHelper.setUpdateCheckDaily(mActivity,false);
                }
            }
        });

        configDebug();

        // We use this to send broadcasts within our local process.
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

        // We are going to watch for broadcasts with status updates
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_UPDATE_STATUS);
        filter.addAction(ACTION_BUTTONS);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();

                if (intent.getAction().equals(ACTION_UPDATE_STATUS)) {
                    if (extras != null) {
                        if (extras.containsKey(EXTRA_UPDATE_STATUS_TITLE)
                                && extras.containsKey(EXTRA_UPDATE_STATUS_TEXT)
                                && extras.containsKey(EXTRA_UPDATE_STATUS_ICON)) {

                            String title = extras.getString(EXTRA_UPDATE_STATUS_TITLE);
                            String text = extras.getString(EXTRA_UPDATE_STATUS_TEXT);
                            int status = extras.getInt(EXTRA_UPDATE_STATUS_ICON);

                            setStatus(title, text, status);
                        }
                    }
                }
                if (intent.getAction().equals(ACTION_BUTTONS)) {
                    if (extras != null) {
                        if (extras.containsKey(EXTRA_BUTTONS_DISABLED)) {

                            boolean buttonsDisabled = extras.getBoolean(EXTRA_BUTTONS_DISABLED);

                            setButtonsDisabled(buttonsDisabled);
                        }
                    }
                }
            }
        };
        mLocalBroadcastManager.registerReceiver(mReceiver, filter);


        // check for root
        if (Utils.isAndroidRooted(mActivity)) {

            // check if hosts file is applied
            if (ApplyUtils.isHostsFileCorrect(mActivity, Constants.ANDROID_SYSTEM_ETC_HOSTS)) {
                // do background update check
                // do only if not disabled in preferences
                if (PreferenceHelper.getUpdateCheck(mActivity)) {
                    Intent updateIntent = new Intent(mActivity, UpdateService.class);
                    updateIntent.putExtra(UpdateService.EXTRA_BACKGROUND_EXECUTION, false);
                    WakefulIntentService.sendWakefulWork(mActivity, updateIntent);
                } else {
                    MainActivity.updateStatusEnabled(mActivity);
                }
            } else {
                MainActivity.updateStatusDisabled(mActivity);
            }

        }

        // schedule CheckUpdateService
        WakefulIntentService.scheduleAlarms(new DailyListener(), mActivity, false);

    }

    public void configDebug(){
        // Set Debug level based on preference
        if (PreferenceHelper.getDebugEnabled(this)) {
            Constants.DEBUG = true;
            Log.d(Constants.TAG, "Debug set to true by preference!");
            // set RootCommands to debug mode based on NoTrack
            RootCommands.DEBUG = Constants.DEBUG;
        } else {
            Constants.DEBUG = false;
            RootCommands.DEBUG = Constants.DEBUG;
        }
    }

    /**
     * Static helper method to send broadcasts to the MainActivity and update status in frontend
     *
     * @param context
     * @param title
     * @param text
     * @param iconStatus Select UPDATE_AVAILABLE, ENABLED, DISABLED, DOWNLOAD_FAIL, or CHECKING from
     *                   StatusCodes
     */
    public static void setStatusBroadcast(Context context, String title, String text, int iconStatus) {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);

        Intent intent = new Intent(ACTION_UPDATE_STATUS);
        intent.putExtra(EXTRA_UPDATE_STATUS_ICON, iconStatus);
        intent.putExtra(EXTRA_UPDATE_STATUS_TITLE, title);
        intent.putExtra(EXTRA_UPDATE_STATUS_TEXT, text);
        localBroadcastManager.sendBroadcast(intent);
    }

    /**
     * Static helper method to send broadcasts to the MainActivity and enable or disable buttons
     *
     * @param context
     * @param buttonsDisabled to enable buttons apply and revert
     */
    public static void setButtonsDisabledBroadcast(Context context, boolean buttonsDisabled) {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);

        Intent intent = new Intent(ACTION_BUTTONS);
        intent.putExtra(EXTRA_BUTTONS_DISABLED, buttonsDisabled);
        localBroadcastManager.sendBroadcast(intent);
    }

    /**
     * Wrapper to set status to enabled
     *
     * @param context
     */
    public static void updateStatusEnabled(Context context) {
        setStatusBroadcast(context, context.getString(R.string.status_enabled),
                context.getString(R.string.status_enabled_subtitle), StatusCodes.ENABLED);
    }

    /**
     * Wrapper to set status to disabled
     *
     * @param context
     */
    public static void updateStatusDisabled(Context context) {
        setStatusBroadcast(context, context.getString(R.string.status_disabled),
                context.getString(R.string.status_disabled_subtitle), StatusCodes.DISABLED);
    }

    /**
     * Button Action to download and apply hosts files
     *
     * @param view
     */
    public void applyClick(View view) {
        WakefulIntentService.sendWakefulWork(mActivity, ApplyService.class);
    }

    /**
     * Button Action to Revert to default hosts file
     *
     * @param view
     */
    public void revertClick(View view) {
        WakefulIntentService.sendWakefulWork(mActivity, RevertService.class);
    }

    /**
     * Set status icon based on StatusCodes
     *
     * @param iconStatus Select UPDATE_AVAILABLE, ENABLED, DISABLED, DOWNLOAD_FAIL, or CHECKING from
     *                   StatusCodes
     */
    private void setStatusIcon(int iconStatus) {
        switch (iconStatus) {
            case StatusCodes.UPDATE_AVAILABLE:
                status_progress.setVisibility(View.GONE);
                status_icon.setVisibility(View.VISIBLE);
                status_icon.setImageResource(R.drawable.status_update);
                break;
            case StatusCodes.ENABLED:
                status_progress.setVisibility(View.GONE);
                status_icon.setVisibility(View.VISIBLE);
                status_icon.setImageResource(R.drawable.status_enabled);
                break;
            case StatusCodes.DISABLED:
                status_progress.setVisibility(View.GONE);
                status_icon.setVisibility(View.VISIBLE);
                status_icon.setImageResource(R.drawable.status_disabled);
                break;
            case StatusCodes.DOWNLOAD_FAIL:
                status_progress.setVisibility(View.GONE);
                status_icon.setImageResource(R.drawable.status_fail);
                status_icon.setVisibility(View.VISIBLE);
                break;
            case StatusCodes.CHECKING:
                status_progress.setVisibility(View.VISIBLE);
                status_icon.setVisibility(View.GONE);
                break;

            default:
                break;
        }
    }

    /**
     * Set status in Fragment
     *
     * @param title
     * @param text
     * @param iconStatus int based on StatusCodes to select icon
     */
    public void setStatus(String title, String text, int iconStatus) {
        status_title.setText(title);
        status_text.setText(text);
        setStatusIcon(iconStatus);
    }

    public void setButtonsDisabled(boolean buttonsDisabled) {
        buttonActive.setEnabled(!buttonsDisabled);
        buttonDisable.setEnabled(!buttonsDisabled);
    }
}
