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
package com.sonyericsson.chkbugreport.plugins.logs.kernel;

import com.sonyericsson.chkbugreport.BugReport;
import com.sonyericsson.chkbugreport.Util;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KernelLogLine {

    private static final Pattern TS = Pattern.compile("\\[ *([0-9]+)\\.([0-9]+)\\].*");

    KernelLogLine mPrev;

    String mLine;
    String mMsg;
    int mLevel = -1;
    long mKernelTime; // Processor time in ms
    String mLineHtml;
    boolean mOk;

    public Vector<String> prefixes = new Vector<String>();


    /**
     * Constructs a KernelLogLine.
     */
    public KernelLogLine(BugReport br, String line, KernelLogLine prev) {
        mLine = line;
        mPrev = prev;

        parse(line);
        // mLevel, mMsg and mKernelTime are set in parse()

        // mLineHtml is set in generateHtml()
    }

    /**
     * Parses this line as a dmesg log line.
     *
     * Returns the log level or -1 if 'line' is not well formatted.
     * Sets mKkernelTime to time stamp recalculated to ms or 0
     * if log level is -1.
     *
     * <6>[ 5616.729156] active wake lock rx_wake, time left 92
     */
    private void parse(String line) {
        mMsg = line;

        // Parse priority
        if (line.length() >= 3) {
            if (line.charAt(0) == '<' && line.charAt(2) == '>') {
                char c = line.charAt(1);
                if (c <= '0' && c <= '7') {
                    mLevel = c - '0';
                }
            }
            line = line.substring(3);
        }

        // Parse timestamp
        if (line.length() < 14) {
            return;
        }

        // Timestamp
        if (line.charAt(0) != '[') {
            // The timestamp is mandatory
            return;
        }

        Matcher m = TS.matcher(line);
        if (m.matches()) {
            int closePos = line.indexOf(']');
            line = line.substring(closePos + 2);
        }
        try {
            String first = m.group(1);
            String second = m.group(2);
            mKernelTime = Integer.parseInt(first) * 1000L + Integer.parseInt(second) / 1000L;
        } catch (Exception e) {
            return;
        }
        if (mKernelTime < 0) {
            mKernelTime = 0;
            return;
        }

        mMsg = line;
        mOk = true;
    }

    /**
     * Returns the log level.
     */
    public int getLevel() {
        return mLevel;
    }

    /**
     * Returns an HTML version of this well formatted log line.
     */
    public String generateHtml() {
        // Colorize based on level
        String css;
        switch (mLevel) {
            case 0: // KERN_EMERG
            case 1: // KERN_ALERT
            case 2: // KERN_CRIT
                css = "log-fatal";
                break;
            case 3: // KERN_ERR
                css = "log-error";
                break;
            case 4: // KERN_WARNING
                css = "log-warning";
                break;
            case 5: // KERN_NOTICE
            case 6: // KERN_INFO
                css = "log-info";
                break;
            case 7: // KERN_DEBUG
                css = "log-debug";
                break;
            default:
                mLineHtml = "<div class=\"log-debug\">" + Util.escape(mLine) + "</div>";
                return mLineHtml;
        }

        // Generate an HTML version of the line
        String line = (mLineHtml != null) ? mLineHtml : Util.escape(mLine); // Use annotated version if present
        mLineHtml = "<div class=\"" + css + "\">" + line + "</div>";
        return mLineHtml;
    }

    /**
     *
     */
    public void addPrefix(String box) {
        prefixes.add(box);
    }

    /**
     *
     */
    public void addMarker(String css, String extraAttr, String msg, String title) {
        if (title == null) {
            title = msg.replace("<br/>", "\n");
        }
        String box = "<div class=\"" + css + "\" "
                + (extraAttr == null ? "" : extraAttr)
                + " title=\"" + title + "\" >" +
                msg + "</div>";
        addPrefix(box);
    }
}

