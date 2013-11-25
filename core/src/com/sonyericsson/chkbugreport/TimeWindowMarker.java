/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
 * Copyright (C) 2012 Sony Mobile Communications AB
 *
 * This file is part of ChkBugReport.
 *
 * ChkBugReport is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * ChkBugReport is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ChkBugReport.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.sonyericsson.chkbugreport;

import com.sonyericsson.chkbugreport.util.Util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class encapsulates a timestamp which marks either the beginning or the end of the log
 * to process. It is used to limit the log to a shorter time window (in case the log is too long)
 */
public class TimeWindowMarker {

    private static final long DAY = 24 * 60 * 60 * 1000;

    private long mDate = -1;
    private long mTime = -1;
    private long mTS = -1;

    public TimeWindowMarker() {
        // NOP
    }

    /**
     * Parse the timestamp.
     * The format is <tt>DATE/TIME</tt> or <TIME>, where:
     * <ul>
     * <li><tt>DATE</tt> is in the format <tt>MM-DD</tt> (MM = 2 digit month number, DD = 2 digit day number)</li>
     * <li><tt>TIME</tt> is in the format <tt>HH:MM:SS.mmm</tt> (HH = 2 digit hour, MM = 2 digit minutes,
     *     SS = 2 digit seconds and mmm = 3 digit milliseconds). Note that the hour part is mandatory, the rest is optional.</li>
     * </ul>
     * Examples for valid timestamp:
     * <ul>
     * <li>11-17/12:00:12.012</li>
     * <li>11-17/12:00</li>
     * <li>12:00:12</li>
     * </ul>
     * @param string The timestamp as string to be parsed
     */
    public TimeWindowMarker(String string) {
        int idx = string.indexOf('/');
        if (idx > 0) {
            Matcher m = Pattern.compile("([0-9]{2})-([0-9]{2})").matcher(string.substring(0, idx));
            if (!m.matches()) {
                throw new IllegalArgumentException("Badly formatted date");
            }
            string = string.substring(idx + 1);
            int month = Integer.parseInt(m.group(1));
            int day = Integer.parseInt(m.group(2));
            mDate = month * 31 + day;
        }
        if (string.length() > 0) {
            int hour = 0, min = 0, sec = 0, msec = 0;
            // Parse hour
            if (!Character.isDigit(string.charAt(0)) || !Character.isDigit(string.charAt(1))) {
                throw new IllegalArgumentException("Wrong time: cannot parse hour");
            }
            hour = Integer.parseInt(string.substring(0, 2));
            string = string.substring(2);
            // Parse minute
            if (string.length() > 0) {
                if (':' != string.charAt(0) || !Character.isDigit(string.charAt(1)) || !Character.isDigit(string.charAt(2))) {
                    throw new IllegalArgumentException("Wrong time: cannot parse minute");
                }
                min = Integer.parseInt(string.substring(1, 3));
                string = string.substring(3);
            }
            // Parse seconds
            if (string.length() > 0) {
                if (':' != string.charAt(0) || !Character.isDigit(string.charAt(1)) || !Character.isDigit(string.charAt(2))) {
                    throw new IllegalArgumentException("Wrong time: cannot parse seconds");
                }
                sec = Integer.parseInt(string.substring(1, 3));
                string = string.substring(3);
            }
            // Parse millis
            if (string.length() > 0) {
                if ('.' != string.charAt(0)) {
                    throw new IllegalArgumentException("Wrong time: cannot parse milliseconds");
                }
                msec = Integer.parseInt(string.substring(1));
            }
            // Build time
            mTime = hour;
            mTime = mTime * 60 + min;
            mTime = mTime * 60 + sec;
            mTime = mTime * 1000 + msec;
        }
        checkTS();
    }

    private void checkTS() {
        if (mTS == -1 && mDate != -1 && mTime != -1) {
            mTS = mTime + mDate * DAY;
        }
    }

    private void checkDay(long ts) {
        if (mDate == -1) {
            mDate = ts / DAY;
            checkTS();
        }
    }

    /**
     * Returns true if the specified timestamp is above the time window mark.
     * It also returns true if the time window mark is not set.
     * @param ts The log timestamp to check
     * @return true if the log timestamp matches the filter
     */
    public boolean isAfterOrNoFilter(long ts) {
        checkDay(ts);
        if (mTS != -1) {
            return ts >= mTS;
        }
        return true;
    }

    /**
     * Returns true if the specified timestamp is below the time window mark.
     * It also returns true if the time window mark is not set.
     * @param ts The log timestamp to check
     * @return true if the log timestamp matches the filter
     */
    public boolean isBeforeOrNoFilter(long ts) {
        checkDay(ts);
        if (mTS != -1) {
            return ts <= mTS;
        }
        return true;
    }

    /**
     * Returns a formatted string of the stored value
     * @return a formatted string of the stored value
     */
    public String format() {
        if (mTS == -1) {
            return "(no limit)";
        }
        return Util.formatLogTS(mTS);
    }

}
