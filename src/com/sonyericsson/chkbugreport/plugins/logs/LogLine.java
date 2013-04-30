/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
 * Copyright (C) 2012-2013 Sony Mobile Communications AB
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
package com.sonyericsson.chkbugreport.plugins.logs;

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.ProcessRecord;
import com.sonyericsson.chkbugreport.doc.Renderer;
import com.sonyericsson.chkbugreport.util.HtmlUtil;

import java.io.IOException;

public class LogLine extends LogLineBase {

    public static final int FMT_UNKNOWN = 0;
    public static final int FMT_STD     = 1;
    public static final int FMT_BRAT    = 2;
    public static final int FMT_SHORT   = 3;

    public char level;

    public int pid = 0;
    public int pidS = -1;
    public int pidE = -1;

    public String[] fields;

    public int fmt = FMT_UNKNOWN;

    private ProcessRecord mPr;

    public LogLine(BugReportModule br, String line, int format, LogLine prev) {
        super(line);
        level = 'D';

        // Validate
        if (line.startsWith("---------")) return;
        switch (format) {
            case FMT_UNKNOWN:
                if (!parseFmtStd(br, line)) {
                    if (!parseFmtBrat(br, line)) {
                        parseFmtShort(br, line, prev);
                    }
                }
                break;
            case FMT_STD:
                parseFmtStd(br, line);
                break;
            case FMT_BRAT:
                parseFmtBrat(br, line);
                break;
            case FMT_SHORT:
                parseFmtShort(br, line, prev);
                break;
            default: throw new RuntimeException("Invalid format: " + format);
        }

        if (pid > 0) {
            mPr = br.getProcessRecord(pid, true, true);
        }
    }

    public LogLine(LogLine orig) {
        super(orig);
        level = orig.level;
        pid = orig.pid;
        pidS = orig.pidS;
        pidE = orig.pidE;
        fields = orig.fields;
        fmt = orig.fmt;
        mPr = orig.mPr;
    }

    @Override
    public LogLine copy() {
        return new LogLine(this);
    }

    /**
     * Parse a log line in the standard bugreport format
     */
    private boolean parseFmtStd(BugReportModule br, String line) {
        int tagEnd = line.indexOf(':', 18);
        if (tagEnd < 20) return false; // this is weird... abort
        int p0 = tagEnd - 7;
        int p1 = tagEnd - 1;
        char c = line.charAt(p0);
        if (c != '(') return false; // not this format
        c = line.charAt(p1);
        if (c != ')') return false; // not this format
        if (line.charAt(20) != '/') return false; // strange log format
        c = line.charAt(0);
        if (c < '0' && c > '9') return false; // strange log format

        parseTS(line);

        // Read log level
        level = line.charAt(19);

        // Read pid
        int p = p0;
        do {
            p++;
        } while (p < line.length() && line.charAt(p) == ' ');
        try {
            pid = Integer.parseInt(line.substring(p, p1));
        } catch(NumberFormatException t) {
            return false; // strange pid
        }
        pidS = p;
        pidE = p1;

        // Read tag
        int tagS = 21;
        int tagE = p0;
        if (tagE < 0) return false;
        while (tagE > tagS && line.charAt(tagE-1) == ' ') {
            tagE--;
        }
        tag = line.substring(tagS, tagE);

        // Read message
        msg = line.substring(p1 + 3);

        finishParse(br);
        fmt = FMT_STD;
        return true;
    }

    private void finishParse(BugReportModule br) {
        // Colorize based on level
        switch (level) {
            case 'F': css = "log-fatal"; break;
            case 'E': css = "log-error"; break;
            case 'W': css = "log-warning"; break;
            case 'I': css = "log-info"; break;
            case 'D': css = "log-debug"; break;
            case 'V': css = "log-verbose"; break;
        }

        // Read fields
        if (msg.startsWith("[") && msg.endsWith("]")) {
            String s = msg.substring(1, msg.length() - 1);
            fields = s.split(",");
        } else {
            fields = new String[1];
            fields[0] = msg;
        }

        ok = true;
    }

