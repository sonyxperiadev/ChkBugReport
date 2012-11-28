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
package com.sonyericsson.chkbugreport.plugins.battery;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.chart.ChartGenerator;
import com.sonyericsson.chkbugreport.chart.ChartPlugin;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.plugins.logs.event.BatteryLevels;

import java.util.Vector;

public class BatteryLevelGenerator {

    private ChartGenerator mChartGen = new ChartGenerator("Battery level");

    public BatteryLevelGenerator(BatteryLevels batteryLevels) {
        mChartGen.addPlugin(new BatteryLevelChart(batteryLevels));
    }

    public void generate(Module br, Chapter mainCh) {
        Chapter ch = new Chapter(br, "Battery level");
        if (generateGraph(br, ch)) {
            mainCh.addChapter(ch);
        }
    }

    private boolean generateGraph(Module br, Chapter ch) {
        String fn = "eventlog_batterylevel_graph.png";

        DocNode ret = mChartGen.generate(br, fn);
        if (ret == null) {
            return false;
        }
        ch.add(ret);
        return true;
    }

    public void addPlugins(Vector<ChartPlugin> plugins) {
        mChartGen.addPlugins(plugins);
    }

}
