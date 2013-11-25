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

import com.sonyericsson.chkbugreport.ProcessRecord;
import com.sonyericsson.chkbugreport.util.Util;

/* package */ class FTraceProcessRecord {
    int pid;
    String name;
    int used;
    String id;
    int state = Const.STATE_SLEEP;
    long lastTime;
    long runTime;
    long waitTime;
    int waitTimeCnt;
    int waitTimeMax;
    long diskTime;
    int diskTimeCnt;
    int diskTimeMax;
    int initState = Const.STATE_SLEEP;
    boolean initStateSet = false;
    ProcessRecord procRec;

    public FTraceProcessRecord(int pid, String name) {
        this.pid = pid;
        this.name = name;
    }

    public String getName() {
        if (name != null) return name;
        return Integer.toString(pid);
    }

    public String getVCDName() {
        if (name != null) {
            return Util.fixVCDName(name);
        }
        return Integer.toString(pid);
    }

}

