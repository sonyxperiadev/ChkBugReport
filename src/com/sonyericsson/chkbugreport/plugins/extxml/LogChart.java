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
import com.sonyericsson.chkbugreport.chart.ChartGenerator;
import com.sonyericsson.chkbugreport.chart.LogFilterChartPlugin;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.doc.Para;
import com.sonyericsson.chkbugreport.util.XMLNode;

/* package */ class LogChart {

    private Module mMod;
    private Chapter mCh;
    private XMLNode mCode;
    private LogFilterChartPlugin mPlugin;

    public LogChart(Module mod, Chapter ch, XMLNode code) {
        mMod = mod;
        mCh = ch;
        mCode = code;
        mPlugin = LogFilterChartPlugin.parse(mMod, mCode);
    }

    public void exec() {
        // And finally create the chart
        String title = mCode.getAttr("name");
        String fn = mCode.getAttr("file");

        ChartGenerator chart = new ChartGenerator(title);
        chart.addPlugin(mPlugin);
        chart.setOutput(fn);
        DocNode ret = chart.generate(mMod);
        if (ret != null) {
            mCh.add(ret);
        } else {
            mCh.add(new Para().add("Chart data missing!"));
        }
    }

}
