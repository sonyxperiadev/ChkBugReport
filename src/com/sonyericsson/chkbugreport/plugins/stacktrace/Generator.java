package com.sonyericsson.chkbugreport.plugins.stacktrace;

import com.sonyericsson.chkbugreport.BugReport;
import com.sonyericsson.chkbugreport.Chapter;
import com.sonyericsson.chkbugreport.PSRecord;
import com.sonyericsson.chkbugreport.ProcessRecord;
import com.sonyericsson.chkbugreport.Report;
import com.sonyericsson.chkbugreport.Util;

import java.util.Calendar;
import java.util.Vector;

public class Generator {

    private StackTracePlugin mPlugin;

    public Generator(StackTracePlugin plugin) {
        mPlugin = plugin;
    }

    public void generate(Report rep, Processes processes) {
        BugReport br = (BugReport)rep;

        int id = processes.getId();
        String chapterName = processes.getName();

        // Generate chapter
        genChapter(br, id, processes, chapterName);
    }

    private void genChapter(BugReport br, int id, Processes processes, String chapterName) {
        Chapter main = processes.getChapter();
        Calendar tsBr = br.getTimestamp();
        Calendar tsSec = Util.parseTimestamp(br, processes.getSectionName());
        String diff = Util.formatTimeDiff(tsBr, tsSec, true);
        diff = (diff == null) ? "" : "; " + diff + "";
        main.addLine("<div class=\"hint\">(Generated from : \"" + processes.getSectionName() + "\")" + diff + "</div>");

        Vector<StackTrace> busy = processes.getBusyStackTraces();
        if (busy.size() > 0) {
            main.addLine("<p>");
            main.addLine("The following threads seems to be busy:");
            main.addLine("<div class=\"hint\">(NOTE: there might be some more busy threads than these, " +
                    "since this tool has only a few patterns to recognise busy threads. " +
                    "Currently only binder threads and looper based threads are recognised.)</div>");
            main.addLine("</p>");

            main.addLine("<ul class=\"stacktrace-busy-list\">");
            for (StackTrace stack : busy) {
                Process proc = stack.getProcess();
                String anchorTrace = proc.getAnchor(stack);
                String link = br.createLinkTo(processes.getChapter(), anchorTrace);
                main.addLine("  <li><a href=\"" + link + "\">" + proc.getName() + "/" + stack.getName() + "</a></li>");
            }
            main.addLine("</ul>");
        }

        for (Process p : processes) {
            String anchor = p.getAnchor();
            Chapter ch = new Chapter(br, p.getName() + " (" + p.getPid() + ")");
            main.addChapter(ch);
            ch.addLine("<a name=\"" + anchor + "\"></a>");

            // Add timestamp
            String dateTime = p.getDate() + " " + p.getTime();
            Calendar tsProc = Util.parseTimestamp(br, dateTime);
            diff = Util.formatTimeDiff(tsBr, tsProc, true);
            diff = (diff == null) ? "" : "; " + diff + "";
            ch.addLine("<div class=\"hint\">(" + dateTime + diff + ")</div>");

            // Add link from global process record
            ProcessRecord pr = br.getProcessRecord(p.getPid(), true, true);
            pr.suggestName(p.getName(), 50);
            pr.beginBlock();
            String link = br.createLinkTo(processes.getChapter(), anchor);
            pr.addLine("<a href=\"" + link + "\">");
            if (id == StackTracePlugin.ID_NOW) {
                pr.addLine("Current stack trace &gt;&gt;&gt;");
            } else if (id == StackTracePlugin.ID_ANR) {
                pr.addLine("Stack trace at last ANR &gt;&gt;&gt;");
            } else if (id == StackTracePlugin.ID_OLD) {
                pr.addLine("Old stack traces &gt;&gt;&gt;");
            } else {
                // Try to use some generic wording
                pr.addLine("Related stack traces &gt;&gt;&gt;");
            }
            pr.addLine("</a>");
            pr.endBlock();

            int cnt = p.getCount();
            for (int i = 0; i < cnt; i++) {
                StackTrace stack = p.get(i);
                String anchorTrace = p.getAnchor(stack);
                String waiting = "";
                int waitOn = stack.getWaitOn();
                if (waitOn >= 0) {
                    String anchorWait = anchor + "_" + waitOn;
                    String linkWait = br.createLinkTo(processes.getChapter(), anchorWait);
                    waiting += " waiting on <a href=\"" + linkWait + "\">thread-" + waitOn + "</a>";
                }
                StackTrace aidlDep = stack.getAidlDependency();
                if (aidlDep != null) {
                    Process aidlDepProc = aidlDep.getProcess();
                    String anchorWait = aidlDepProc.getAnchor(aidlDep);
                    String linkWait = br.createLinkTo(processes.getChapter(), anchorWait);
                    waiting += " waiting on <a href=\"" + linkWait + "\">" + aidlDepProc.getName() + "/" + aidlDep.getName() + "</a>";
                }
                String sched = parseSched(stack.getProperty("sched"));
                String nice = parseNice(stack.getProperty("nice"));
                ch.addLine("<a name=\"" + anchorTrace + "\"></a>");
                ch.addLine("<div class=\"stacktrace\">");
                ch.addLine("<div class=\"stacktrace-name\">");
                ch.addLine("  <span>-</span>");
                ch.addLine("  <span class=\"stacktrace-name-name\">" + stack.getName() + "</span>");
                ch.addLine("  <span class=\"stacktrace-name-info\"> " +
                        "(tid=" + stack.getTid() +
                        " pid=" + stack.getProperty("sysTid") +
                        " prio=" + stack.getPrio() +
                        " " + nice +
                        " " + sched +
                        " state=" + stack.getState() +
                        waiting +
                        ")</span>");
                ch.addLine("</div>");
                ch.addLine("<div class=\"stacktrace-items\">");
                int itemCnt = stack.getCount();
                for (int j = 0; j < itemCnt; j++) {
                    StackTraceItem item = stack.get(j);
                    ch.addLine("<div class=\"stacktrace-item\">");
                    ch.addLine("  <span class=\"stacktrace-item-method " + item.getStyle() + "\">" + item.getMethod() + "</span>");
                    if (item.getFileName() != null) {
                        ch.addLine("  <span class=\"stacktrace-item-file\">(" + item.getFileName() + ":" + item.getLine() + ")</span>");
                    }
                    ch.addLine("</div>");
                }
                ch.addLine("</div>");
                ch.addLine("</div>");
            }

            cnt = p.getUnknownThreadCount();
            if (cnt > 0) {
                ch.addLine("<div class=\"stacktrace-unknown\">");
                ch.addLine("<p>Other/unknown threads:</p>");
                ch.addLine("<div class=\"hint\">(These are child processes/threads of this application, but there is no information about the stacktrace of them. They are either native threads, or dalvik threads which haven't existed yet when the stack traces were saved)</div>");
                ch.addLine("<ul>");
                for (int i = 0; i < cnt; i++) {
                    PSRecord psr = p.getUnknownThread(i);
                    ch.addLine("<li>" + psr.getName() + "(" + psr.getPid() + ")</li>");
                }
                ch.addLine("</ul>");
                ch.addLine("</div>");
            }
        }

        if (processes.getId() < StackTracePlugin.ID_SLOW) {
            br.addChapter(main);
        } else {
            mPlugin.addSlowChapter(br, main);
        }
    }

    private String parseSched(String sched) {
        int ret = PSRecord.PCY_UNKNOWN;
        String fields[] = sched.split("/");
        try {
            int v = Integer.parseInt(fields[0]);
            switch (v) {
                case 0: ret = PSRecord.PCY_NORMAL; break;
                case 1: ret = PSRecord.PCY_FIFO; break;
                case 3: ret = PSRecord.PCY_BATCH; break;
            }
        } catch (Exception e) { /* NOP */ }
        return Util.getSchedImg(ret);
    }

    private String parseNice(String sNice) {
        int nice = PSRecord.NICE_UNKNOWN;
        try {
            nice = Integer.parseInt(sNice);
        } catch (Exception e) { /* NOP */ }
        return Util.getNiceImg(nice);
    }


}
