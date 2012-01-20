/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Vector;

import com.sonyericsson.chkbugreport.Bug;
import com.sonyericsson.chkbugreport.BugReport;
import com.sonyericsson.chkbugreport.Chapter;
import com.sonyericsson.chkbugreport.PSRecord;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.ProcessRecord;
import com.sonyericsson.chkbugreport.Report;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.Util;

/**
 * Processes the current stacktrace and the stacktrace at the last ANR.
 *
 * If DB access is working (in other words the sqlite jdbc driver is in the classpath),
 * then the stacktraces will be imported also in the database.
 *
 * Here is an example query which lists the number of threads per process:
 *
 *   select count(*) as nr_threads,p.name,p.pid,p.group_id
 *     from stacktrace_processes p
 *     inner join stacktrace_threads t on p.id = t.process_id
 *     group by p.id, p.group_id
 *     order by nr_threads desc
 *
 */
public class StackTracePlugin extends Plugin {

    private static final String TAG = "[StackTracePlugin]";

    private static final int STATE_INIT  = 0;
    private static final int STATE_PROC  = 1;
    private static final int STATE_STACK = 2;

    private static final int ID_NOW = 1;
    private static final int ID_ANR = 2;
    private static final int ID_OLD = 3;

    private HashMap<Integer, Processes> mProcesses = new HashMap<Integer, Processes>();

    private Connection mConn;

    @Override
    public int getPrio() {
        return 10;
    }

    @Override
    public void load(Report br) {
        // Reset state
        mConn = null;
        mProcesses.clear();

        // Load data
        mConn = br.getSQLConnection();

        run(br, ID_NOW, "VM TRACES JUST NOW", "VM traces just now");
        run(br, ID_ANR, "VM TRACES AT LAST ANR", "VM traces at last ANR");
        // backward compatibility
        run(br, ID_OLD, "VM TRACES", "VM traces");
    }

    private void run(Report rep, int id, String sectionName, String chapterName) {
        BugReport br = (BugReport)rep;
        Section sec = br.findSection(sectionName);
        if (sec == null) {
            br.printErr(TAG + "Cannot find section: " + sectionName + " (aborting plugin)");
            return;
        }

        // Scan stack traces
        Processes processes = scanProcesses(br, id, sec, chapterName);
        mProcesses.put(id, processes);

        // Also do some initial pre-processing, mainly to extract some useful info for other plugins
        for (Process process : processes) {
            // First make a list of all known threads
            Vector<PSRecord> chpsr = br.findChildPSRecords(process.getPid());
            // Suggest names and remove known children
            int cnt = process.getCount();
            for (int i = 0; i < cnt; i++) {
                StackTrace stack = process.get(i);
                String propSysTid = stack.getProperty("sysTid");
                if (propSysTid != null) {
                    try {
                        int sysTid = Integer.parseInt(propSysTid);
                        ProcessRecord pr = br.getProcessRecord(sysTid, true, false);
                        pr.suggestName(stack.getName(), 40);
                        // remove known child process records
                        PSRecord psr = br.getPSRecord(sysTid);
                        if (psr != null) {
                            chpsr.remove(psr);
                        }
                    } catch (NumberFormatException nfe) { }
                }
            }
            // Store unknown process records
            for (PSRecord psr : chpsr) {
                process.addUnknownThread(psr);
            }
        }

    }

    @Override
    public void generate(Report br) {
        if (mProcesses.size() == 0) return;

        for (Processes processes : mProcesses.values()) {
            generate(br, processes);
        }

        importIntoDB();
    }

    private void generate(Report rep, Processes processes) {
        BugReport br = (BugReport)rep;

        int id = processes.getId();
        String chapterName = processes.getName();

        // Analyze the stack trace
        analyze(br, id, processes);

        // Generate chapter
        genChapter(br, id, processes, chapterName);
    }

