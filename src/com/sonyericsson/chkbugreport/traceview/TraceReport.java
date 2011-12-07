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
package com.sonyericsson.chkbugreport.traceview;

import com.sonyericsson.chkbugreport.Chapter;
import com.sonyericsson.chkbugreport.Report;
import com.sonyericsson.chkbugreport.Util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

public class TraceReport extends Report {

    public static final int STATE_SLEEP = 0;
    public static final int STATE_WAIT = 1;
    public static final int STATE_RUN = 2;

    public static final int METHOD_ENTRY = 0;
    public static final int METHOD_EXIT = 1;
    public static final int METHOD_EXIT_W_EXC = 2;

    // Threads
    public static class ThreadInfo {
        public int id;
        public String name;
        public int timeOffs;
        public int timePause;
        public int timeLast;
        public Vector<Integer> stack = new Vector<Integer>();
        public Vector<MethodRun> calls = new Vector<MethodRun>();
        public MethodRun currentCall;
        public int lastLocatTime;

        public String getFullName() {
            return "Thread-" + id + " (" + name + ")";
        }
    }

    private Vector<ThreadInfo> mThreads = new Vector<ThreadInfo>();
    private HashMap<Integer, ThreadInfo> mThreadHash = new HashMap<Integer, ThreadInfo>();

    // Methods
    public static class MethodInfo {
        public int id;
        public String name;
        public String shortName;
        public char state;
        public Vector<MethodRun> calls = new Vector<TraceReport.MethodRun>();
        public int nrCalls;
        public int nrRecCalls;
        public int nesting;
        public int dur;
        public int durL;
        public int durExc;
        public int durExcL;
        public int maxDurL;
    }

    private Vector<MethodInfo> mMethods = new Vector<MethodInfo>();
    private HashMap<Integer, MethodInfo> mMethodHash = new HashMap<Integer, MethodInfo>();

    // Records
    public static class TraceRecord {
        int tid;
        int mid;
        int time;
        int localTime;
    }

    private Vector<TraceRecord> mRecords = new Vector<TraceRecord>();

    // Method run records
    public static class MethodRun {
        public int mid;
        public int tid;
        public MethodRun caller;
        public Vector<MethodRun> calls = new Vector<MethodRun>();
        public int startTime, startLocalTime;
        public int endTime, endLocalTime;
        public int nrCalls;
        public String name;
        public String shortName;
    }

    private long mAbsStartTime = 0;
    private boolean mAbsTime = false;
    private int mLastPrintTime = 0;

    {
        addPlugin(new StatsPlugin());
        addPlugin(new TreeViewPlugin());
        addPlugin(new TreePNGPlugin());
        addPlugin(new MainLooplugin());
    }

    public TraceReport(String fileName) {
        super(fileName);
    }

    public Vector<MethodInfo> getMethodInfos() {
        return mMethods;
    }

    public Vector<ThreadInfo> getThreadInfos() {
        return mThreads;
    }

    public Vector<TraceRecord> getTraceRecords() {
        return mRecords;
    }

