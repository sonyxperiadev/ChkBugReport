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

/* package */ class Const {

    // Event types
    public static final int UNKNOWN = 0;
    public static final int WAKEUP = 1;
    public static final int SWITCH = 2;

    // Process states
    public static final int STATE_SLEEP = 0;
    public static final int STATE_DISK = 1;
    public static final int STATE_WAIT = 2;
    public static final int STATE_RUN = 3;

    public static int calcPrevState(char srcState) {
        if (srcState == 'R') return STATE_WAIT;
        if (srcState == 'D') return STATE_DISK;
        return STATE_SLEEP;
    }

}