    private void parseTS(String line) {
        // Read time stamp
        try {
            int month = Integer.parseInt(line.substring(0, 2));
            int day = Integer.parseInt(line.substring(3, 5));
            int hour = Integer.parseInt(line.substring(6, 8));
            int min = Integer.parseInt(line.substring(9, 11));
            int sec  = Integer.parseInt(line.substring(12, 14));
            int ms = Integer.parseInt(line.substring(15, 18));
            ts = month;
            ts = ts * 31 + day;
            ts = ts * 24 + hour;
            ts = ts * 60 + min;
            ts = ts * 60 + sec;
            ts = ts * 1000 + ms;
        } catch (NumberFormatException nfe) {
            return; // strange log format
        }
    }

    /**
     * Parse a log line from logs generated by brat scripts (adb logcat -v threadtime)
     */
    private boolean parseFmtBrat(BugReportModule br, String line) {
        if (line.length() <= 33) return false;

        parseTS(line);

        // Parse pid
        pidS = 19;
        pidE  = 24;
        while (line.charAt(pidS) == ' ') pidS++;
        if (pidS >= pidE) return false;
        try {
            pid = Integer.parseInt(line.substring(pidS, pidE));
        } catch(NumberFormatException t) {
            return false; // strange pid
        }

        // Parse level
        level = line.charAt(31);

        // Parse tag
        int tagS = 33;
        int tagE = line.indexOf(':', tagS);
        if (tagE < 0) return false;
        int realE = tagE;
        while (realE > tagS && line.charAt(realE-1) == ' ') {
            realE--;
        }
        tag = line.substring(tagS, realE);

        // Read message
        if (tagE + 2 < line.length()) {
            msg = line.substring(tagE + 2);
        } else {
            msg = "";
        }

        finishParse(br);
        fmt = FMT_BRAT;
        return true;
    }

    /**
     * Parse a log line in the short format (no timestamp, default when just running "adb logcat")
     */
    private boolean parseFmtShort(BugReportModule br, String line, LogLine prev) {
        int tagEnd = line.indexOf(':', 10);
        if (tagEnd < 10) return false; // this is weird... abort
        int p0 = tagEnd - 7;
        int p1 = tagEnd - 1;
        char c = line.charAt(p0);
        if (c != '(') return false; // not this format
        c = line.charAt(p1);
        if (c != ')') return false; // not this format
        if (line.charAt(1) != '/') return false; // strange log format
        c = line.charAt(0);
        if (-1 == "FEWIDV".indexOf(c)) return false; // strange log format

        // No timestamp, so autogenerate one just so we can render some charts
        ts = (prev == null) ? 0 : prev.ts + 10;

        // Read log level
        level = line.charAt(0);

        // Read pid
        int p = p0;
        do {
            p++;
        } while (p < line.length() && line.charAt(p) == ' ');
        try {
            pid = Integer.parseInt(line.substring(p, p1));
        } catch(NumberFormatException t) {
            return false; // strange pid
        }
        pidS = p;
        pidE = p1;

        // Read tag
        int tagS = 2;
        int tagE = p0;
        if (tagE < 0) return false;
        while (tagE > tagS && line.charAt(tagE-1) == ' ') {
            tagE--;
        }
        tag = line.substring(tagS, tagE);

        // Read message
        msg = line.substring(p1 + 3);

        finishParse(br);
        fmt = FMT_SHORT;
        return true;
    }

    /**
     * This is a safe way to access fields.
     * If a given field does not exists, null will be returned (instead of throwing an exception)
     * @param idx The index of the field
     * @return The field value or null if the field is missing
     */
    public String getFields(int idx) {
        return (idx < fields.length) ? fields[idx] : null;
    }

    @Override
    protected void renderThis(Renderer r) throws IOException {
        if (mPr == null) {
            r.println("<div class=\"" + css + "\" id=\"l" + id + "\">" + HtmlUtil.escape(line) + "</div>");
        } else {
            String prHRef = mPr.getAnchor().getHRef();
            r.println("<div class=\"" + css + "\" id=\"l" + id + "\">" +
                    HtmlUtil.escape(line.substring(0, pidS)) +
                    "<a href=\"" + prHRef + "\">" + pid + "</a>" +
                    HtmlUtil.escape(line.substring(pidE)) +
                    "</div>");
        }
    }

}