    @Override
    public void load(InputStream is) throws IOException {
        String buff;

        // Skip to the threads
        boolean found_threads = false;
        while (null != (buff = Util.readLine(is))) {
            if ("*threads".equals(buff)) {
                found_threads = true;
                break;
            }
        }
        if (!found_threads) {
            System.err.println("Error parsing input file (threads section not found)!");
            return;
        }

        // Read threads into table until methods found
        boolean found_methods = false;
        while (null != (buff = Util.readLine(is))) {
            if ("*methods".equals(buff)) {
                found_methods = true;
                break;
            }
            String fields[] = buff.split("\t");
            ThreadInfo t = new ThreadInfo();
            t.id = Integer.parseInt(fields[0]);
            t.name = fixName(fields[1]);
            mThreads.add(t);
            mThreadHash.put(t.id, t);
        }
        System.out.println(String.format("Read %d threads...", mThreads.size()));
        if (!found_methods) {
            System.err.println("Error parsing input file (methods section not found)!");
            return;
        }

        // Read methods into table until end found
        boolean found_end = false;
        while (null != (buff = Util.readLine(is))) {
            if ("*end".equals(buff)) {
                found_end = true;
                break;
            }
            String fields[] = buff.split("\t");
            MethodInfo m = new MethodInfo();
            m.id = Integer.parseInt(fields[0].substring(2), 16);
            m.name = fixName(fields[1]) + "." + fixName(fields[2]) + fixName(fields[3]);
            m.shortName = fixName(fields[1]) + "." + fixName(fields[2]);
            mMethods.add(m);
            mMethodHash.put(m.id, m);
        }
        System.out.println(String.format("Read %d methods...", mMethods.size()));
        if (!found_end) {
            System.err.println("Error parsing input file (end not found)!");
            return;
        }

        // Sort Data
        System.out.println("Sorting threads...");
        Collections.sort(mThreads, new Comparator<ThreadInfo>() {
            @Override
            public int compare(ThreadInfo rec1, ThreadInfo rec2) {
                if (rec1.id < rec2.id) {
                    return -1;
                }
                if (rec1.id > rec2.id) {
                    return 1;
                }
                return 0;
            }
        });
        System.out.println("Sorting methods...");
        Collections.sort(mMethods, new Comparator<MethodInfo>() {
            @Override
            public int compare(MethodInfo rec1, MethodInfo rec2) {
                if (rec1.id < rec2.id) {
                    return -1;
                }
                if (rec1.id > rec2.id) {
                    return 1;
                }
                return 0;
            }
        });

        // Parse the tracing data header
        byte sig[] = new byte[4];
        is.read(sig);
        if (!"SLOW".equals(new String(sig, 0, 4))) {
            System.err.println("Error parsing input file (signature mismatch)!\n");
            return;
        }
        is.skip(2); // skip version
        int delta = Util.read2LE(is); // read header size/offs to data
        mAbsStartTime = Util.read8LE(is); // read absolute start time
        is.skip(delta - 16); // skip rest of the header

        // Parse the tracing data
        try {
            while (true) {
                TraceRecord t = new TraceRecord();
                t.tid = is.read();
                t.mid = Util.read4BE(is);
                t.time = t.localTime = Util.read4BE(is);
                mRecords.add(t);
            }
        } catch (IOException e) {
            // ignore, assume it's end of file
        }
        System.out.println(String.format("Read %d records...", mRecords.size()));

        // Fix timestamps
        ThreadInfo lastThread = null;
        System.out.println("Fixing timestamps...");
        int global_time = 0;
        for (TraceRecord r : mRecords) {
            int time = r.time;
            int tid = r.tid;

            // Check if thread has changed
            ThreadInfo thread = findThread(tid);
            if (thread != lastThread) {
                if (lastThread != null) {
                    // Save the paused time
                    lastThread.timePause = global_time;
                    lastThread.timeOffs = global_time - lastThread.timeLast;
                }
                //          printf("Thread switch: %3d -> %3d @ %d\n", last_thread_idx, thread_idx, time);
                lastThread = thread;
                lastThread.timeOffs += global_time - lastThread.timePause;
            }

            global_time = time + thread.timeOffs;
            thread.timeLast = time;
            r.time = global_time;
        }

        // Collect MethodRun information
        System.out.println("Collecting method run info...");
        int lastTime = 0;
        for (TraceRecord r : mRecords) {
            lastTime = r.time;
            int tid = r.tid;
            int mid = r.mid;
            int act = mid & 3;
            mid &= 0xfffffffc;

            ThreadInfo thread = findThread(tid);
            thread.lastLocatTime = r.localTime;

            if (act == METHOD_ENTRY) {
                MethodRun run = new MethodRun();
                run.startTime = r.time;
                run.startLocalTime = r.localTime;
                run.tid = tid;
                run.mid = mid;
                addRun(mid, run);
                if (thread.currentCall == null) {
                    // first call on the thread
                    thread.calls.add(run);
                } else {
                    thread.currentCall.calls.add(run);
                }
                run.caller = thread.currentCall;
                thread.currentCall = run;
            } else if (act == METHOD_EXIT || act == METHOD_EXIT_W_EXC) {
                if (thread.currentCall == null) {
                    // we don't have information about this method
                    // so create a new record, and assume 0 start time
                    MethodRun run = new MethodRun();
                    run.startTime = 0;
                    run.startLocalTime = 0;
                    run.tid = tid;
                    run.mid = mid;
                    addRun(mid, run);
                    thread.currentCall = run;

                    // Add all previous calls to this one
                    for (MethodRun prev : thread.calls) {
                        run.calls.add(prev);
                    }

                    // also we must reset the calls list
                    thread.calls.clear();
                    thread.calls.add(run);
                }

                thread.currentCall.endTime = r.time;
                thread.currentCall.endLocalTime = r.localTime;
                thread.currentCall = thread.currentCall.caller;
            }
        }

        // Now, we might have some calls which are not finished
        // we need to set the end time for those
        for (ThreadInfo t : mThreads) {
            MethodRun run = t.currentCall;
            while (run != null) {
                run.endTime = lastTime;
                run.endLocalTime = t.lastLocatTime;
                run = run.caller;
            }
        }

        // Collect MethodRun statistics
        System.out.println("Collecting method run statistics...");
        for (ThreadInfo t : mThreads) {
            for (MethodRun run : t.calls) {
                collectMethodStats(run);
            }
        }

    }

