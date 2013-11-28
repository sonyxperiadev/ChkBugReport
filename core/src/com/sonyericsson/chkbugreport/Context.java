/*
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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Context contains various configurations which affects the whole processing.
 */
public final class Context {

    // Log cache
    private Vector<String> mLogCache = new Vector<String>();
    private PrintStream mOut = null;
    private OutputListener mOutListener;
    // Time window markers
    private TimeWindowMarker mTimeWindowStart = new TimeWindowMarker();
    private TimeWindowMarker mTimeWindowEnd = new TimeWindowMarker();
    // GMT offset
    private int mGmtOffset = 0;
    // URL to ChkBugReport's homepage
    private String mHomePageUrl = "http://github.com/sonyxperiadev/ChkBugReport";
    // Input file limit (used only in some sections)
    private int mLimit = Integer.MAX_VALUE;
    // Silent mode
    private boolean mSilent = false;
    // Next chapter id to be allocated
    private int mNextChapterId = 1;

    /**
     * Returns the url to ChkBugReport's homepage
     * @return the url to ChkBugReport's homepage
     */
    public String getHomePageUrl() {
        return mHomePageUrl;
    }

    /**
     * Changes the url which will be used as ChkBugReport's homepage.
     * This could be used to redirect the link to internal website in organizations.
     * @param url The new url
     */
    public void setHomePageUrl(String url) {
        mHomePageUrl = url;
    }


    /**
     * Returns the starting point of the time window to limit the logs to.
     * If this value is set, the plugins processing the logs should ignore every line who's
     * timestamp is below this value.
     *
     * @return the starting point of the time window to limit the logs to.
     */
    public TimeWindowMarker getTimeWindowStart() {
        return mTimeWindowStart;
    }

    /**
     * Returns the ending point of the time window to limit the logs to.
     * If this value is set, the plugins processing the logs should ignore every line who's
     * timestamp is above this value.
     *
     * @return the ending point of the time window to limit the logs to.
     */
    public TimeWindowMarker getTimeWindowEnd() {
        return mTimeWindowEnd;
    }

    /**
     * Returns the GMT offset/timezone of the logs.
     * It's used to map the kernel timestamps with the log timestamps.
     * @return the GMT offset/timezone of the logs.
     */
    public int getGmtOffset() {
        return mGmtOffset;
    }

    /**
     * Return the input file limitation.
     * Note: this is just a recomended value, some sections might ignore this
     * @return The file size limit in bytes
     */
    public int getLimit() {
        return mLimit;
    }

    /**
     * Set the file size limit
     * @param value The new file size limit in bytes
     */
    public void setLimit(int value) {
        mLimit = value;
    }

    /**
     * Returns true if the application should be silent
     * @return true if the application should be silent
     */
    public boolean isSilent() {
        return mSilent;
    }

    /**
     * Set silent mode
     * @param silent True if silent mode should be used
     */
    public void setSilent(boolean silent) {
        mSilent = silent;
    }

    /* package */ void parseTimeWindow(String timeWindow) {
        try {
            Matcher m = Pattern.compile("(.*)\\.\\.(.*)").matcher(timeWindow);
            if (!m.matches()) {
                throw new IllegalArgumentException("Incorrect time window range");
            }
            mTimeWindowStart = new TimeWindowMarker(m.group(1));
            mTimeWindowEnd = new TimeWindowMarker(m.group(2));
        } catch (Exception e) {
            System.err.println("Error parsing timewindow: `" + timeWindow + "': " + e);
            System.exit(1);
        }
    }

    /* package */ void parseGmtOffset(String param) {
        try {
            if (param.startsWith("+")) {
                param = param.substring(1);
            }
            mGmtOffset = Integer.parseInt(param);
        } catch (NumberFormatException e) {
            System.err.println("Error parsing gmt offset: `" + param + "': " + e);
            System.exit(1);
        }
    }

    public void printOut(int level, String s) {
        String line = " <" + level + "> " + s;
        if (mOut == null) {
            mLogCache.add(line);
        } else {
            mOut.println(line);
        }
        if (mOutListener != null) {
            mOutListener.onPrint(level, OutputListener.TYPE_OUT, s);
        }
    }

    public void printErr(int level, String s) {
        String line = "!<" + level + "> " + s;
        if (mOut == null) {
            mLogCache.add(line);
        } else {
            mOut.println(line);
        }
        if (mOutListener != null) {
            mOutListener.onPrint(level, OutputListener.TYPE_ERR, s);
        }
    }

    /* package */ void setLogOutput(String logName) {
        if (mOut == null) {
            try {
                File f = new File(logName);
                f.getParentFile().mkdirs();
                mOut = new PrintStream(f);
                for (String line : mLogCache) {
                    mOut.println(line);
                }
                mLogCache.clear();
            } catch (IOException e) {
                System.err.println("Error opening output log file: " + e);
            }
        }
    }

    /* package */ void setOutputListener(OutputListener listener) {
        mOutListener = listener;
    }

    public int allocChapterId() {
        return mNextChapterId++;
    }

}
