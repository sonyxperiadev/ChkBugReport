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

import com.sonyericsson.chkbugreport.util.db.DBField;
import com.sonyericsson.chkbugreport.util.db.DBField.Type;
import com.sonyericsson.chkbugreport.util.db.DbBackedData;

import java.sql.Connection;

public class FilterGroup extends DbBackedData<Filter> {

    @DBField(type = Type.ID)
    private int mId;
    @DBField(type = Type.VARCHAR)
    private String mName;

    public FilterGroup(Connection conn, String prefix, String name) {
        super(conn, prefix + "_filters");
        mName = name;
    }

    public int getId() {
        return mId;
    }

    public int getCount() {
        return getData().size();
    }

    @Override
    public void add(Filter item) {
        item.setGroupId(mId);
        super.add(item);
    }

    public Filter get(int idx) {
        return getData().get(idx);
    }

    public void setName(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    @Override
    protected Filter createItem() {
        return new Filter(null, null, null, null, 0);
    }

}
