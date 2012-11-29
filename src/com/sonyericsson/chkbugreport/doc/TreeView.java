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

import com.sonyericsson.chkbugreport.util.HtmlUtil;

import java.io.IOException;

public class TreeView extends DocNode {

    private String mLine;
    private int mLevel;

    public TreeView(String line, int level) {
        mLine = line;
        mLevel = level;
    }

    @Override
    public void render(Renderer r) throws IOException {
        if (mLevel == 0) {
            r.println("<div class=\"tree\">");
        }
        if (mLine != null) {
            r.println("<span>" + HtmlUtil.escape(mLine) + "</span>");
        }
        int cnt = getChildCount();
        if (cnt > 0) {
            r.println("<ul>");
            for (int i = 0; i < cnt; i++) {
                r.println("<li>");
                getChild(i).render(r);
                r.println("</li>");
            }
            r.println("</ul>");
        }
        if (mLevel == 0) {
            r.println("</div>");
        }
    }

}
