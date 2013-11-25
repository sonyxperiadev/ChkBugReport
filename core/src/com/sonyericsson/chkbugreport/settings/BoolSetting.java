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

public class BoolSetting extends Setting {

    private boolean mValue;

    public BoolSetting(boolean defValue, Settings owner, String id, String descr) {
        super(owner, id, descr);
        mValue = defValue;
    }

    public boolean get() {
        return mValue;
    }

    public void set(boolean b) {
        mValue = b;
    }

    @Override
    public void load(Properties props) {
        String value = props.getProperty(getId());
        if (value != null) {
            if ("true".equals(value)) {
                mValue = true;
            }
            if ("false".equals(value)) {
                mValue = false;
            }
        }
    }

    @Override
    public void store(Properties props) {
        props.setProperty(getId(), mValue ? "true" : "false");
    }

}
