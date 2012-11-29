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

public class Button extends HtmlNode {

    private String mOnClick;

    public Button(String text, String onClick) {
        super("button", text);
        mOnClick = onClick;
    }

    @Override
    protected void renderAttrs(Renderer r) {
        super.renderAttrs(r);
        if (mOnClick != null) {
            r.print(" onClick=\"");
            r.print(mOnClick);
            r.print("\"");
        }
    }

}
