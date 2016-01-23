package com.paolodirauso.notrack.util;

import android.content.Context;
import android.os.StatFs;

import org.sufficientlysecure.rootcommands.Shell;
import org.sufficientlysecure.rootcommands.Toolbox;
import org.sufficientlysecure.rootcommands.command.SimpleCommand;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ApplyUtils {
    /**
     * Check if there is enough space on partition where target is located
     *
     * @param size   size of file to put on partition
     * @param target path where to put the file
     * @return true if it will fit on partition of target, false if it will not fit.
     */
    public static boolean hasEnoughSpaceOnPartition(String target, long size) {
        try {
            // new File(target).getFreeSpace() (API 9) is not working on data partition

            // get directory without file
            String directory = new File(target).getParent().toString();

            StatFs stat = new StatFs(directory);
            long blockSize = stat.getBlockSize();
            long availableBlocks = stat.getAvailableBlocks();
            long availableSpace = availableBlocks * blockSize;

            Log.i(Constants.TAG, "Checking for enough space: Target: " + target + ", directory: "
                    + directory + " size: " + size + ", availableSpace: " + availableSpace);

            if (size < availableSpace) {
                return true;
            } else {
                Log.e(Constants.TAG, "Not enough space on partition!");
                return false;
            }
        } catch (Exception e) {
            // if new StatFs(directory) fails catch IllegalArgumentException and just return true as
            // workaround
            Log.e(Constants.TAG, "Problem while getting available space on partition!", e);
            return true;
        }
    }

    /**
     * Checks by reading hosts file if NoTrack hosts file is applied or not
     *
     * @return true if it is applied
     */
    public static boolean isHostsFileCorrect(Context context, String target) {
        boolean status = false;

        /* Check if first line in hosts file is NoTrack comment */
        InputStream stream = null;
        InputStreamReader in = null;
        BufferedReader br = null;
        try {
            File file = new File(target);

            stream = new FileInputStream(file);
            in = new InputStreamReader(stream);
            br = new BufferedReader(in);

            String firstLine = br.readLine();

            Log.d(Constants.TAG, "First line of " + target + ": " + firstLine);

            if (firstLine.equals(Constants.HEADER1)) {
                status = true;
            } else {
                status = false;
            }
        } catch (FileNotFoundException e) {
            Log.e(Constants.TAG, "FileNotFoundException", e);
            status = true; // workaround for: http://code.google.com/p/ad-away/issues/detail?id=137
        } catch (Exception e) {
            Log.e(Constants.TAG, "Exception: ", e);
            status = false;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    Log.e(Constants.TAG, "Exception", e);
                }
            }
        }

        return status;
    }

    /**
     * Copy hosts file from private storage of NoTrack to internal partition using RootTools
     *
     * @throws NotEnoughSpaceException RemountException CopyException
     */
    public static void copyHostsFile(Context context, String target, Shell shell)
            throws NotEnoughSpaceException, CommandException {
        Log.i(Constants.TAG, "Copy hosts file with target: " + target);
        String privateDir = context.getFilesDir().getAbsolutePath();
        String privateFile = privateDir + File.separator + Constants.HOSTS_FILENAME;

        /* check for space on partition */
        long size = new File(privateFile).length();
        Log.i(Constants.TAG, "Size of hosts file: " + size);
        if (!hasEnoughSpaceOnPartition(target, size)) {
            throw new NotEnoughSpaceException();
        }

        Toolbox tb = new Toolbox(shell);

        /* Execute commands */
        try {
            // remount for write access
            Log.i(Constants.TAG, "Remounting for RW...");
            if (!tb.remount(target, "RW")) {
                Log.e(Constants.TAG, "Remounting as RW failed! Probably not a problem!");
            }

            // remove before copying when using /system/etc/hosts
            if (target.equals(Constants.ANDROID_SYSTEM_ETC_HOSTS)) {
                SimpleCommand command = new SimpleCommand(Constants.COMMAND_RM + " " + target);
                shell.add(command).waitForFinish();
            }

            // copy file
            if (!tb.copyFile(privateFile, target, false, false)) {
                throw new CommandException();
            }

            // execute commands: chown, chmod
            SimpleCommand command = new SimpleCommand(Constants.COMMAND_CHOWN + " " + target,
                    Constants.COMMAND_CHMOD_644 + " " + target);
            shell.add(command).waitForFinish();
        } catch (Exception e) {
            Log.e(Constants.TAG, "Exception!", e);

            throw new CommandException();
        } finally {
            if (target.equals(Constants.ANDROID_SYSTEM_ETC_HOSTS)) {
                // after all remount system back as read only
                Log.i(Constants.TAG, "Remounting back to RO...");
                if (!tb.remount(target, "RO")) {
                    Log.e(Constants.TAG, "Remounting failed in finally! Probably not a problem!");
                }
            }
        }
    }
}