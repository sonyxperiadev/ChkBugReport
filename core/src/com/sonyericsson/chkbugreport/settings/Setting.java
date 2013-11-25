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
package com.sonyericsson.chkbugreport.settings;

import java.util.Properties;

public abstract class Setting {

    private Settings mOwner;
    private String mId;
    private String mDescr;

    public Setting(Settings owner, String id, String descr) {
        mOwner = owner;
        mId = id;
        mDescr = descr;
        owner.add(this);
    }

    public Settings getOwner() {
        return mOwner;
    }

    public String getId() {
        return mId;
    }

    public String getDescription() {
        return mDescr;
    }

    abstract public void load(Properties props);

    abstract public void store(Properties props);

}