    private void collectMethodStats(MethodRun run) {
        MethodInfo m = findMethod(run.mid);
        int dur = run.endTime - run.startTime;
        int durL = run.endLocalTime - run.startLocalTime;
        int durC = 0;
        int durLC = 0;
        run.nrCalls = 1;
        run.name = m.name;
        run.shortName = m.shortName;

        m.nesting++;
        for (MethodRun ch : run.calls) {
            durC += ch.endTime - ch.startTime;
            durLC += ch.endLocalTime - ch.startLocalTime;
            collectMethodStats(ch);
            run.nrCalls += ch.nrCalls;
        }
        m.nesting--;

        if (m.nesting == 0) {
            m.nrCalls++;
            m.dur += dur;
            m.durL += durL;
            m.maxDurL = Math.max(m.maxDurL, durL);
            m.durExc += dur - durC;
            m.durExcL += durL - durLC;
        } else {
            m.nrRecCalls++;
        }
    }

    private void addRun(int mid, MethodRun run) {
        MethodInfo m = findMethod(mid);
        m.calls.add(run);
    }

    public ThreadInfo findThread(int tid) {
        return mThreadHash.get(tid);
    }

    public MethodInfo findMethod(int mid) {
        return mMethodHash.get(mid);
    }

    private void saveTraceVCD(int filterTid, String fn) throws IOException {
        System.out.println("Writing " + fn + "...");

        FileOutputStream fos = new FileOutputStream(getBaseDir() + fn);
        PrintStream ps = new PrintStream(fos);
        mLastPrintTime = 0;

        // Write the VCD header
        ps.println("$timescale 1us $end");
        ps.println("$scope traceview $end");
        if (filterTid == -1) {
            for (ThreadInfo t : mThreads) {
                ps.println(String.format("$var wire 1 t%d T-%s $end", t.id, t.name));
            }
            for (MethodInfo m : mMethods) {
                ps.println(String.format("$var wire 1 m%08x M-%s $end", m.id, m.name));
            }
        } else {
            for (MethodInfo m : mMethods) {
                ps.println(String.format("$var wire 1 m%08x %s $end", m.id, m.name));
            }
        }
        ps.println("$upscope $end");
        ps.println("$enddefinitions $end");
        ps.println("#" + (mAbsTime ? mAbsStartTime : 0));

        // TODO: guess initial value instead of assuming 0

        if (filterTid == -1) {
            for (ThreadInfo t : mThreads) {
                ps.println(String.format("b0 t%d", t.id));
            }
        }
        for (MethodInfo m : mMethods) {
            ps.println(String.format("b0 m%08x", m.id));
        }

        // Reset thread stack traces
        for (ThreadInfo t : mThreads) {
            t.stack.clear();
        }

        // Generate the output
        ThreadInfo lastThread = null;
        ThreadInfo thread = null;
        if (filterTid != -1) {
            thread = findThread(filterTid);
        }
        for (TraceRecord r : mRecords) {
            int time = (filterTid != -1) ? r.localTime : r.time;
            int tid = r.tid;
            int mid = r.mid;
            int act = mid & 3;
            mid &= 0xfffffffc;

            if (filterTid != -1 && tid != filterTid) {
                continue; // skip this, not interested in this thread
            }

            if (filterTid == -1) {
                thread = findThread(tid);
                // Check if thread has changed
                if (thread != lastThread) {
                    if (lastThread != null) {
                        printSignal(ps, time, String.format("b0 t%d", lastThread.id));
                        // Pause the method on top of the stack
                        int prevMid = peekStack(lastThread);
                        if (prevMid != -1) {
                            printSignal(ps, time, String.format("bZ m%08x", prevMid));
                        }
                    }
                    lastThread = thread;
                    printSignal(ps, time, String.format("bX t%d", lastThread.id));
                    // Resume method on top of stack
                    int nextMid = peekStack(lastThread);
                    if (nextMid != -1) {
                        printSignal(ps, time, String.format("bX m%08x", nextMid));
                    }
                }
            }

            // Handle methods
            if (act == METHOD_ENTRY) {
                // Pause last method on stack
                int prevMid = peekStack(thread);
                if (prevMid != -1) {
                    printSignal(ps, time, String.format("bZ m%08x", prevMid));
                }
                // Save this method on stack
                pushStack(thread, mid);
                // Method enter
                printSignal(ps, time, String.format("bX m%08x", mid));
            } else if (act == METHOD_EXIT || act == METHOD_EXIT_W_EXC) {
                // Method exit or exception
                printSignal(ps, time, String.format("b0 m%08x", mid));
                // Pop it from stack
                popStack(thread);
                // Resume method on top of stack
                int nextMid = peekStack(thread);
                if (nextMid != -1) {
                    printSignal(ps, time, String.format("bX m%08x", nextMid));
                }
            }
        }

        ps.close();
        fos.close();
    }

