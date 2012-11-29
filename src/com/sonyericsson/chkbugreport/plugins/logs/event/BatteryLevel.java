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
package com.sonyericsson.chkbugreport.plugins.logs.event;

public class BatteryLevel {

    private int mLevel;
    private int mVolt;
    private int mTemp;
    private long mTs;
    private long mMsPerMV;
    private long mMVPerHour;

    public BatteryLevel(int level, int volt, int temp, long ts, long msPerMV, long mVPerHour) {
        mLevel = level;
        mVolt = volt;
        mTemp = temp;
        mTs = ts;
        mMsPerMV = msPerMV;
        mMVPerHour = mVPerHour;
    }

    public int getLevel() {
        return mLevel;
    }

    public int getVolt() {
        return mVolt;
    }

    public int getTemp() {
        return mTemp;
    }

    public long getTs() {
        return mTs;
    }

    public long getMsPerMV() {
        return mMsPerMV;
    }

    public long getMVPerHour() {
        return mMVPerHour;
    }

}
