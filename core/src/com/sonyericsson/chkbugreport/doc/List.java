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

public class List extends DocNode {

    public static final int TYPE_UNORDERED = 0;
    public static final int TYPE_ORDERED = 1;

    private int mType;

    public List() {
        this(TYPE_UNORDERED);
    }

    public List(int type) {
        mType = type;
    }

    public List(int type, DocNode parent) {
        mType = type;
        if (parent != null) {
            parent.add(this);
        }
    }

    @Override
    public void render(Renderer r) throws IOException {
        r.println(mType == TYPE_UNORDERED ? "<ul>" : "<ol>");
        int cnt = getChildCount();
        for (int i = 0; i < cnt; i++) {
            r.print("<li>");
            getChild(i).render(r);
            r.println("</li>");
        }
        r.println(mType == TYPE_UNORDERED ? "</ul>" : "</ol>");
    }


}
