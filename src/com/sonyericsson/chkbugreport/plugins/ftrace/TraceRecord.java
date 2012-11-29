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

/* package */ class TraceRecord {
    long time;
    int prevPid, nextPid;
    char prevState, nextState;
    int event;
    int nrRunWait;
    TraceRecord next;

    TraceRecord(long time, int prev, int next, char prevState, char nextState, int event) {
        this.time = time;
        this.prevPid = prev;
        this.nextPid = next;
        this.prevState = prevState;
        this.nextState = nextState;
        this.event = event;
        this.next = null;
        this.nrRunWait = 0;
    }
}
