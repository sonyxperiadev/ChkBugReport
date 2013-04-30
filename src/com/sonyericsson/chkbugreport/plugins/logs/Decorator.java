/*
 * Copyright (C) 2013 Sony Mobile Communications AB
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

import com.sonyericsson.chkbugreport.doc.Renderer;

public abstract class Decorator {

    private int mStart = -1;
    private int mEnd = -1;

    public Decorator(int start, int end) {
        mStart = start;
        mEnd = end;
    }

    public int getStart() {
        return mStart;
    }

    public int getEnd() {
        return mEnd;
    }

    public abstract void render(Renderer renderer, boolean start);

    public int compare(Decorator other) {
        if (mStart < other.mStart) return -1;
        if (mStart > other.mStart) return +1;
        if (mEnd < other.mEnd) return -1;
        if (mEnd > other.mEnd) return +1;
        return 0;
    }

    public boolean isEmpty() {
        return mStart >= mEnd;
    }

}