    private Processes scanProcesses(BugReport br, int id, Section sec, String chapterName) {
        int cnt = sec.getLineCount();
        int state = STATE_INIT;
        Processes processes = new Processes(br, id, chapterName, sec.getName());
        Process curProc = null;
        StackTrace curStackTrace = null;
        for (int i = 0; i < cnt; i++) {
            String buff = sec.getLine(i);
            switch (state) {
                case STATE_INIT:
                    if (buff.startsWith("----- pid ")) {
                        state = STATE_PROC;
                        String fields[] = buff.split(" ");
                        int pid = Integer.parseInt(fields[2]);
                        curProc = new Process(processes, pid, fields[4], fields[5]);
                        processes.add(curProc);
                    }
                    break;
                case STATE_PROC:
                    if (buff.startsWith("----- end ")) {
                        curProc = null;
                        state = STATE_INIT;
                    } else if (buff.startsWith("Cmd line: ")) {
                        curProc.setName(buff.substring(10));
                    } else if (buff.startsWith("\"")) {
                        state = STATE_STACK;
                        int idx = buff.indexOf('"', 1);
                        String name = buff.substring(1, idx);
                        String fields[] = buff.substring(idx + 2).split(" ");
                        String threadState = "?";
                        int prio = -1, tid = -1;
                        int fieldCount = fields.length;
                        for (int fi = 0; fi < fieldCount; fi++) {
                            String f = fields[fi];
                            idx = f.indexOf('=');
                            if (idx < 0) {
                                // Keyword
                                if (fi == fieldCount-1) {
                                    threadState = f;
                                }
                            } else {
                                // key=value
                                String key = f.substring(0, idx);
                                String value = f.substring(idx + 1);
                                if (key.equals("prio")) {
                                    prio = Integer.parseInt(value);
                                } else if (key.equals("tid")) {
                                    tid = Integer.parseInt(value);
                                }
                            }
                        }
                        curStackTrace = new StackTrace(curProc, name, tid, prio, threadState);
                        curProc.addStackTrace(curStackTrace);
                    }
                    break;
                case STATE_STACK:
                    if (!buff.startsWith("  ")) {
                        state = STATE_PROC;
                        curStackTrace = null;
                    } else if (buff.startsWith("  | ")) {
                        // Parse the extra properties
                        curStackTrace.parseProperties(buff.substring(4));
                    } else if (buff.startsWith("  - ")) {
                        buff = buff.substring(4);
                        if (buff.startsWith("waiting to lock")) {
                            String needle = "held by threadid=";
                            int idx = buff.indexOf(needle);
                            if (idx > 0) {
                                idx += needle.length();
                                int idx2 = buff.indexOf(' ', idx);
                                if (idx2 > 0) {
                                    int tid = Integer.parseInt(buff.substring(idx, idx2));
                                    curStackTrace.setWaitOn(tid);
                                }
                            }
                        }
                    } else if (buff.startsWith("  at ")) {
                        int idx0 = buff.indexOf('(');
                        int idx1 = buff.indexOf(':');
                        int idx2 = buff.indexOf(')');
                        if (idx0 >= 0 && idx2 >= 0 && idx2 > idx0) {
                            String method = buff.substring(5, idx0);
                            String fileName = null;
                            int line = -1;
                            if (idx1 >= 0 && idx1 > idx0 && idx2 > idx1) {
                                fileName = buff.substring(idx0 + 1, idx1);
                                String lineS = buff.substring(idx1 + 1, idx2);
                                if (lineS.startsWith("~")) {
                                    lineS = lineS.substring(1);
                                }
                                line = Integer.parseInt(lineS);
                            }
                            StackTraceItem item = new StackTraceItem(method, fileName, line);
                            curStackTrace.addStackTraceItem(item);
                        }
                    }
            }

        }
        return processes;
    }

