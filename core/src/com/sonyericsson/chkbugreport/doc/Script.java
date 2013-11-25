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
package com.sonyericsson.chkbugreport.doc;

import com.sonyericsson.chkbugreport.util.Util;

import java.io.IOException;

public class Script extends DocNode {

    private String mResName;
    private StringBuilder mContent;

    public Script(DocNode parent) {
        super(parent);
        mContent = new StringBuilder();
    }

    public Script(DocNode parent, String jsResName) {
        super(parent);
        mResName = jsResName;
    }

    public void println(String line) {
        mContent.append(line);
        mContent.append('\n');
    }

    @Override
    public void render(Renderer r) throws IOException {
        r.println("<script type=\"text/javascript\">");
        if (mResName != null) {
            Util.printResource(r, mResName);
        } else if (mContent != null) {
            r.print(mContent.toString());
        }
        r.println("</script>");
    }

}
