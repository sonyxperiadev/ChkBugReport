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
package com.sonyericsson.chkbugreport.plugins;

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.doc.Bug;
import com.sonyericsson.chkbugreport.doc.MemRenderer;
import com.sonyericsson.chkbugreport.plugins.logs.LogLines;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Vector;

public class SummaryPlugin extends Plugin {

    public static final int INTERESTING_BUGS[] = {
        Bug.PRIO_ANR_EVENT_LOG,
        Bug.PRIO_ANR_SYSTEM_LOG,
        Bug.PRIO_JAVA_CRASH_EVENT_LOG,
        Bug.PRIO_JAVA_CRASH_SYSTEM_LOG,
        Bug.PRIO_NATIVE_CRASH,
    };

    private static final int RELATED_BUG_RANGE = 10;

    @Override
    public int getPrio() {
        return 100+1; // Execute last, to make sure all info is available
    }

    @Override
    public void reset() {
        // NOP
    }

    @Override
    public void load(Module br) {
        // NOP: do all the work in generate, when all other plugins have finished
    }

    @Override
    public void generate(Module br) {
        // NOP
    }

    private Bug findLastInterestingBug(Module br) {
        long retTs = -1;
        Bug ret = null;
        for (int i = 0; i < br.getBugCount(); i++) {
            Bug bug = br.getBug(i);
            if (isInterestingBug(bug)) {
                if (ret == null || retTs < bug.getTimeStamp()) {
                    ret = bug;
                    retTs = bug.getTimeStamp();
                }
            }
        }
        return ret;
    }

