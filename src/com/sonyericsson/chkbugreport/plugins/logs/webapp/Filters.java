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
package com.sonyericsson.chkbugreport.plugins.logs.webapp;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.webserver.JSON;
import com.sonyericsson.chkbugreport.webserver.engine.HTTPRequest;
import com.sonyericsson.chkbugreport.webserver.engine.HTTPResponse;

import java.util.Vector;

public class Filters {

    /** The set of filter groups */
    private Vector<FilterGroup> mFilterGroups = new Vector<FilterGroup>();

    private FilterGroup find(String filterName) {
        if (filterName != null) {
            for (FilterGroup fg : mFilterGroups) {
                if (filterName.equals(fg.getName())) {
                    return fg;
                }
            }
        }
        return null;
    }

    public void listFilters(Module mod, HTTPRequest req, HTTPResponse resp) {
        JSON json = new JSON();
        JSON filters = json.addArray("filters");
        for (FilterGroup fg : mFilterGroups) {
            filters.add(fg.getName());
        }
        json.writeTo(resp);
    }

    public void listFilter(Module mod, HTTPRequest req, HTTPResponse resp) {
        JSON json = new JSON();
        String filterName = req.getArg("filter", null);
        FilterGroup fg = find(filterName);
        if (fg == null) {
            // NOP - return empty object in case of error
        } else {
            int count = fg.getCount();
            json.add("name", fg.getName());
            json.add("count", count);
            JSON arr = json.addArray("filters");
            for (int i = 0; i < count; i++) {
                Filter f = fg.get(i);
                JSON item = arr.add();
                item.add("action", f.getAction().name());
                item.add("actionArg", f.getActionArg());
                item.add("tag", f.getTag());
                item.add("msg", f.getMsg());
                item.add("line", f.getLine());
            }
        }
    }

}
