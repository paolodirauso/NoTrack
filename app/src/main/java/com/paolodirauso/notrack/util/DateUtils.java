package com.paolodirauso.notrack.util;

import android.content.Context;

import com.paolodirauso.notrack.R;

import java.text.DateFormat;
import java.util.Date;

public class DateUtils {
    /**
     * Builds date string out of long value containing unix date
     *
     * @param input
     * @return formatted date string
     */
    public static String longToDateString(Context context, long input) {
        if (input != 0) {
            Date date = new Date(input);
            DateFormat dataformat = DateFormat.getDateInstance(DateFormat.MEDIUM);

            return dataformat.format(date);
        } else {
            return context.getString(R.string.hosts_not_available);
        }
    }
}
