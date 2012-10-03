package com.sonyericsson.chkbugreport;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeWindowMarker {

    private static final long DAY = 24 * 60 * 60 * 1000;

    private long mDate = -1;
    private long mTime = -1;
    private long mTS = -1;

    public TimeWindowMarker() {
        // NOP
    }

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

    public boolean isAfterOrNoFilter(long ts) {
        checkDay(ts);
        if (mTS != -1) {
            return ts >= mTS;
        }
        return true;
    }

    public boolean isBeforeOrNoFilter(long ts) {
        checkDay(ts);
        if (mTS != -1) {
            return ts <= mTS;
        }
        return true;
    }

    public String format() {
        if (mTS == -1) {
            return "(no limit)";
        }
        return Util.formatLogTS(mTS);
    }

}
