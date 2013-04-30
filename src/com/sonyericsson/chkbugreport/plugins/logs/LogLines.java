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

import java.util.Vector;

@SuppressWarnings("serial")
public class LogLines extends Vector<LogLine> {

    private LogLine mCachedLastItem = null;
    private int mSeq = 0;

    @Override
    public synchronized boolean add(LogLine item) {
        // Need to generate an id on the fly
        if (mCachedLastItem != null && mCachedLastItem.ts == item.ts) {
            mSeq++;
        } else {
            mSeq = 0;
        }
        item.id = (item.ts << 16) + mSeq;
        mCachedLastItem = item;
        // Update ID
        return super.add(item);
    }

}
