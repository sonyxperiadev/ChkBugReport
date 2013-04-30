/*
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
package com.sonyericsson.chkbugreport.doc;

import com.sonyericsson.chkbugreport.util.Util;

public class Anchor extends DocNode {

    private String mName;
    private String mFileName;
    private String mPrefix;

    public Anchor(String name) {
        mName = name;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getHRef() {
        if (mFileName == null) {
            return mFileName;
        }
        if (mName == null) {
            return mFileName;
        } else {
            return mFileName + "#" + mPrefix + mName;
        }

    }

    @Override
    public void prepare(Renderer r) {
        mFileName = r.getFileName();
        mPrefix = "";
        while (mFileName == null) {
            mPrefix = r.getChapter().getId() + "$";
            r = r.getParent();
            mFileName = r.getFileName();
        }
        Util.assertNotNull(mFileName);
    }

    @Override
    public void render(Renderer r) {
        r.print("<a name=\"" + mPrefix + mName + "\"></a>");
    }

}
