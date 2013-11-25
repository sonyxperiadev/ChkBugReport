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
package com.sonyericsson.chkbugreport.plugins.charteditor;

import com.sonyericsson.chkbugreport.util.SavedField;
import com.sonyericsson.chkbugreport.util.SavedField.Type;
import com.sonyericsson.chkbugreport.util.Util;

public class ChartData {

    private static final String SEPARATOR = "|";
    private static final String SEPARATOR_RE = "\\|";

    @SavedField(type = Type.ID)
    private int mId;
    @SavedField(type = Type.VARCHAR)
    private String mName;
    @SavedField(type = Type.VARCHAR)
    private String mPlugins;

    public ChartData(String name) {
        mName = name;
        mPlugins = "";
    }

    public int getId() {
        return mId;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    public void setPlugins(String v) {
        mPlugins = v;
    }

    public String getPlugins() {
        return mPlugins;
    }

    public void deletePlugin(String plugin) {
        if (Util.isEmpty(mPlugins)) return;
        String p[] = mPlugins.split(SEPARATOR_RE);
        StringBuilder ret = new StringBuilder();
        for (String s : p) {
            if (s.equals(plugin)) continue;
            if (ret.length() > 0) {
                ret.append(SEPARATOR);
            }
            ret.append(s);
        }
        mPlugins = ret.toString();
    }

    public void addPlugin(String plugin) {
        if (!Util.isEmpty(mPlugins)) {
            mPlugins += SEPARATOR;
        }
        mPlugins += plugin;
    }

    public String[] getPluginsAsArray() {
        if (Util.isEmpty(mPlugins)) {
            return new String[0];
        }
        return mPlugins.split(SEPARATOR_RE);
    }

}
