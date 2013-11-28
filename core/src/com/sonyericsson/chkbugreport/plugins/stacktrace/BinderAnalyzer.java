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
package com.sonyericsson.chkbugreport.plugins.stacktrace;

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.Section;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class parses the binder state to detect inter-process dependencies.
 * The binder state is the dump of /sys/kernel/debug/binder/state and we are looking
 * for lines like:
 *
 *   incoming transaction 18397911: d1d52bc0 from 16421:16699 to 16435:14261 code 3 flags 10 pri 0 r1 node 18397661 size 80:0 data f1e00104
 */
/* package */ final class BinderAnalyzer {

    public BinderAnalyzer(StackTracePlugin stackTracePlugin) {
    }

    public void analyze(BugReportModule br, Processes proc, Section sec) {
        int cnt = sec.getLineCount();
        Pattern pattern = Pattern.compile("outgoing transaction [0-9]+: [0-9a-f]+ from ([0-9]+):([0-9]+) to ([0-9]+):([0-9]+)");
        for (int idx = 0; idx < cnt; idx++) {
            String line = sec.getLine(idx);
            Matcher m = pattern.matcher(line);
            if (!m.find()) continue;
            try {
                int srcPid = Integer.parseInt(m.group(1));
                int srcTid = Integer.parseInt(m.group(2)); // Note: this is actually the linux PID of the thread
                int dstPid = Integer.parseInt(m.group(3));
                int dstTid = Integer.parseInt(m.group(4)); // Note: this is actually the linux PID of the thread

                // We found an AIDL connection, now let's add the dependency
                Process srcProc = proc.findPid(srcPid);
                if (srcProc == null) {
                    br.printErr(4, StackTracePlugin.TAG + "Cannot find process with pid " + srcPid + "!");
                    continue;
                }
                StackTrace srcThread = srcProc.findPid(srcTid);
                if (srcThread == null) {
                    br.printErr(4, StackTracePlugin.TAG + "Cannot find thread with pid " + srcTid + "!");
                    continue;
                }
                Process dstProc = proc.findPid(dstPid);
                if (dstProc == null) {
                    br.printErr(4, StackTracePlugin.TAG + "Cannot find process with pid " + dstPid + "!");
                    continue;
                }
                StackTrace dstThread = dstProc.findPid(dstTid);
                if (dstThread == null) {
                    br.printErr(4, StackTracePlugin.TAG + "Cannot find thread with pid " + dstTid + "!");
                    continue;
                }

                // Everything seems to be in place, so let's mark the dependency
                srcThread.setAidlDependency(dstThread);

            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

}
