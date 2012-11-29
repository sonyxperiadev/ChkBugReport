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
package com.sonyericsson.chkbugreport.plugins.extxml;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.chart.LogFilterChartPlugin;
import com.sonyericsson.chkbugreport.plugins.battery.BatteryInfoPlugin;
import com.sonyericsson.chkbugreport.util.XMLNode;

/* package */ class BatteryLogChart {

    private Module mMod;
    private LogFilterChartPlugin mPlugin;

    public BatteryLogChart(Module mod, XMLNode code) {
        mMod = mod;
        mPlugin = LogFilterChartPlugin.parse(mMod, code);
    }

    public void exec() {
        BatteryInfoPlugin bip = (BatteryInfoPlugin) mMod.getPlugin(BatteryInfoPlugin.class.getSimpleName());
        bip.addBatteryLevelChartPlugin(mPlugin);
    }

}
