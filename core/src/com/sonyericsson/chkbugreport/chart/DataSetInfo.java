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
package com.sonyericsson.chkbugreport.chart;

import com.sonyericsson.chkbugreport.Module;

public class DataSetInfo implements ChartPluginInfo {

    private DataSet mDs;
    private String mName;

    public DataSetInfo(DataSet ds, String prefix) {
        mDs = ds;
        mName = prefix + "/" + ds.getName();
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public ChartPlugin createInstance() {
        return new ChartPlugin() {

            @Override
            public boolean init(Module mod, ChartGenerator chart) {
                chart.add(mDs);
                return true;
            }

            @Override
            public long getLastTs() {
                return mDs.getLastTs();
            }

            @Override
            public long getFirstTs() {
                return mDs.getFirstTs();
            }
        };
    }

}
