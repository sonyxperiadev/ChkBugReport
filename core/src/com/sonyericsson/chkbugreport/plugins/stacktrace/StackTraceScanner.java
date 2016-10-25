/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
 * Copyright (C) 2012 Sony Mobile Communications AB
 * Copyright (C) 2016 Tuenti Technologies
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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is responsible to scan the stack trace output and collect the data
 */
/* package */ final class StackTraceScanner {

    private static final int STATE_INIT  = 0;
    private static final int STATE_PROC  = 1;
    private static final int STATE_STACK = 2;


    public StackTraceScanner(StackTracePlugin stackTracePlugin) {
    }

    public Processes scan(BugReportModule br, int id, Section sec, String chapterName) {
        Pattern pNat = Pattern.compile("  #..  pc (........)  ([^() ]+)(?: \\((.*)\\+(.*)\\))?");
        Pattern pNatAlt = Pattern.compile("  #..  pc (........)  ([^() ]+) \\(deleted\\)");
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
                        curProc = new Process(br, processes, pid, fields[4], fields[5]);
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
                        String sysTid = null;
                        int fieldCount = fields.length;

                        // Check for native only threads
                        if (fieldCount == 1 && fields[0].startsWith("sysTid=")) {
                            threadState = "NATIVE_THREAD";
                            sysTid = fields[0];
                        }

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
                        if (sysTid != null) {
                            curStackTrace.parseProperties(sysTid);
                        }
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
                        StackTraceItem item = new StackTraceItem("", buff, 0);
                        curStackTrace.addStackTraceItem(item);
                        if (buff.startsWith("waiting ")) {
                            processWaitingToLockLine(curStackTrace, buff);
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
                                } else if (lineS.lastIndexOf(':') > 0) {
                                    int position = lineS.lastIndexOf(':');
                                    lineS = lineS.substring(position+1);
                                }
                                line = Integer.parseInt(lineS);
                            }
                            StackTraceItem item = new StackTraceItem(method, fileName, line);
                            curStackTrace.addStackTraceItem(item);
                        }
                    } else if (buff.startsWith("  #")) {
                        Matcher m = pNat.matcher(buff);
                        if (!m.matches()) {
                            m = pNatAlt.matcher(buff);
                        }
                        if (!m.matches()) {
                            br.printErr(4, "Cannot parse line: " + buff);
                            continue;
                        }
                        long pc = Long.parseLong(m.group(1), 16);
                        String fileName = m.group(2);
                        String method = (m.groupCount() >= 3) ? m.group(3) : null;
                        int methodOffset = (method == null) ? -1 : Integer.parseInt(m.group(4));
                        StackTraceItem item = new StackTraceItem(pc, fileName, method, methodOffset);
                        curStackTrace.addStackTraceItem(item);
                    }
            }

        }
        return processes;
    }

    private void processWaitingToLockLine(StackTrace curStackTrace, String buff) {
        int idx = -1;
        String needle = "";
        for (String possibleNeedle : getPossibleWaitingNeedles()) {
            idx = buff.indexOf(possibleNeedle);
            if (idx > 0) {
                needle = possibleNeedle;
                break;
            }
        }
        if (idx > 0) {
            idx += needle.length();
            int idx2 = buff.indexOf(' ', idx);
            if (idx2 < 0) {
                idx2 = buff.length();
            }
            if (idx2 > 0) {
                int tid = Integer.parseInt(buff.substring(idx, idx2));
                if (tid != curStackTrace.getTid()) {
                    String lockId = buff.substring(buff.indexOf("<") + 1, buff.indexOf(">"));
                    String lockType = buff.substring(buff.indexOf("(") + 1, buff.indexOf(")"));
                    curStackTrace.setWaitOn(new StackTrace.WaitInfo(tid, lockId, lockType));
                }
            }
        }
    }

    private Iterable<String> getPossibleWaitingNeedles() {
        List<String> list = new ArrayList<String>();
        list.add("held by threadid=");
        list.add("held by tid=");
        list.add("held by thread ");
        return list;
    }

}
