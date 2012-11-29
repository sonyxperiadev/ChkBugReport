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
package com.sonyericsson.chkbugreport.doc;

import java.io.IOException;

public class ShadedValue extends DocNode {

    private long mValue;

    public ShadedValue(long value) {
        mValue = value;
    }

    @Override
    public String getText() {
        return Long.toString(mValue);
    }

    @Override
    public void render(Renderer r) throws IOException {
        if (mValue < 1000) {
            r.print("<span class=\"kb\">");
            r.print(mValue);
            r.print("</span>");
        } else {
            long mb = mValue / 1000;
            long kb = mValue % 1000;
            if (mb != 0) {
                r.print(mb);
            }
            r.print("<span class=\"kb\">");
            if (kb < 100) {
                r.print('0');
            }
            if (kb < 10) {
                r.print('0');
            }
            r.print(kb);
            r.print("</span>");
        }
    }


}
