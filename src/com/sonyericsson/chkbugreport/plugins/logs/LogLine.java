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
package com.sonyericsson.chkbugreport.plugins.logs;

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.ProcessRecord;
import com.sonyericsson.chkbugreport.doc.Block;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.doc.Renderer;
import com.sonyericsson.chkbugreport.doc.SimpleText;
import com.sonyericsson.chkbugreport.util.HtmlUtil;
import com.sonyericsson.chkbugreport.util.Util;

import java.io.IOException;

public class LogLine extends LogLineBase {

    public static final int FMT_UNKNOWN = 0;
    public static final int FMT_STD     = 1;
    public static final int FMT_BRAT    = 2;
    public static final int FMT_CRASH   = 3;
    public static final int FMT_SHORT   = 4;

    public char level;

    public int pid = 0;
    public int pidS = -1;
    public int pidE = -1;

    public String tag;
    public int tagS = -1;
    public int tagE = -1;
    public int tagId;

    public String msg;
    public int msgS = -1;
    public int msgE = -1;

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
                        if (!parseFmtCrash(br, line, prev)) {
                            parseFmtShort(br, line, prev);
                        }
                    }
                }
                break;
            case FMT_STD:
                parseFmtStd(br, line);
                break;
            case FMT_BRAT:
                parseFmtBrat(br, line);
                break;
            case FMT_CRASH:
                parseFmtCrash(br, line, prev);
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
        tagS = 21;
        tagE = p0;
        if (tagE < 0) return false;
        while (tagE > tagS && line.charAt(tagE-1) == ' ') {
            tagE--;
        }
        tag = line.substring(tagS, tagE);

        // Read message
        msg = line.substring(p1 + 3);
        msgS = p1 + 3;
        msgE = line.length();

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
        tagS = 33;
        tagE = line.indexOf(':', tagS);
        if (tagE < 0) return false;
        int realE = tagE;
        while (realE > tagS && line.charAt(realE-1) == ' ') {
            realE--;
        }
        tag = line.substring(tagS, realE);

        // Read message
        if (tagE + 2 < line.length()) {
            msg = line.substring(tagE + 2);
            msgS = tagE + 2;
            msgE = line.length();
        } else {
            msg = "";
            msgS = msgE = line.length();
        }

        finishParse(br);
        fmt = FMT_BRAT;
        return true;
    }

    /**
     * Parse a log line from logs generated by crash
     */
    private boolean parseFmtCrash(BugReportModule br, String line, LogLine prev) {
        if (line.length() <= 33) return false; // just some sane value... maybe it should be 23?

        // Do some initial verification
        boolean newLine = true;
        char c = line.charAt(8);
        if (c != '[') newLine = false;
        c = line.charAt(16);
        if (c != '.') newLine = false;
        c = line.charAt(23);
        if (c != ']') newLine = false;

        if (!newLine) {
            // Continuation of the previous one
            if (prev == null) {
                // No previous line, we cannot continue, ignore
                return false;
            }
            // Copy the important fields from the previous line
            level = prev.level;
            ts = prev.ts;
            pidS = prev.pidS;
            pidE = prev.pidE;
            pid = prev.pid;
            tagS = prev.tagS;
            tagE = prev.tagE;
            tag = prev.tag;
            msgS = prev.msgS;
            msgE = msgS + line.length();
            msg = line;
            line = prev.line.substring(0, prev.msgS) + msg;
            this.line = line;
        } else {
            // This will hold the rebuilt line
            StringBuffer sb = new StringBuffer();

            // Parse the level
            level = line.charAt(0);
            if (level == ' ') {
                level = line.charAt(2);
            }
            if (level == ' ') {
                level = line.charAt(3);
            }
            if (level == ' ') {
                level = 'I';
            }

            // Parse the timestamp
            try {
                int sec = Integer.parseInt(Util.strip(line.substring(9, 16)));
                int usec = Integer.parseInt(Util.strip(line.substring(17, 23)));
                ts = sec * 1000 + usec / 1000;
                sb.append(String.format("%02d:%02d:%02d.%03d ",
                        (sec / 3600) % 24, (sec / 60) % 60, sec % 60, usec / 1000));
            } catch (NumberFormatException e) {
                return false; // wrong ts? or wrong format?
            }

            // Parse pid
            pidS = line.indexOf('(');
            pidE = line.indexOf(':', pidS);
            if (pidS < 0 || pidE < 0) {
                return false; // wrong format?
            }
            pidS++;
            try {
                pid = Integer.parseInt(line.substring(pidS, pidE));
            } catch(NumberFormatException t) {
                return false; // strange pid
            }

            // Parse tag
            tagS = line.indexOf(')');
            if (tagS < 0) return false;
            tagS += 2;
            tagE = line.indexOf(" ", tagS); // This might not work for tags which actually contain a space, but there is no more reliable way
            if (tagE < 0) return false;
            tag = line.substring(tagS, tagE);

            // This could be an event tag, extract the tag id
            int idx0 = tag.indexOf('(');
            int idx1 = tag.indexOf(')');
            try {
                if (idx0 == -1 && idx1 == -1) {
                    tagId = Integer.parseInt(tag);
                } else if (idx0 != -1 && idx1 != -1 && idx0 < idx1) {
                    tagId = Integer.parseInt(tag.substring(idx0 + 1, idx1));
                }
            } catch (NumberFormatException nfe) { /* ignore */ }

            // Read message
            int idx = tagE + 1;
            while (idx < line.length() && line.charAt(idx) == ' ') {
                idx++;
            }
            if (idx < line.length()) {
                msg = line.substring(idx);
                msgS = idx;
                msgE = line.length();
            } else {
                msg = "";
                msgS = msgE = line.length();
            }

            // Finish reconstruction
            sb.append(level);
            sb.append('/');
            tagS = sb.length();
            sb.append(tag);
            tagE = sb.length();
            sb.append('(');
            pidS = sb.length();
            sb.append(pid);
            pidE = sb.length();
            sb.append("): ");
            msgS = sb.length();
            sb.append(msg);
            msgE = sb.length();
            line = sb.toString();
            this.line = line;
        }

        finishParse(br);

        // Override field parsing (Do some basic field parsing... very basic)
        if (msg != null) {
            fields = msg.split(",");
            for (int i = 0; i < fields.length; i++) {
                fields[i] = Util.strip(fields[i]);
            }
        }

        fmt = FMT_CRASH;
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
        tagS = 2;
        tagE = p0;
        if (tagE < 0) return false;
        while (tagE > tagS && line.charAt(tagE-1) == ' ') {
            tagE--;
        }
        tag = line.substring(tagS, tagE);

        // Read message
        msg = line.substring(p1 + 3);
        msgS = p1 + 3;
        msgE = line.length();

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

    public void addMarker(String css, String msg, String title) {
        if (title == null) {
            title = msg.replace("<br/>", "\n");
        }
        addMarker(css, new SimpleText(msg), title);
    }

    public void addMarker(String css, DocNode msg, String title) {
        Block box = new Block(this);
        box.addStyle(css);
        box.setTag(title);
        box.add(msg);
    }

    @Override
    protected void renderThis(Renderer r) throws IOException {
        if (mPr == null) {
            r.println("<div class=\"" + css + "\">" + HtmlUtil.escape(line) + "</div>");
        } else {
            String prFn = mPr.getAnchor().getFileName();
            String prA = mPr.getAnchor().getName();
            r.println("<div class=\"" + css + "\">" +
                    HtmlUtil.escape(line.substring(0, pidS)) +
                    "<a href=\"" + prFn + "#" + prA + "\">" + pid + "</a>" +
                    HtmlUtil.escape(line.substring(pidE)) +
                    "</div>");
        }
    }

}

