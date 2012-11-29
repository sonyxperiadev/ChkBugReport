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
package com.sonyericsson.chkbugreport.plugins.ftrace;

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.util.Util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/* package */ class VCDGenerator {

    private static final char STATE_SIGNALS[] = {'0', 'Z', 'W', 'X' };
    private static final char STATE_SIGNALS_IDLE[] = {'0', '0', '0', 'X' };

    private BugReportModule mBr;
    private String mFn;

    public VCDGenerator(BugReportModule br) {
        mBr = br;
        mFn = mBr.getRelRawDir() + "ftrace.vcd";
    }

    private char getSignal(int pid, int state) {
        if (pid == 0) return STATE_SIGNALS_IDLE[state];
        return STATE_SIGNALS[state];
    }

    public void execute(FTraceData data) {

        // Save the VCD file
        try {
            int runWaitBits = 8;
            FileOutputStream fos = new FileOutputStream(mBr.getBaseDir() + mFn);
            PrintStream fo = new PrintStream(fos);

            // write header
            fo.println("$timescale 1us $end");
            fo.println("$scope mytrace $end");

            fo.println("$var wire " + runWaitBits + " RUNWAIT Processes.Running.And.Waiting $end");

            for (int i = 0; i < 65535; i++) {
                FTraceProcessRecord proc = data.getProc(i);
                if (proc != null && proc.used > 0) {
                    String id = data.genId();
                    String name = data.getProc(i, mBr).getVCDName();
                    proc.id = id;
                    fo.println("$var wire 1 " + id + " " + name + " $end");
                }
            }

            fo.println("$upscope $end");
            fo.println("$enddefinitions $end");

            TraceRecord cur = data.getFirstTraceRecord();

            fo.println("#" + cur.time);
            fo.println("b" + Util.toBinary(0, runWaitBits) + " RUNWAIT");
            for (int i = 0; i < 65535; i++) {
                FTraceProcessRecord proc = data.getProc(i);
                if (proc != null && proc.used > 0) {
                    fo.println("b" + getSignal(i, proc.initState) + " " + proc.id);
                }
            }

            long lastTime = 0;
            int lastNrRunWait = 0;
            while (cur != null) {
                if (lastTime != cur.time) {
                    lastTime = cur.time;
                    fo.println("#" + cur.time);
                }

                // Update the number of processes running
                if (cur.nrRunWait != lastNrRunWait) {
                    lastNrRunWait = cur.nrRunWait;
                    fo.println("b" + Util.toBinary(lastNrRunWait, runWaitBits) + " RUNWAIT");
                }

                // Now check what happens with the prev task
                // In case of wakeup, nothing happens with the previous task, so we are
                // interested only in context switches
                if (cur.event == Const.SWITCH) {
                    FTraceProcessRecord prev = data.getProc(cur.prevPid, mBr);
                    int prevState = Const.calcPrevState(cur.prevState);
                    if (prevState != prev.state) {
                        // Change in state
                        if (prev.lastTime != 0) {
                            long elapsed = lastTime - prev.lastTime;
                            if (prev.state == Const.STATE_RUN) {
                                prev.runTime += elapsed;
                            } else if (prev.state == Const.STATE_WAIT) {
                                prev.waitTime += elapsed;
                                prev.waitTimeCnt++;
                                prev.waitTimeMax = Math.max(prev.waitTimeMax, (int)elapsed);
                            } else if (prev.state == Const.STATE_DISK) {
                                prev.diskTime += elapsed;
                                prev.diskTimeCnt++;
                                prev.diskTimeMax = Math.max(prev.diskTimeMax, (int)elapsed);
                            }
                        }
                        prev.state = prevState;
                        prev.lastTime = lastTime;
                        fo.println("b" + getSignal(prev.pid, prevState) + " " + prev.id);
                    }
                }

                // And let's see what happens with the new task
                FTraceProcessRecord next = data.getProc(cur.nextPid, mBr);
                int nextState = Const.STATE_RUN;
                if (cur.event == Const.WAKEUP) {
                    // Not running yet, so it must be waiting
                    nextState = Const.STATE_WAIT;
                }
                if (nextState != next.state) {
                    // Change in state
                    if (next.lastTime != 0) {
                        long elapsed = lastTime - next.lastTime;
                        if (next.state == Const.STATE_RUN) {
                            next.runTime += elapsed;
                        } else if (next.state == Const.STATE_WAIT) {
                            next.waitTime += elapsed;
                            next.waitTimeCnt++;
                            next.waitTimeMax = Math.max(next.waitTimeMax, (int)elapsed);
                        } else if (next.state == Const.STATE_DISK) {
                            next.diskTime += elapsed;
                            next.diskTimeCnt++;
                            next.diskTimeMax = Math.max(next.diskTimeMax, (int)elapsed);
                        }
                    }
                    next.state = nextState;
                    next.lastTime = lastTime;
                    fo.println("b" + getSignal(next.pid, nextState) + " " + next.id);
                }
                cur = cur.next;
            }
            fo.close();
            fos.close();
        } catch (IOException e) {
            mBr.printErr(3, FTracePlugin.TAG + "Error saving vcd file: " + e);
        }
    }

    public String getFileName() {
        return mFn;
    }

}