    @Override
    public void generate() throws IOException {
        super.generate();

        // Save the raw files
        generateVCD();

        // Run all the plugins
        runPlugins();

        // Collect detected bugs
        System.out.println("Collecting errors...");
        collectBugs();

        // Write header
        System.out.println("Writing header...");
        writeHeader();

        // Write the table of contents
        System.out.println("Writing TOC...");
        writeTOC();

        // Write all the chapters
        System.out.println("Writing Chapters...");
        writeChapters();

        // Close the file
        System.out.println("Writing footer...");
        writeFooter();
        closeFile();

        // Copy over some builtin resources
        System.out.println("Copying extra resources...");
        copyRes(Util.COMMON_RES);

        System.out.println("DONE!");
    }

    private void generateVCD() throws IOException {
        Chapter ch = new Chapter(this, "VCD files");
        addChapter(ch);
        ch.addLine("<p>Here are the generated VCD files, you can open them with GTKWave</p>");
        ch.addLine("<ul>");

        // Save the complete one
        String fn = getRelRawDir() + "thread_all.vcd";
        saveTraceVCD(-1, fn);
        ch.addLine("<li>All threads: <a href=\"" + fn + "\">" + fn + "</a></li>");

        // Save individual threads
        for (ThreadInfo t : mThreads) {
            int tid = t.id;
            fn = getRelRawDir() + "thread_" + tid + ".vcd";
            saveTraceVCD(tid, fn);
            ch.addLine("<li>" + t.getFullName() + ": <a href=\"" + fn + "\">" + fn + "</a></li>");
        }

        ch.addLine("</ul>");
    }

    @Override
    protected void writeHeader() {
        super.writeHeader();
        writeHeaderChapter();
    }

    private String fixName(String name)
    {
        char chars[] = name.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == ' ' || c == ':' || c == '@' || c == '.') {
                chars[i] = '_';
            }
        }
        return new String(chars);
    }

    private void printSignal(PrintStream fo, int time, String text)
    {
        if (time != mLastPrintTime) {
            mLastPrintTime = time;
            if (mAbsTime) {
                fo.println("#" + (time + mAbsStartTime));
            } else {
                fo.println("#" + time);
            }
        }
        fo.println(text);
    }

    private int peekStack(ThreadInfo t)
    {
        int cnt = t.stack.size();
        if (cnt == 0) {
            return -1;
        }
        return t.stack.get(cnt - 1);
    }

    private void pushStack(ThreadInfo t, int mid)
    {
        t.stack.add(mid);
    }

    private int popStack(ThreadInfo t)
    {
        int cnt = t.stack.size();
        if (cnt == 0) {
            return -1;
        }
        int ret = t.stack.get(cnt - 1);
        t.stack.remove(cnt - 1);
        return ret;
    }

}
