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
import java.util.HashMap;
import java.util.Map.Entry;

public class HtmlNode extends DocNode {

    private String mTag;
    private String mId;
    private String mStyles;
    private String mTitle;
    private HashMap<String, String> mAttrs = null;

    public HtmlNode(String tag, DocNode parent) {
        this(tag, parent, null);
    }

    public HtmlNode(String tag) {
        this(tag, null, null);
    }

    public HtmlNode(String tag, String text) {
        this(tag, null, text);
    }

    public HtmlNode(String tag, DocNode parent, String text) {
        mTag = tag;
        if (parent != null) {
            parent.add(this);
        }
        if (text != null) {
            add(text);
        }
    }

    public HtmlNode setId(String string) {
        mId = string;
        return this;
    }

    public HtmlNode setTag(String tag) {
        mTag = tag;
        return this;
    }

    public HtmlNode setTitle(String title) {
        mTitle = title;
        return this;
    }

    public HtmlNode addStyle(String style) {
        if (mStyles == null) {
            mStyles = style;
        } else {
            mStyles = mStyles + " " + style;
        }
        return this;
    }

    public HtmlNode setName(String name) {
        setAttr("name", name);
        return this;
    }

    public HtmlNode setAttr(String key, String value) {
        if (mAttrs == null) {
            mAttrs = new HashMap<String, String>();
        }
        mAttrs.put(key, value);
        return this;
    }

    @Override
    public void render(Renderer r) throws IOException {
        r.print("<");
        r.print(mTag);
        if (mId != null) {
            r.print(" id=\"");
            r.print(mId);
            r.print("\"");
        }
        if (mStyles != null) {
            r.print(" class=\"");
            r.print(mStyles);
            r.print("\"");
        }
        if (mTitle != null) {
            r.print(" title=\"");
            r.print(mTitle);
            r.print("\"");
        }
        if (mAttrs != null) {
            for (Entry<String, String> kv : mAttrs.entrySet()) {
                r.print(" " + kv.getKey() + "=\"" + kv.getValue() + "\"");
            }
        }
        renderAttrs(r);
        r.print(">");
        super.render(r);
        r.print("</");
        r.print(mTag);
        r.print(">");
    }

    protected void renderAttrs(Renderer r) {
        // NOP
    }

}
