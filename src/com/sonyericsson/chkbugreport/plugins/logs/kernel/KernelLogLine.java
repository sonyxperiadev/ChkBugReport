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

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.ProcessRecord;
import com.sonyericsson.chkbugreport.doc.Renderer;
import com.sonyericsson.chkbugreport.plugins.logs.LogLineBase;
import com.sonyericsson.chkbugreport.util.HtmlUtil;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KernelLogLine extends LogLineBase {

    private static final Pattern TS = Pattern.compile("\\[ *([0-9]+)\\.([0-9]+)\\].*");

    public int level = -1;

    public int pidS;
    public int pidE;

    public long realTs;

    /**
     * Constructs a KernelLogLine.
     */
    public KernelLogLine(BugReportModule br, String line, KernelLogLine prev, long realTs) {
        super(line);
        this.realTs = realTs;

        parse(line);
        // mLevel, mMsg and mKernelTime are set in parse()

        // mLineHtml is set in generateHtml()
    }

    public KernelLogLine(KernelLogLine orig) {
        super(orig);
        msg = orig.msg;
        level = orig.level;
        pidS = orig.pidS;
        pidE = orig.pidE;
        realTs = orig.realTs;
    }

    @Override
    public KernelLogLine copy() {
        return new KernelLogLine(this);
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
        this.line = line;
        msg = line;
        tag = "kernel";

        // Parse priority
        if (line.length() >= 3) {
            if (line.charAt(0) == '<' && line.charAt(2) == '>') {
                char c = line.charAt(1);
                if (c <= '0' && c <= '7') {
                    level = c - '0';
                }
                line = line.substring(3);
            }
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
            ts = Integer.parseInt(first) * 1000L + Integer.parseInt(second) / 1000L;
        } catch (Exception e) {
            return;
        }
        if (ts < 0) {
            ts = 0;
            return;
        }

        msg = line;
        setColorFromLevel();
        ok = true;
    }

    private void setColorFromLevel() {
        switch (level) {
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
            css = "log-debug";
            break;
        }
    }

    @Override
    protected void renderThis(Renderer r) throws IOException {
        renderChildren(r);

        // Colorize based on level
        r.print("<div class=\"");
        r.print(css);
        r.print("\" id=\"l");
        r.print(id);
        r.print("\">");
        if (pidS != pidE) {
            try {
                int pid = Integer.parseInt(line.substring(pidS, pidE));
                ProcessRecord pr = ((BugReportModule)r.getModule()).getProcessRecord(pid, false, false);
                if (pr != null && pr.isExported()) {
                    r.print(HtmlUtil.escape(line.substring(0, pidS)));
                    r.print("<a href=\"" + pr.getAnchor().getHRef() + "\">");
                    r.print(pid);
                    r.print("</a>");
                    r.print(HtmlUtil.escape(line.substring(pidE)));
                }
            } catch (NumberFormatException nfe) {
                r.print(HtmlUtil.escape(line));
            }
        } else {
            r.print(HtmlUtil.escape(line));
        }
        r.print("</div>");
    }

    public void markPid(int start, int end) {
        pidS = start;
        pidE = end;
    }

}

