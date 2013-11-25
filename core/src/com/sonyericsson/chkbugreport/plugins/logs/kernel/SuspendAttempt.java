/*
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
package com.sonyericsson.chkbugreport.plugins.logs.kernel;

import com.sonyericsson.chkbugreport.Lines;

import java.util.Vector;

/* package */ class SuspendAttempt {

    public static final int STATE_UNKNOWN = -1;
    public static final int STATE_FAILED = 0;
    public static final int STATE_SUCCEEDED = 1;

    public int state = STATE_UNKNOWN;
    public Vector<String> wakelocks = new Vector<String>();
    public Lines log = new Lines(null);

    public void addWakelock(String name) {
        wakelocks.add(name);
    }

}
