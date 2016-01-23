package com.paolodirauso.notrack.util;

public class Constants {
    /* DEBUG enables Log.d outputs
     DEBUG must be set by a preference */
    public static boolean DEBUG;
    public static final boolean DEBUG_UPDATE_CHECK_SERVICE = false;
    public static final boolean DEBUG_DISABLE_ROOT_CHECK = false;

    public static final String TAG = "NoTrack";

    public static final String PREFS_NAME = "preferences";

    public static final String LOCALHOST_IPv4 = "127.0.0.1";
    public static final String LOCALHOST_IPv6 = "::1";
    public static final String LOCALHOST_HOSTNAME = "localhost";

    public static final String HOSTSSOURCES = "hosts_sources";
    public static final String DOWNLOADED_HOSTS_FILENAME = "hosts_downloaded";
    public static final String HOSTS_FILENAME = "hosts";
    public static final String LINE_SEPERATOR = System.getProperty("line.separator", "\n");
    public static final String FILE_SEPERATOR = System.getProperty("file.separator", "/");

    public static final String COMMAND_CHOWN = "chown 0:0";
    public static final String COMMAND_CHMOD_644 = "chmod 644";
    public static final String COMMAND_RM = "rm -f";

    public static final String ANDROID_SYSTEM_PATH = System.getProperty("java.home", "/system");
    public static final String ANDROID_SYSTEM_ETC_HOSTS = ANDROID_SYSTEM_PATH + FILE_SEPERATOR
            + "etc" + FILE_SEPERATOR + HOSTS_FILENAME;

    public static final String HEADER1 = "# This hosts file is generated by NoTrack.";
    public static final String HEADER2 = "# Please do not modify it directly, it will be overwritten when NoTrack is applied again.";
    public static final String HEADER_SOURCES = "# This file is generated from the following sources:";
}