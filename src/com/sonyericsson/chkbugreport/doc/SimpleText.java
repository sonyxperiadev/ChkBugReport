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

public class SimpleText extends DocNode {

    /* Various flags for chapters */
    public static final int FLAG_NONE   = 0x00;
    public static final int FLAG_BOLD   = 0x01;
    public static final int FLAG_ITALIC = 0x02;
    public static final int FLAG_STRIKE = 0x04;

    /** The text to render */
    private String mText;
    /** Flags controlling the generation of the chapter */
    private int mFlags = FLAG_NONE;

    public SimpleText(String text) {
        mText = text;
    }

    public SimpleText(String text, int flags) {
        mText = text;
        mFlags = flags;
    }

    @Override
    public String getText() {
        return mText;
    }

    public void setText(String text) {
        mText = text;
    }

    @Override
    public void render(Renderer r) {
        if (0 != (mFlags & FLAG_BOLD)) {
            r.print("<b>");
        }
        if (0 != (mFlags & FLAG_ITALIC)) {
            r.print("<i>");
        }
        if (0 != (mFlags & FLAG_STRIKE)) {
            r.print("<strike>");
        }
        r.print(mText);
        if (0 != (mFlags & FLAG_STRIKE)) {
            r.print("</strike>");
        }
        if (0 != (mFlags & FLAG_ITALIC)) {
            r.print("</i>");
        }
        if (0 != (mFlags & FLAG_BOLD)) {
            r.print("</b>");
        }
    }

    public void setFlags(int bits, boolean set) {
        if (set) {
            mFlags |= bits;
        } else {
            mFlags &= ~bits;
        }
    }

    @Override
    public SimpleText copy() {
        return new SimpleText(mText, mFlags);
    }

}