    private boolean isInterestingBug(Bug bug) {
        int prio = bug.getPrio();
        for (int p : INTERESTING_BUGS) {
            if (prio == p) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void finish(Module mod) {
        BugReportModule br = (BugReportModule) mod;

        // Find the last interesting bug
        Bug bug = findLastInterestingBug(br);
        if (bug == null) {
            return;
        }

        // Find related bugs
        Vector<Bug> bugs = new Vector<Bug>();
        bugs.add(bug);
        for (int i = 0; i < br.getBugCount(); i++) {
            Bug bug2 = br.getBug(i);
            if (isRelated(bug2, bug)) {
                bugs.add(bug2);
            }
        }

        // Sort the bugs again
        Collections.sort(bugs, Bug.getComparator());

        // If we found at least one bug, then we can create the summary page
        String fn = br.getRelRawDir() + "summary.txt";
        try {
            PrintStream out = new PrintStream(new File(br.getBaseDir() + fn));

            // Copy the first three lines of the header (the dumpstate date)
            for (String line : br.getBugReportHeader()) {
                out.println(line);
            }

            // Print some device info
            SysPropsPlugin sysProps = (SysPropsPlugin) br.getPlugin("SysPropsPlugin");
            out.println("HW:         " + sysProps.getProp("ro.build.product"));
            out.println("SW:         " + sysProps.getProp("ro.build.id"));
            out.println("Variant:    " + sysProps.getProp("ro.semc.version.sw_variant") + "/" + sysProps.getProp("ro.semc.version.fs"));
            out.println("Build type: " + sysProps.getProp("ro.build.type"));
            out.println("IMEI:       " + sysProps.getProp("persist.radio.imei"));
            out.println();

            // Print the detected error type
            out.println("================================");
            switch (bug.getPrio()) {
            case Bug.PRIO_ANR_EVENT_LOG:
            case Bug.PRIO_ANR_SYSTEM_LOG:
            case Bug.PRIO_ANR_MONKEY:
                out.println("ANR");
                break;
            case Bug.PRIO_JAVA_CRASH_EVENT_LOG:
            case Bug.PRIO_JAVA_CRASH_SYSTEM_LOG:
                out.println("Java Crash");
                break;
            case Bug.PRIO_NATIVE_CRASH:
                out.println("Native Crash");
                break;
            default:
                out.println(bug.getName());
                break;
            }
            out.println("================================");
            out.println();

            // Dump the bugs
            for (Bug b : bugs) {
                dumpBug(out, b, br);
            }

            // Done
            out.close();
            br.addHeaderLine("Summary saved to: <a href=\"" + fn + "\">" + fn + "</a>");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isRelated(Bug bug2, Bug bug) {
        int mainPrio = bug.getPrio();
        int prio = bug2.getPrio();

        // For ANR main bug we are interested either in it's pair, or some clue which could have caused it
        if (isAnr(mainPrio)) {
            if (isAnr(prio)) {
                return isMatching(bug, bug2);
            } else if (prio == Bug.PRIO_DEADLOCK || prio == Bug.PRIO_MAIN_VIOLATION) {
                return true;
            }
            return false;
        }

        // If we get a fatal exception, we might be iterested in the hprof dump
        if (isJavaCrash(mainPrio)) {
            if (isJavaCrash(prio)) {
                return isMatching(bug, bug2);
            } else if (prio == Bug.PRIO_HPROF) {
                return true;
            }
            return false;
        }

        return false;
    }

    private boolean isMatching(Bug bug1, Bug bug2) {
        if (bug1.getPrio() != bug2.getPrio()) {
            if (Math.abs(bug1.getTimeStamp() - bug2.getTimeStamp()) < RELATED_BUG_RANGE*1000) {
                return true;
            }
        }
        return false;
    }

    private boolean isAnr(int prio) {
        return prio == Bug.PRIO_ANR_EVENT_LOG || prio == Bug.PRIO_ANR_SYSTEM_LOG;
    }

    private boolean isJavaCrash(int prio) {
        return prio == Bug.PRIO_JAVA_CRASH_EVENT_LOG || prio == Bug.PRIO_JAVA_CRASH_SYSTEM_LOG;
    }

    private void dumpBug(PrintStream out, Bug b, Module br) {
        int prio = b.getPrio();
        switch (prio) {
        case Bug.PRIO_ANR_EVENT_LOG:
        case Bug.PRIO_JAVA_CRASH_EVENT_LOG:
            dumpBugAnrOrJCrashEventLog(out, b, br);
            break;
        case Bug.PRIO_ANR_SYSTEM_LOG:
            dumpBugAnrSystemLog(out, b, br);
            break;
        case Bug.PRIO_JAVA_CRASH_SYSTEM_LOG:
            dumpBugJCrashSystemLog(out, b, br);
            break;
        case Bug.PRIO_NATIVE_CRASH:
            dumpBugNativeCrash(out, b, br);
            break;
        case Bug.PRIO_HPROF:
            dumpBugHprof(out, b);
            break;
        default:
            dumpBugGeneric(out, b);
            break;
        }
    }

    private void dumpBugHprof(PrintStream out, Bug b) {
        out.println("--------------------------------");
        out.println(b.getName());
        out.println("--------------------------------");
        out.println();
    }

    private void dumpBugAnrOrJCrashEventLog(PrintStream out, Bug b, Module br) {
        out.println("Package: " + b.getAttr(Bug.ATTR_PACKAGE));
        out.println("Pid:     " + b.getAttr(Bug.ATTR_PID));
        out.println("Reason:  " + b.getAttr(Bug.ATTR_REASON));
        out.println();
    }

    private void dumpBugAnrSystemLog(PrintStream out, Bug b, Module br) {
        dumpBugLog(out, b, br);
    }

    private void dumpBugJCrashSystemLog(PrintStream out, Bug b, Module br) {
        dumpBugLog(out, b, br);
    }

    private void dumpBugNativeCrash(PrintStream out, Bug b, Module br) {
        dumpBugLog(out, b, br);
    }

    private void dumpBugLog(PrintStream out, Bug b, Module br) {
        int firstLine = (Integer) b.getAttr(Bug.ATTR_FIRST_LINE);
        int lastLine = (Integer) b.getAttr(Bug.ATTR_LAST_LINE);
        String infoId = (String) b.getAttr(Bug.ATTR_LOG_INFO_ID);
        LogLines log = (LogLines) br.getInfo(infoId);

        if (log != null) {
            out.println(infoId + ":");
            for (int i = firstLine; i < lastLine; i++) {
                out.println(log.get(i).line);
            }
            out.println();
        }
    }

    private void dumpBugGeneric(PrintStream out, Bug b) {
        try {
            // For now just play dumb and print the lines, but stripping the html markers
            out.println("--------------------------------");
            out.println(b.getName());
            out.println("--------------------------------");
            MemRenderer r = new MemRenderer();
            r.begin();
            b.render(r);
            r.end();
            byte[] data = r.getData();
            boolean html = false;
            int l = data.length;
            for (int j = 0; j < l; j++) {
                char c = (char) data[j];
                if (html) {
                    if (c == '>') {
                        html = false;
                    }
                } else {
                    if (c == '<') {
                        html = true;
                    } else {
                        out.print(c);
                    }
                }
            }
            out.println();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
