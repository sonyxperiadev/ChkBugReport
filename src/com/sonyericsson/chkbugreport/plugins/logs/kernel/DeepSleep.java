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

public class DeepSleep {

    private long mLastRealTs;
    private long mLastTs;
    private long mRealTs;
    private long mTs;

    public DeepSleep(long lastRealTs, long lastTs, long realTs, long ts) {
        mLastRealTs = lastRealTs;
        mLastTs = lastTs;
        mRealTs = realTs;
        mTs = ts;
    }

    public long getLastRealTs() {
        return mLastRealTs;
    }

    public long getLastTs() {
        return mLastTs;
    }

    public long getRealTs() {
        return mRealTs;
    }

    public long getTs() {
        return mTs;
    }

}
