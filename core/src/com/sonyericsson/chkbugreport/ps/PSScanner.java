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
package com.sonyericsson.chkbugreport.ps;

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.ProcessRecord;
import com.sonyericsson.chkbugreport.Section;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//Note: This code appears to be written from a time when all of this data was collected on a per process basis, and
//there was no threads in the dump.

//We're collecting several things that are process specific:
//PID, PPID, Policy, and Name (Extracted from Process times)

public class PSScanner {

    private BugReportModule mBr;
    private HashMap<Integer, String> mPIDNameLut = new HashMap<Integer, String>();

    private static final Pattern PS_HEADER_PATTERN = Pattern.compile("LABEL\\s+USER\\s+PID\\s+TID\\s+PPID\\s+VSZ\\s+RSS\\s+WCHAN\\s+ADDR\\s+S\\s+PRI\\s+NI\\s+RTPRIO\\s+SCH\\s+PCY.*");

    public PSScanner(BugReportModule br) {
        mBr = br;
    }

    public PSRecords run() {
        readPST();
        return readPS();
    }

    private void readPST() {
        final Pattern PST_PARSING_PATTERN = Pattern.compile("(\\d+)\\s+(\\S+)\\s+.*"); //Only parse up to the name

        Section psT = mBr.findSection(Section.PROCESSES_TIMES);
        if (psT == null) {
            mBr.printErr(3, "Cannot find section: " + Section.PROCESSES_TIMES + " (skipping process name lookup)");
            return;
        }
        for (int i = 0; i < psT.getLineCount(); i++) {
            String buff = psT.getLine(i);
            Matcher lineMatcher = PST_PARSING_PATTERN.matcher(buff);
            if(lineMatcher.matches()) {
                int pid = -1;
                String sPid = lineMatcher.group(1);
                String name = lineMatcher.group(2);
                try {
                    pid = Integer.parseInt(sPid);
                    mPIDNameLut.put(pid, name);
                } catch (NumberFormatException e) {
                    mBr.printErr(3, "Error parsing pid from : " + sPid + " (ignoring it)");
                }
                continue;
            }
            mBr.printErr(3, "Could not parse line: " + buff + " (ignoring it)");
        }
    }

    private PSRecords readPS() {
        Section ps = mBr.findSection(Section.PROCESSES_AND_THREADS);
        if (ps == null) {
            mBr.printErr(3, "Cannot find section: " + Section.PROCESSES_AND_THREADS + " (ignoring it)");
            return null;
        }

        // Process the PS section
        PSRecords ret = new PSRecords();
        boolean foundHeader = false;
        Pattern p = null;
        int lineIdx = 0, idxPid = -1, idxPPid = -1, idxPcy = -1, idxNice = -1;
        for (int tries = 0; tries < 10 && lineIdx < ps.getLineCount(); tries++) {
            String buff = ps.getLine(lineIdx++);
            Matcher headerMatcher = PS_HEADER_PATTERN.matcher(buff);
            if (headerMatcher.matches()) {
                foundHeader = true;
                idxPid = 2;
                idxPPid = 4;
                idxPcy = 14;
                idxNice = 11;
                break;
            }
        }
        if (!foundHeader) {
            mBr.printErr(4, "Could not find header in ps output, perhaps output has changed?");
            return null;
        }

        // Now read and process every line
        int pidZygote = -1;
        int cnt = ps.getLineCount();
        for (int i = lineIdx; i < cnt; i++) {
            String buff = ps.getLine(i);
            if (buff.startsWith("[")) break;
            String matches[] = buff.split("\\s+");
            if (matches.length <= 14) { //As long as we parse up to Policy we don't need more matches.
                mBr.printErr(4, "Error parsing line: " + buff);
                continue;
            }

            int pid = -1;
            if (idxPid >= 0) {
                String sPid = matches[idxPid];
                try {
                    pid = Integer.parseInt(sPid);
                } catch (NumberFormatException nfe) {
                    mBr.printErr(4, "Error parsing pid from: " + sPid);
                    break;
                }
            }

            // Extract ppid
            int ppid = -1;
            if (idxPPid >= 0) {
                String sPid = matches[idxPPid];
                try {
                    ppid = Integer.parseInt(sPid);
                } catch (NumberFormatException nfe) {
                    mBr.printErr(4, "Error parsing ppid from: " + sPid);
                    break;
                }
            }

            // Extract nice
            // Fixme: This can actually be different for different threads in the same process
            // (Linux threads violate POSIX.1 https://linux.die.net/man/7/pthreads
            // When this is modified to build a thread tree, we should fix this.  For now we just
            // Run over the last process entry so the last thread listed wins.
            int nice = PSRecord.NICE_UNKNOWN;
            if (idxNice >= 0) {
                String sNice = matches[idxNice];
                try {
                    nice = Integer.parseInt(sNice);
                } catch (NumberFormatException nfe) {
                    mBr.printErr(4, "Error parsing nice from: " + sNice);
                    break;
                }
            }

            // Extract scheduler policy
            int pcy = PSRecord.PCY_UNKNOWN;
            if (idxPcy >= 0) {
                String sPcy = matches[idxPcy];
                if ("fg".equals(sPcy)) {
                    pcy = PSRecord.PCY_NORMAL;
                } else if ("bg".equals(sPcy)) {
                    pcy = PSRecord.PCY_BATCH;
                } else if ("un".equals(sPcy)) {
                    pcy = PSRecord.PCY_FIFO;
                } else {
                    pcy = PSRecord.PCY_OTHER;
                }
            }

            // Extract name
            String name = mPIDNameLut.get(pid);
            if(name == null) {
                name = "unknown";
            }

            // Fix the name
            ret.put(pid, new PSRecord(pid, ppid, nice, pcy, name));

            // Check if we should create a ProcessRecord for this
            if (pidZygote == -1 && name.equals("zygote")) {
                pidZygote = pid;
            }
            ProcessRecord pr = mBr.getProcessRecord(pid, true, false);
            if(pr != null) {
                pr.suggestName(name, 10);
            }
        }

        // Build tree structure as well
        for (PSRecord psr : ret) {
            int ppid = psr.mPPid;
            PSRecord parent = ret.getPSRecord(ppid);
            if (parent == null) {
                parent = ret.getPSTree();
            }
            parent.mChildren.add(psr);
            psr.mParent = parent;
        }

        return ret;
    }

}
