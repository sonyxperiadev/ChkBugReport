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
import com.sonyericsson.chkbugreport.doc.Anchor;
import com.sonyericsson.chkbugreport.doc.Block;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.doc.Renderer;
import com.sonyericsson.chkbugreport.doc.SimpleText;
import com.sonyericsson.chkbugreport.util.HtmlUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogLine extends DocNode {

    private static final Pattern TS = Pattern.compile("\\[ *([0-9]+)\\.([0-9]+)\\].*");

    public static final int FMT_UNKNOWN = 0;
    public static final int FMT_STD     = 1;
    public static final int FMT_BRAT    = 2;
    public static final int FMT_SHORT   = 3;
    public static final int FMT_KERNEL  = 4;

    public char level;
    public String line;
    public String css;
    public long ts;
    public long realTs;
    public long id;
    public String tag;
    public String msg;
    public int pid = 0;
    public String[] fields;
    public int fmt = FMT_UNKNOWN;
    public boolean ok = false;

    private boolean mHidden;
    private Anchor mAnchor;
    private Vector<Decorator> mDecors;

    private static final Comparator<Decorator> sDecorSorter = new Comparator<Decorator>() {
        @Override
        public int compare(Decorator o1, Decorator o2) {
            return o1.compare(o2);
        }
    };

    public LogLine(BugReportModule br, String line, int format, LogLine prev) {
        this.line = line;
        css = "log-debug";
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
            case FMT_KERNEL:
                parseFmtKernel(line);
                break;
            default: throw new RuntimeException("Invalid format: " + format);
        }
    }

    public LogLine(LogLine orig) {
        line = orig.line;
        css = orig.css;
        ts = orig.ts;
        ok = orig.ok;
        id = orig.id;
        tag = orig.tag;
        msg = orig.msg;
        level = orig.level;
        pid = orig.pid;
        fields = orig.fields;
        fmt = orig.fmt;
        mHidden = orig.mHidden;
        for (int i = 0; i < orig.getChildCount(); i++) {
            add(orig.getChild(i));
        }
    }

    public void setHidden(boolean b) {
        mHidden = b;
    }

    public boolean isHidden() {
        return mHidden;
    }

    public void addStyle(String style) {
        css += " " + style;
    }

    public void addMarker(String css, String msg, String title) {
        if (title == null) {
            title = msg.replace("<br/>", "\n");
        }
        if (css == null) {
            css = "log-float";
        }
        addMarker(css, new SimpleText(msg), title);
    }

    public void addMarker(String css, DocNode msg, String title) {
        Block box = new Block(this);
        box.addStyle(css);
        box.setTitle(title);
        box.add(msg);
    }

    @Override
    public final void render(Renderer r) throws IOException {
        renderAnchor(r);
        renderChildren(r);
        renderThis(r);
    }

    @Override
    public void prepare(Renderer r) {
        prepareAnchor(r);
        prepareChildren(r);
    }

    private void prepareChildren(Renderer r) {
        super.prepare(r);
    }

    private void prepareAnchor(Renderer r) {
        if (mAnchor != null) {
            mAnchor.prepare(r);
        }
    }

    private void renderAnchor(Renderer r) {
        if (mAnchor != null) {
            mAnchor.render(r);
        }
    }

    public Anchor getAnchor() {
        if (mAnchor == null) {
            mAnchor = new Anchor("l" + ts);
        }
        return mAnchor;
    }

    public LogLineProxy symlink() {
        return new LogLineProxy(this);
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
            if (pid > 0) {
                addDecorator(new PidDecorator(p, p1, br, pid));
            }
        } catch(NumberFormatException t) {
            return false; // strange pid
        }

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

        finishParse();
        fmt = FMT_STD;
        return true;
    }

    private void finishParse() {
        // Colorize based on level
        switch (level) {
        case '0': // KERN_EMERG
        case '1': // KERN_ALERT
        case '2': // KERN_CRIT
        case 'F': css = "log-fatal"; break;
        case '3': // KERN_ERR
        case 'E': css = "log-error"; break;
        case '4': // KERN_WARNING
        case 'W': css = "log-warning"; break;
        case '5': // KERN_NOTICE
        case '6': // KERN_INFO
        case 'I': css = "log-info"; break;
        case '7': // KERN_DEBUG
        case 'D': css = "log-debug"; break;
        default:
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
            realTs = ts;
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
        int pidS = 19;
        int pidE = 24;
        while (line.charAt(pidS) == ' ') pidS++;
        if (pidS >= pidE) return false;
        try {
            pid = Integer.parseInt(line.substring(pidS, pidE));
            if (pid > 0) {
                addDecorator(new PidDecorator(pidS, pidE, br, pid));
            }
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

        finishParse();
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
        realTs = 0;

        // Read log level
        level = line.charAt(0);

        // Read pid
        int p = p0;
        do {
            p++;
        } while (p < line.length() && line.charAt(p) == ' ');
        try {
            pid = Integer.parseInt(line.substring(p, p1));
            if (pid > 0) {
                addDecorator(new PidDecorator(p, p1, br, pid));
            }
        } catch(NumberFormatException t) {
            return false; // strange pid
        }

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

        finishParse();
        fmt = FMT_SHORT;
        return true;
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
    private boolean parseFmtKernel(String line) {
        msg = line;
        tag = "kernel";

        // Parse priority
        if (line.length() >= 3) {
            if (line.charAt(0) == '<' && line.charAt(2) == '>') {
                char c = line.charAt(1);
                if (c <= '0' && c <= '7') {
                    level = c;
                }
                line = line.substring(3);
            }
        }

        // Parse timestamp
        if (line.length() < 14) {
            return false;
        }

        // Timestamp
        if (line.charAt(0) != '[') {
            // The timestamp is mandatory
            return false;
        }

        Matcher m = TS.matcher(line);
        if (m.matches()) {
            int closePos = line.indexOf(']');
            line = line.substring(closePos + 2);
        }
        try {
            String first = m.group(1);
            String second = m.group(2);
            ts = Integer.parseInt(first) * 1000L + Integer.parseInt(second) / 1000L;
        } catch (Exception e) {
            return false;
        }
        if (ts < 0) {
            ts = 0;
            return false;
        }

        msg = line;
        finishParse();
        fmt = FMT_KERNEL;
        return true;
    }

    public void addDecorator(Decorator d) {
        if (d.isEmpty()) return;
        if (mDecors == null) {
            mDecors = new Vector<Decorator>();
        }
        mDecors.add(d);
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

    protected void renderThis(Renderer r) throws IOException {
        if (mDecors == null) {
            r.println("<div class=\"log-line " + css + "\" id=\"l" + id + "\">" + HtmlUtil.escape(line) + "</div>");
        } else {
            // So, this is tricky, since we have to render piecewise.
            // There can be any number of decorator segments, and they can even overlap.
            // Since we are rendering html, we ignore segments which intersect others, i.e.
            // we accept only those overlapping ones, which are fully contained in the previous one
            Collections.sort(mDecors, sDecorSorter);
            int pos = 0, nextEnd = -1;
            Stack<Decorator> open = new Stack<Decorator>();
            Iterator<Decorator> iter = mDecors.iterator();
            Decorator nextDecor = iter.next();
            r.print("<div class=\"log-line " + css + "\" id=\"l" + id + "\">");

            // Repeat as long as there is an open or an unprocessed decorator
            while (nextEnd >= 0 || nextDecor != null) {
                // Case 1: the next decor comes after the current one is closed
                // or there is no next one
                if ((nextEnd >= 0 && nextDecor != null && nextEnd <= nextDecor.getStart()) || (nextEnd >= 0 && nextDecor == null)) {
                    // Close the top-most opened decor
                    r.print(HtmlUtil.escape(line.substring(pos, nextEnd)));
                    pos = nextEnd;
                    open.pop().render(r, false);
                    nextEnd = open.isEmpty() ? -1 : open.peek().getEnd();
                    continue;
                }

                // Case 2: the next decor intersects the currently opened one
                if (nextEnd >= 0 && nextDecor != null && nextEnd > nextDecor.getStart() && nextEnd < nextDecor.getEnd()) {
                    // Ignore next decor
                    nextDecor = iter.hasNext() ? iter.next() : null;
                    continue;
                }

                // Case 3: the next decor inside the currently opened one
                // or no opened decor et
                if ((nextEnd >= 0 && nextDecor != null && nextEnd > nextDecor.getStart() && nextEnd >= nextDecor.getEnd()) || (nextEnd < 0 && nextDecor != null)) {
                    // Add next decor to stack
                    int nextPos = nextDecor.getStart();
                    r.print(HtmlUtil.escape(line.substring(pos, nextPos)));
                    nextDecor.render(r, true);
                    pos = nextPos;
                    open.push(nextDecor);
                    nextEnd = nextDecor.getEnd();
                    nextDecor = iter.hasNext() ? iter.next() : null;
                    continue;
                }
            }

            // Finish remaining line
            r.print(HtmlUtil.escape(line.substring(pos, line.length())));
            r.println("</div>");
        }
    }

    /* package */ static class LogLineProxy extends DocNode {

        private LogLine mLogLine;

        public LogLineProxy(LogLine logLine) {
            mLogLine = logLine;
        }

        @Override
        public void render(Renderer r) throws IOException {
            mLogLine.renderThis(r);
        }

    }

}

