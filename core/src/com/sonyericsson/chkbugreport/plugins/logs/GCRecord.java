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
package com.sonyericsson.chkbugreport.plugins.logs;

public class GCRecord {

    public long ts;
    public int pid;
    public int memFreeAlloc;
    public int memExtAlloc;
    public int memFreeSize;
    public int memExtSize;

    public GCRecord(long ts, int pid, int memFreeAlloc, int memFreeSize, int memExtAlloc, int memExtSize) {
        this.ts = ts;
        this.pid = pid;
        this.memFreeAlloc = memFreeAlloc;
        this.memExtAlloc = memExtAlloc;
        this.memFreeSize = memFreeSize;
        this.memExtSize = memExtSize;
    }
}
