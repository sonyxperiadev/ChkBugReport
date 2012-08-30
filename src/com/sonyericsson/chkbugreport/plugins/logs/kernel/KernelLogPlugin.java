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

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sonyericsson.chkbugreport.Bug;
import com.sonyericsson.chkbugreport.BugReport;
import com.sonyericsson.chkbugreport.Chapter;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.Report;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.Util;

public class KernelLogPlugin extends Plugin {
    public static final String TAG = "[KernelLogPlugin]";

    private static final Pattern SELECT_TO_KILL = Pattern.compile(".*] select (\\d+) .*, to kill$");
    private static final Pattern SEND_SIGKILL = Pattern.compile(".*] send sigkill to (\\d+) .*");
    private static final Pattern BINDER_RELEASE_NOT_FREED = Pattern.compile(".*] binder: release proc (\\d+)" +
            ", transaction (\\d+), not freed$");

    private Chapter mCh;
    private boolean mLoaded = false;
    private Vector<KernelLogLine> mParsedLog = new Vector<KernelLogLine>();
    int mUnparsed;


    @Override
    public int getPrio() {
        return 31;
    }

    @Override
    public void load(Report rep) {
        BugReport br = (BugReport)rep;

        // Reset previous data
        mParsedLog.clear();
        mLoaded = false;
        mCh = null;
        mUnparsed = 0;

        // Load the data
        Section section = br.findSection(Section.KERNEL_LOG);
        if (section == null) {
            br.printErr(3, TAG + "Cannot find section Kernel log (aborting plugin)");
            return;
        }

        // Load and parse the lines
        mCh = new Chapter(br, "Kernel log");
        int cnt = section.getLineCount();
        KernelLogLine prev = null;
        for (int i = 0; i < cnt; i++) {
            String line = section.getLine(i);
            KernelLogLine kl = new KernelLogLine(br, line, prev);
            if (kl.mOk) {
                mParsedLog.add(kl);
            } else {
                mUnparsed++;
            }
        }
        cnt = mParsedLog.size();

        // Annotate the log
        for (int i = 0; i < cnt; i++) {
            KernelLogLine kl = mParsedLog.get(i);
            annotate(kl, br, i);
        }

        // Analyze the log
        for (int i = 0; i < cnt;) {
            KernelLogLine kl = mParsedLog.get(i);
            i += analyze(kl, i, br, section);
        }

        // Load successful
        mLoaded = true;
    }

    /**
     * Generate the HTML document for the kernel log section.
     */
    @Override
    public void generate(Report rep) {
        BugReport br = (BugReport)rep;

        if (!mLoaded) {
            return;
        }

        if (mUnparsed > 0) {
            mCh.addLine("<div class=\"hint\">(Note: " + mUnparsed + " lines were not parsed)</div>");
        }

        br.addChapter(mCh);
        generateLog(br);
    }

    private void generateLog(BugReport br) {
        Chapter ch = new Chapter(br, "Log");
        mCh.addChapter(ch);

        ch.addLine("<div class=\"log\">");

        int cnt = mParsedLog.size();
        for (int i = 0; i < cnt; i++) {
            KernelLogLine kl = mParsedLog.get(i);

            for (String prefix : kl.prefixes) {
                ch.addLine(prefix);
            }
            ch.addLine(kl.mLineHtml);
        }

        ch.addLine("</div>");
    }

    /**
     * Analyze a log line to see if it can be annotated with further information, such as links to
     * referenced data.
     */
    private void annotate(KernelLogLine kl, BugReport br, int i) {
        annotateSelectToKill(kl, br);
        annotateSendSigkill(kl, br);
        annotateBinderReleaseNotFreed(kl, br);

        kl.generateHtml();
    }

