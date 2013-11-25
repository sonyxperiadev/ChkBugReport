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
import com.sonyericsson.chkbugreport.util.SaveFile;
import com.sonyericsson.chkbugreport.util.SavedData;
import com.sonyericsson.chkbugreport.util.Util;
import com.sonyericsson.chkbugreport.webserver.JSON;
import com.sonyericsson.chkbugreport.webserver.engine.HTTPRequest;
import com.sonyericsson.chkbugreport.webserver.engine.HTTPResponse;

public class Filters extends SavedData<FilterGroup> {

    /** Prefix used in table names */
    private String mPrefix;

    public Filters(SaveFile saveFile, String prefix) {
        super(saveFile, prefix + "_filter_groups");
        mPrefix = prefix;
        load();
    }

    public FilterGroup find(String filterName) {
        if (filterName != null) {
            for (FilterGroup fg : getData()) {
                if (filterName.equals(fg.getName())) {
                    return fg;
                }
            }
        }
        return null;
    }

    public void listFilterGroups(Module mod, HTTPRequest req, HTTPResponse resp) {
        JSON json = new JSON();
        JSON filters = json.addArray("filters");
        for (FilterGroup fg : getData()) {
            filters.add(fg.getName());
        }
        json.writeTo(resp);
    }

    public void newFilterGroup(Module mod, HTTPRequest req, HTTPResponse resp) {
        JSON json = new JSON();
        String name = req.getArg("name");
        if (name == null || name.length() == 0) {
            json.add("err", 400);
            json.add("msg", "Name is not specified or empty!");
        } else if (!name.matches("[a-zA-Z0-9_]+")) {
            json.add("err", 400);
            json.add("msg", "Invalid characters in name!");
        } else if (null != find(name)) {
            json.add("err", 400);
            json.add("msg", "A filter with that name already exists!");
        } else {
            FilterGroup fg = new FilterGroup(getSaveFile(), mPrefix, name);
            add(fg);
            json.add("err", 200);
            json.add("msg", "Filter created!");
        }
        json.writeTo(resp);
    }

    public void deleteFilterGroup(Module mod, HTTPRequest req, HTTPResponse resp) {
        JSON json = new JSON();
        String filterName = req.getArg("filter", null);
        FilterGroup fg = find(filterName);
        if (fg == null) {
            json.add("err", 400);
            json.add("msg", "Cannot find filter group!");
        } else {
            // Note: first all items needs to be deleted!
            for (int i = fg.getCount() - 1; i >= 0; i--) {
                fg.delete(fg.get(i));
            }
            delete(fg);
            json.add("err", 200);
            json.add("msg", "Filter group deleted!");
        }
        json.writeTo(resp);
    }

    public void listFilters(Module mod, HTTPRequest req, HTTPResponse resp) {
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
                item.add("idx", i);
                item.add("id", f.getId());
                item.add("action", f.getAction().name());
                item.add("actionArg", f.getActionArg());
                item.add("tag", f.getTag());
                item.add("msg", f.getMsg());
                item.add("line", f.getLine());
            }
        }
        json.writeTo(resp);
    }

    public void newFilter(Module mod, HTTPRequest req, HTTPResponse resp) {
        JSON json = new JSON();
        String tag = Util.strip(req.getArg("tag"));
        String msg = Util.strip(req.getArg("msg"));
        String line = Util.strip(req.getArg("line"));
        String action = req.getArg("action");
        Filter.Action actionV = Filter.Action.valueOf(action);
        String filterName = req.getArg("filter", null);
        FilterGroup fg = find(filterName);

        if (Util.isEmpty(tag) && Util.isEmpty(msg) && Util.isEmpty(line)) {
            json.add("err", 400);
            json.add("msg", "A pattern for at least the log tag, log message or the whole log line must be specified!");
        } else if (actionV == null) {
            json.add("err", 400);
            json.add("msg", "An action must be specified!");
        } else if (fg == null) {
            json.add("err", 400);
            json.add("msg", "Cannot find filter group!");
        } else {
            Filter f = new Filter(tag, msg, line, actionV, 0);
            fg.add(f);
            json.add("err", 200);
            json.add("msg", "Filter created!");
        }
        json.writeTo(resp);
    }

    public void updateFilter(Module mod, HTTPRequest req, HTTPResponse resp) {
        JSON json = new JSON();
        int id = Integer.parseInt(req.getArg("id"));
        String tag = Util.strip(req.getArg("tag"));
        String msg = Util.strip(req.getArg("msg"));
        String line = Util.strip(req.getArg("line"));
        String action = req.getArg("action");
        Filter.Action actionV = Filter.Action.valueOf(action);
        String filterName = req.getArg("filter", null);
        FilterGroup fg = find(filterName);
        Filter f = (fg == null) ? null : fg.findById(id);
        if (Util.isEmpty(tag) && Util.isEmpty(msg) && Util.isEmpty(line)) {
            json.add("err", 400);
            json.add("msg", "A pattern for at least the log tag, log message or the whole log line must be specified!");
        } else if (actionV == null) {
            json.add("err", 400);
            json.add("msg", "An action must be specified!");
        } else if (f == null) {
            json.add("err", 400);
            json.add("msg", "Cannot find filter or filter group!");
        } else {
            f.setTag(tag);
            f.setMsg(msg);
            f.setLine(line);
            f.setAction(actionV);
            fg.update(f);
            json.add("err", 200);
            json.add("msg", "Filter updated!");
        }
        json.writeTo(resp);
    }

    public void deleteFilter(Module mod, HTTPRequest req, HTTPResponse resp) {
        JSON json = new JSON();
        int id = Integer.parseInt(req.getArg("id"));
        String filterName = req.getArg("filter", null);
        FilterGroup fg = find(filterName);
        Filter f = (fg == null) ? null : fg.findById(id);
        if (f == null) {
            json.add("err", 400);
            json.add("msg", "Cannot find filter or filter group!");
        } else {
            fg.delete(f);
            json.add("err", 200);
            json.add("msg", "Filter deleted!");
        }
        json.writeTo(resp);
    }

    @Override
    protected FilterGroup createItem() {
        return new FilterGroup(getSaveFile(), mPrefix, null);
    }

    @Override
    protected void onLoaded(FilterGroup item) {
        item.load("mGroupId", item.getId());
    }

}
