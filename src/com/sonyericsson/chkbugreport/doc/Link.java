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

public class Link extends DocNode {

    private Anchor mAnchor;
    private String mAnchorText;
    private String mText;
    private String mTarget;
    private String mTitle;

    public Link(Anchor anchor, String text) {
        mAnchor = anchor;
        mAnchorText = null;
        mText = text;
    }

    public Link(String anchor, String text) {
        mAnchor = null;
        mAnchorText = anchor;
        mText = text;
    }

    public Link setTarget(String target) {
        mTarget = target;
        return this;
    }

    @Override
    public void render(Renderer r) throws IOException {
        // Handle special case first: link points to anchor not included in the output
        if (mAnchor != null && mAnchor.getHRef() == null) {
            // TODO: this should really not happen! But we still shouldn't crash
            System.err.println("Link points to missing anchor! text=" + mText);
            return;
        }

        r.print("<a href=\"");
        if (mAnchor == null) {
            r.print(mAnchorText);
        } else {
            r.print(mAnchor.getHRef());
        }
        r.print("\"");
        if (mTarget != null) {
            r.print(" target=\"");
            r.print(mTarget);
            r.print("\"");
        }
        if (mTitle != null) {
            r.print(" title=\"");
            r.print(mTitle);
            r.print("\"");
        }
        r.print(">");
        if (mText == null) {
            super.render(r);
        } else {
            r.print(mText);
        }
        r.print("</a>");
    }

    public void setTitle(String string) {
        mTitle = string;
    }

}
