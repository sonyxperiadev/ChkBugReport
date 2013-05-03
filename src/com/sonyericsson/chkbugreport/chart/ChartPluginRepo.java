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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

public class ChartPluginRepo implements Iterable<ChartPluginInfo>, Comparator<ChartPluginInfo> {

    public Vector<ChartPluginInfo> mPlugins = new Vector<ChartPluginInfo>();
    public HashMap<String, ChartPluginInfo> mPluginsByName = new HashMap<String, ChartPluginInfo>();

    public void add(ChartPluginInfo info) {
        mPlugins.add(info);
        mPluginsByName.put(info.getName(), info);
        Collections.sort(mPlugins, this);
    }

    public ChartPluginInfo get(String name) {
        return mPluginsByName.get(name);
    }

    public int getCount() {
        return mPlugins.size();
    }

    public ChartPluginInfo get(int idx) {
        return mPlugins.get(idx);
    }

    @Override
    public Iterator<ChartPluginInfo> iterator() {
        return mPlugins.iterator();
    }

    @Override
    public int compare(ChartPluginInfo o1, ChartPluginInfo o2) {
        return o1.getName().compareTo(o2.getName());
    }

}
