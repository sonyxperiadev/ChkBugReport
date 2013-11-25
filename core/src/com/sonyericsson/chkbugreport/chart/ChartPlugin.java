/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
 * Copyright (C) 2012-2013 Sony Mobile Communications AB
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
package com.sonyericsson.chkbugreport.chart;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.doc.DocNode;

public abstract class ChartPlugin {

    // Some color suggestions
    public static final int COL_GREEN     = 0x4080ff80;
    public static final int COL_YELLOW    = 0x80ffff80;
    public static final int COL_RED       = 0xc0ff8080;

    public abstract boolean init(Module mod, ChartGenerator chart);

    public abstract long getFirstTs();

    public abstract long getLastTs();

    public DocNode getPreface() {
        return null; // NOP
    }

    public DocNode getAppendix() {
        return null; // NOP
    }

}
