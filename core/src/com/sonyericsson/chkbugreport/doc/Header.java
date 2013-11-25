/*
 * Copyright (C) 2012-2013 Sony Mobile Communications AB
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

public class Header extends DocNode {

    private DocNode mName;
    private int mLevel;

    public Header(DocNode name) {
        mName = name;
    }

    public void setName(DocNode name) {
        mName = name;
    }

    @Override
    public void prepare(Renderer r) {
        mLevel = r.getLevel();
        mLevel = Math.min(6, mLevel);
        mLevel = Math.max(1, mLevel);
        mName.prepare(r);
    }

    @Override
    public void render(Renderer r) throws IOException {
        r.print("<h" + mLevel + ">");
        mName.render(r);
        r.println("</h" + mLevel + ">");
    }

}
