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
package com.sonyericsson.chkbugreport.plugins;

import com.sonyericsson.chkbugreport.BugReport;
import com.sonyericsson.chkbugreport.Util;

import java.util.Vector;

public class KernelLogLine {

    KernelLogLine mPrev;

    String mLine;
    int mLevel = -1;
    long mKernelTime; // Processor time in ms
    String mLineHtml;

    public Vector<String> prefixes = new Vector<String>();

    /**
     * Constructs a KernelLogLine.
     */
    public KernelLogLine(BugReport br, String line, KernelLogLine prev) {
        mLine = line;
        mPrev = prev;

        mLevel = parse(line);
        // mKernelTime is set in parse()

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
     private int parse(String line) {
         if (line.length() <= 17) { // Length of level + timestamp
            return -1;
        }

        // Level
        if (line.charAt(0) != '<' || line.charAt(2) != '>') {
            return -1;
        }
        char c = line.charAt(1);
        if (c < '0' || c > '7') {
            return -1;
        }
        int level = c - '0';

        // Timestamp
        if (line.charAt(3) != '[') {
            return -1;
        }
        int closePos = line.indexOf(']');
        if (closePos < 16) {
            return -1;
        }
        try {
            int dotPos = line.indexOf('.');
            String first = line.substring(4, dotPos).trim();
            String second = line.substring(dotPos + 1, closePos);
            mKernelTime = Integer.parseInt(first) * 1000L +
                    Integer.parseInt(second) / 1000L;
        } catch (Exception e) {
            return -1;
        }
        if (mKernelTime < 0) {
            mKernelTime = 0;
            return -1;
        }

        return level;
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