    /**
     * Link the pid to its process record in a "select to kill" line.
     *
     * select 13814 (android.support), adj 8, size 5977, to kill
     */
    private void annotateSelectToKill(KernelLogLine kl, BugReport br) {
        Matcher matcher = SELECT_TO_KILL.matcher(kl.mLine);
        if (!matcher.matches()) {
            return;
        }

        int start = matcher.start(1);
        int end = matcher.end(1);
        String pid = matcher.group(1);
        try {
            kl.mLineHtml = kl.mLine.substring(0, start) + "<a href=\""
                    + br.createLinkToProcessRecord(Integer.parseInt(pid)) + "\">" + pid + "</a>"
                    + Util.escape(kl.mLine.substring(end));
        } catch (NumberFormatException nfe) {
            // Ignore
        }
    }

    /**
     * Link the pid to its process record in a "send sigkill" line.
     *
     * send sigkill to 8506 (et.digitalclock), adj 10, size 5498
     */
    private void annotateSendSigkill(KernelLogLine kl, BugReport br) {
        Matcher matcher = SEND_SIGKILL.matcher(kl.mLine);
        if (!matcher.matches()) {
            return;
        }

        int start = matcher.start(1);
        int end = matcher.end(1);
        String pid = matcher.group(1);
        try {
            kl.mLineHtml = kl.mLine.substring(0, start) + "<a href=\""
            + br.createLinkToProcessRecord(Integer.parseInt(pid)) + "\">" + pid + "</a>"
            + Util.escape(kl.mLine.substring(end));
        } catch (NumberFormatException nfe) {
            // Ignore
        }
    }

    /**
     * Link the pid to its process record in a "binder: release not freed" line.
     *
     * binder: release proc 9107, transaction 1076363, not freed
     */
    private void annotateBinderReleaseNotFreed(KernelLogLine kl, BugReport br) {
        Matcher matcher = BINDER_RELEASE_NOT_FREED.matcher(kl.mLine);
        if (!matcher.matches()) {
            return;
        }

        int start = matcher.start(1);
        int end = matcher.end(1);
        String pid = matcher.group(1);
        try {
            kl.mLineHtml = kl.mLine.substring(0, start) + "<a href=\""
            + br.createLinkToProcessRecord(Integer.parseInt(pid)) + "\">" + pid + "</a>"
            + kl.mLine.substring(end);
        } catch (NumberFormatException nfe) {
            // Ignore
        }
    }

    /**
     * Analyze the log starting from line 'i' to see if it contain any
     * information that will result in a Bug.
     *
     * Return the number of lines consumed.
     */
    private int analyze(KernelLogLine kl, int i, BugReport br, Section s) {
        int inc = analyzeFatal(kl, br, i, s);
        if (inc > 0) return inc;


        return 1;
    }

    /**
     * Generate a Bug for each block of log lines with a level of 1 or 2.
     */
    private int analyzeFatal(KernelLogLine kl, BugReport br, int i, Section s) {
        // Put a marker box
        String anchor = "kernel_log_fe_" + i;
        String type;
        int level = kl.getLevel();
        switch (level) {
            case 1:
                type = "EMERGENCY";
                break;
            case 2:
                type = "ALERT";
                break;
            default:
                return 1;
        }
        kl.addMarker("log-float-err", null, "<a name=\"" + anchor + "\">KERNEL<br/>" + type
                + "</a>", "KERNEL " + type);

        // Create a bug and store the relevant log lines
        Bug bug = new Bug(Bug.PRIO_ALERT_KERNEL_LOG, kl.mKernelTime, "KERNEL " + type);
        bug.addLine("<div><a href=\"" + br.createLinkTo(mCh, anchor) + "\">(link to log)</a></div>");
        bug.addLine("<div class=\"log\">");
        bug.addLine(kl.mLineHtml);
        int end = i + 1;
        while (end < s.getLineCount()) {
            KernelLogLine extra = mParsedLog.get(end);
            if (extra.getLevel() != level)
                break;
            bug.addLine(extra.mLineHtml);
            end++;
        }
        bug.addLine("</div>");
        bug.setAttr("firstLine", i);
        bug.setAttr("lastLine", end);
        bug.setAttr("section", s);
        br.addBug(bug);

        return end - i;
    }

}