    private void genChapter(BugReport br, int id, Processes processes, String chapterName) {
        Chapter main = processes.getChapter();
        main.addLine("<div class=\"hint\">(Generated from : \"" + processes.getSectionName() + "\")</div>");

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
            ch.addLine("<div class=\"hint\">(" + p.getDate() + " " + p.getTime() + ")</div>");

            // Add link from global process record
            ProcessRecord pr = br.getProcessRecord(p.getPid(), true, true);
            pr.suggestName(p.getName(), 50);
            pr.beginBlock();
            String link = br.createLinkTo(processes.getChapter(), anchor);
            pr.addLine("<a href=\"" + link + "\">");
            if (id == ID_NOW) {
                pr.addLine("Current stack trace &gt;&gt;&gt;");
            }
            if (id == ID_ANR) {
                pr.addLine("Stack trace at last ANR &gt;&gt;&gt;");
            }
            if (id == ID_OLD) {
                pr.addLine("Old stack traces &gt;&gt;&gt;");
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
                    waiting = " waiting on <a href=\"" + linkWait + "\">thread-" + waitOn + "</a>";
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

        br.addChapter(main);
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

    private void analyze(BugReport br, int id, Processes processes) {
        for (Process p : processes) {
            int cnt = p.getCount();
            for (int i = 0; i < cnt; i++) {
                StackTrace stack = p.get(i);
                // Apply a default colorization
                colorize(p, stack, br);
                // Check for main thread violations
                boolean isMainThread = stack.getName().equals("main");
                // I misunderstood the IntentService usage. I still keep parts of the code
                // for similar cases
                boolean isIntentServiceThread = false; // stack.getName().startsWith("IntentService[");
                if (isMainThread || isIntentServiceThread) {
                    checkMainThreadViolation(stack, p, br, isMainThread, true);
                    // Also check indirect violations: if the main thread is waiting on another thread
                    int waitOn = stack.getWaitOn();
                    if (waitOn >= 0) {
                        StackTrace other = p.findTid(waitOn);
                        checkMainThreadViolation(other, p, br, isMainThread, false);
                    }
                }
                // Check for deadlocks
                checkDeadLock(p, stack, br);
            }
        }
    }

    private void colorize(Process p, StackTrace stack, BugReport br) {
        if (stack == null) return;

        // Check android looper based threads
        int loopIdx = stack.findMethod("android.os.Looper.loop");
        if (loopIdx >= 0) {
            int waitIdx1 = stack.findMethod("android.os.MessageQueue.nativePollOnce");
            int waitIdx2 = stack.findMethod("android.os.MessageQueue.next");
            if (waitIdx1 < 0 && waitIdx2 < 0) {
                // This looper based thread seems to be doing something
                stack.setStyle(0, loopIdx, StackTraceItem.STYLE_BUSY);
                p.addBusyThreadStack(stack);
            }
        }

        // Check binder transactions
        int binderIdx = stack.findMethod("android.os.Binder.execTransact");
        if (binderIdx >= 0) {
            stack.setStyle(0, binderIdx, StackTraceItem.STYLE_BUSY);
            p.addBusyThreadStack(stack);
        }
        // Check NativeStart.run based threads
        int nativeStartRunIdx = stack.findMethod("dalvik.system.NativeStart.run");
        if (nativeStartRunIdx > 0) {
            // Thread is not currently in NativeStart.run, it seems to be doing something
            stack.setStyle(0, nativeStartRunIdx, StackTraceItem.STYLE_BUSY);
            p.addBusyThreadStack(stack);
        }
    }

    private void checkMainThreadViolation(StackTrace stack, Process p, BugReport br, boolean isMainThread, boolean isDirect) {
        if (stack == null) return;
        int itemCnt = stack.getCount();
        for (int j = itemCnt-1; j >= 0; j--) {
            StackTraceItem item = stack.get(j);
            if (isMainViolation(item.getMethod())) {
                // Report a bug
                StackTraceItem caller = stack.get(j+1);
                String anchorTrace = p.getAnchor(stack);
                String linkTrace = br.createLinkTo(p.getGroup().getChapter(), anchorTrace);
                String title = (isMainThread ? "Main" : "IntentService") + " thread violation: " + item.getMethod();
                if (!isDirect) {
                    title = "(Indirect) " + title;
                }
                ProcessRecord pr = br.getProcessRecord(p.getPid(), true, true);
                String startPrA = "";
                String endPrA = "";
                if (pr != null) {
                    startPrA = "<a href=\"" + br.createLinkToProcessRecord(p.getPid()) + "\">";
                    endPrA = "</a>";
                }
                Bug bug = new Bug(Bug.PRIO_MAIN_VIOLATION, 0, title);
                bug.addLine("<div class=\"bug\">");
                bug.addLine("<p>The process " + startPrA + p.getName() + "(pid " + p.getPid() + ")" + endPrA +
                        " is violating the " + (isMainThread ? "main" : "IntentService") + " thread");
                bug.addLine("by calling the method <tt>" + item.getMethod() + "</tt>");
                bug.addLine("from method <tt>" + caller.getMethod() + "(" + caller.getFileName() + ":" + caller.getLine() + ")</tt>!</p>");
                bug.addLine("<div><a href=\"" + linkTrace + "\">(full stack trace in chapter \"" +
                        stack.getProcess().getGroup().getName() + "\")</a></div>");
                if (!isDirect) {
                    String anchorWait = p.getAnchor(p.findTid(1));
                    String linkWait = br.createLinkTo(p.getGroup().getChapter(), anchorWait);
                    bug.addLine("<p>NOTE: This is an indirect violation: the thread is waiting on another thread which executes a blocking method!</p>");
                    bug.addLine("<div><a href=\"" + linkWait + "\">(full stack trace on waiting thread)</a></div>");
                }
                bug.addLine("</div>");
                br.addBug(bug);
                // Also colorize the stack trace
                stack.setStyle(j, j + 2, StackTraceItem.STYLE_ERR);
                break;
            }
        }
    }

    private boolean isMainViolation(String method) {
        if (method.startsWith("android.content.ContentResolver.")) return true;
        if (method.startsWith("org.apache.harmony.luni.internal.net.www.protocol.http.HttpsURLConnectionImpl.")) return true;
        if (method.startsWith("org.apache.harmony.luni.internal.net.www.protocol.https.HttpsURLConnectionImpl.")) return true;
        if (method.startsWith("android.database.sqlite.SQLiteDatabase.")) return true;
        return false;
    }

    private void checkDeadLock(Process p, StackTrace stack, BugReport br) {
        StackTrace orig = stack;
        int cnt = p.getCount();
        boolean used[] = new boolean[cnt];
        int idx = p.indexOf(stack.getTid());
        while (true) {
            used[idx] = true;
            int tid = stack.getWaitOn();
            if (tid < 0) return;
            idx = p.indexOf(tid);
            if (idx < 0) return;
            stack = p.get(idx);
            if (used[idx]) {
                // DEADLOCK DETECTED
                break;
            }
        }

        // If we got here, then a deadlock was detected, so create the bug

        // We found the dead lock (a loop in the dependency graph), but we need to clean it
        // (remove nodes which are not in the loop)
        int masterIdx = idx; // this is definitely part of the deadlock loop
        for (int i = 0; i < cnt; i++) {
            used[i] = false;
        }
        used[masterIdx] = true;
        while (true) {
            used[idx] = true;
            stack = p.get(idx);
            int tid = stack.getWaitOn();
            idx = p.indexOf(tid);
            if (idx < masterIdx) {
                masterIdx = idx; // we need a unique id per loop, so keep the smallest node index
            }
            if (used[idx]) {
                // Loop closed
                break;
            }
        }

        // Now make sure we print the deadlock only once
        if (p.get(masterIdx) != orig) {
            // This deadlock will be detected by someone else
            return;
        }

        // But: a deadlock will be detected many times, we must make sure we show it only once
        String a1 = "", a2 = "";
        int pid = p.getPid();
        ProcessRecord pr = br.getProcessRecord(pid, false, false);
        if (pr != null) {
            a1 = "<a href=\"" + br.createLinkToProcessRecord(pid) + "\">";
            a2 = "</a>";
        }
        Bug bug = new Bug(Bug.PRIO_DEADLOCK, 0, "Deadlock in process " + p.getName());
        bug.addLine("<div class=\"bug\">");
        bug.addLine("<p>The process " + a1 + p.getName() + "(pid " + pid + ")" + a2 + " has a deadlock involving the following threads (from \"" + p.getGroup().getName() + "\"):</p>");
        bug.addLine("<ul>");
        for (int i = 0; i < cnt; i++) {
            if (!used[i]) continue;
            stack = p.get(i);
            String anchorTrace = p.getAnchor(stack);
            String linkTrace = br.createLinkTo(p.getGroup().getChapter(), anchorTrace);
            bug.addLine("<li><a href=\"" + linkTrace + "\">" + stack.getName() + "</a></li>");
        }
        bug.addLine("</ul>");
        bug.addLine("</div>");
        br.addBug(bug);
    }

    private void importIntoDB() {
        if (mConn == null) return;
        try {
            importIntoDBUnsafe();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void importIntoDBUnsafe() throws SQLException {
        int nextProcessId = 0;
        int nextThreadId = 0;
        int nextItemId = 0;

        // Create the table structure
        Statement stat = mConn.createStatement();
        stat.execute("CREATE TABLE stacktrace_groups (id int, name varchar)");
        stat.execute("INSERT INTO stacktrace_groups VALUES (" + ID_NOW + ", \"VM traces just now\")");
        stat.execute("INSERT INTO stacktrace_groups VALUES (" + ID_ANR + ", \"VM traces at last ANR\")");
        stat.execute("INSERT INTO stacktrace_groups VALUES (" + ID_OLD + ", \"VM traces\")");
        stat.execute("CREATE TABLE stacktrace_processes (id int, pid int, name varchar, group_id int)");
        stat.execute("CREATE TABLE stacktrace_threads (id int, tid int, name varchar, process_id int)");
        stat.execute("CREATE TABLE stacktrace_items (id int, idx id, method varchar, file varchar, line int, thread_id int)");
        stat.close();
        PreparedStatement insProc = mConn.prepareStatement("INSERT INTO stacktrace_processes(id,pid,name,group_id) VALUES (?,?,?,?)");
        PreparedStatement insThread = mConn.prepareStatement("INSERT INTO stacktrace_threads(id,tid,name,process_id) VALUES (?,?,?,?)");
        PreparedStatement insItem = mConn.prepareStatement("INSERT INTO stacktrace_items(id,idx,method,file,line,thread_id) VALUES (?,?,?,?,?,?)");

        // Handle each process group
        for (Processes processes : mProcesses.values()) {
            for (Process process : processes) {
                int processId = ++nextProcessId;
                insProc.setInt(1, processId);
                insProc.setInt(2, process.getPid());
                insProc.setString(3, process.getName());
                insProc.setInt(4, processes.getId());
                insProc.addBatch();

                int threadCnt = process.getCount();
                for (int i = 0; i < threadCnt; i++) {
                    int threadId = ++nextThreadId;
                    StackTrace stack = process.get(i);
                    insThread.setInt(1, threadId);
                    insThread.setInt(2, stack.getTid());
                    insThread.setString(3, stack.getName());
                    insThread.setInt(4, processId);
                    insThread.addBatch();

                    int stackSize = stack.getCount();
                    for (int j = 0; j < stackSize; j++) {
                        int itemId = ++nextItemId;
                        StackTraceItem item = stack.get(j);
                        insItem.setInt(1, itemId);
                        insItem.setInt(2, j);
                        insItem.setString(3, item.getMethod());
                        insItem.setString(4, item.getFileName());
                        insItem.setInt(5, item.getLine());
                        insItem.setInt(6, threadId);
                        insItem.addBatch();
                    }
                }
            }
        }

        // Cleanup
        insItem.executeBatch();
        insItem.close();
        insThread.executeBatch();
        insThread.close();
        insProc.executeBatch();
        insProc.close();
        mConn.commit();
    }

    static class Processes extends Vector<Process> {

        private int mId;
        private String mName;
        private String mSectionName;
        private Vector<StackTrace> mBusy = new Vector<StackTrace>();
        private Chapter mCh;

        public Processes(Report report, int id, String name, String sectionName) {
            mId = id;
            mName = name;
            mSectionName = sectionName;
            mCh = new Chapter(report, name);
        }

        public int getId() {
            return mId;
        }

        public String getName() {
            return mName;
        }

        public String getSectionName() {
            return mSectionName;
        }

        public Chapter getChapter() {
            return mCh;
        }

        private static final long serialVersionUID = 1L;

        public void addBusyThreadStack(StackTrace stack) {
            if (!mBusy.contains(stack)) {
                mBusy.add(stack);
            }
        }

        public Vector<StackTrace> getBusyStackTraces() {
            return mBusy;
        }

    }

    static class Process {

        private int mPid;
        private String mName;
        private Vector<StackTrace> mStacks = new Vector<StackTrace>();
        private Vector<PSRecord> mUnknownThreads= new Vector<PSRecord>();
        private Processes mGroup;
        private String mDate;
        private String mTime;

        public Process(Processes processes, int pid, String date, String time) {
            mGroup = processes;
            mPid = pid;
            mDate = date;
            mTime = time;
        }

        public Processes getGroup() {
            return mGroup;
        }

        public String getDate() {
            return mDate;
        }

        public String getTime() {
            return mTime;
        }

        public void addBusyThreadStack(StackTrace stack) {
            mGroup.addBusyThreadStack(stack);
        }

        public String getAnchor() {
            return "stacktrace_" + mGroup.getId() + "_" + mPid;
        }

        public String getAnchor(StackTrace stack) {
            return "stacktrace_" + mGroup.getId() + "_" + mPid + "_" + stack.getTid();
        }

        public StackTrace findTid(int tid) {
            for (StackTrace stack : mStacks) {
                if (stack.getTid() == tid) {
                    return stack;
                }
            }
            return null;
        }

        public int indexOf(int tid) {
            for (int i = 0; i < mStacks.size(); i++) {
                if (mStacks.get(i).getTid() == tid) {
                    return i;
                }
            }
            return -1;
        }

        public int getPid() {
            return mPid;
        }

        public void setName(String name) {
            mName = name;
        }

        public String getName() {
            return mName;
        }

        public void addStackTrace(StackTrace stackTrace) {
            mStacks.add(stackTrace);
        }

        public int getCount() {
            return mStacks.size();
        }

        public StackTrace get(int idx) {
            return mStacks.get(idx);
        }

        public void addUnknownThread(PSRecord psr) {
            mUnknownThreads.add(psr);
        }

        public int getUnknownThreadCount() {
            return mUnknownThreads.size();
        }

        public PSRecord getUnknownThread(int idx) {
            return mUnknownThreads.get(idx);
        }

    }

    static class StackTrace {

        private String mName;
        private Vector<StackTraceItem> mStack = new Vector<StackTraceItem>();
        private int mTid;
        private int mPrio;
        private String mState;
        private int mWaitOn;
        private Process mProc;
        private HashMap<String, String> mProps = new HashMap<String, String>();

        public StackTrace(Process process, String name, int tid, int prio, String threadState) {
            mProc = process;
            mName = name;
            mTid = tid;
            mPrio = prio;
            mState = threadState;
            mWaitOn = -1;
        }

        public void parseProperties(String s) {
            String kvs[] = s.split(" ");
            for (String kv : kvs) {
                if (kv.length() == 0) continue;
                String pair[] = kv.split("=");
                if (pair.length != 2) continue;
                mProps.put(pair[0], pair[1]);
            }
        }

        public String getProperty(String key) {
            return mProps.get(key);
        }

        public void setStyle(int from, int to, String style) {
            from = Math.max(0, from);
            to = Math.min(getCount(), to);
            for (int i = from; i < to; i++) {
                get(i).setStyle(style);
            }
        }

        public int findMethod(String methodName) {
            int cnt = getCount();
            for (int i = 0; i < cnt; i++) {
                if (get(i).getMethod().equals(methodName)) {
                    return i;
                }
            }
            return -1;
        }

        public Process getProcess() {
            return mProc;
        }

        public String getName() {
            return mName;
        }

        public int getTid() {
            return mTid;
        }

        public int getPrio() {
            return mPrio;
        }

        public int getWaitOn() {
            return mWaitOn;
        }

        public void setWaitOn(int tid) {
            mWaitOn = tid;
        }

        public String getState() {
            return mState;
        }

        public void addStackTraceItem(StackTraceItem item) {
            mStack.add(item);
        }

        public int getCount() {
            return mStack.size();
        }

        public StackTraceItem get(int idx) {
            return mStack.get(idx);
        }

    }

    static class StackTraceItem {

        public static final String STYLE_ERR = "stacktrace-err";
        public static final String STYLE_BUSY = "stacktrace-busy";

        private String mMethod;
        private String mFileName;
        private int mLine;
        private String mStyle = "";

        public StackTraceItem(String method, String fileName, int line) {
            mMethod = method;
            mFileName = fileName;
            mLine = line;
        }

        public String getStyle() {
            return mStyle;
        }

        public void setStyle(String style) {
            mStyle = style;
        }

        public String getMethod() {
            return mMethod;
        }

        public String getFileName() {
            return mFileName;
        }

        public int getLine() {
            return mLine;
        }

    }
}
