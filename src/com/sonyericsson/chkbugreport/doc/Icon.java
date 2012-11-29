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

import java.io.IOException;

public class Icon extends DocNode {

    public static final int TYPE_FILE = 0;
    public static final int TYPE_SMALL = 1;
    public static final int TYPE_BIG = 2;

    private String mName;
    private int mType;

    public Icon(int type , String name) {
        mType = type;
        mName = name;
    }

    @Override
    public void render(Renderer r) throws IOException {
        if (mType == TYPE_SMALL) {
            r.println("<div class=\"builtin-icon builtin-icon-" + mName + "\"></div>");
        } else if (mType == TYPE_BIG) {
            r.println("<div class=\"builtin-big-icon builtin-icon-" + mName + "\"></div>");
        } else {
            r.print("<img src=\"" + mName + "\"/>");
        }
    }

}
