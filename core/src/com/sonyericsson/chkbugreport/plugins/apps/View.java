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
package com.sonyericsson.chkbugreport.plugins.apps;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.util.DumpTree.Node;
import com.sonyericsson.chkbugreport.util.Rect;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class View {

    private static final String N = "(-?[0-9]+)";
    private static final Pattern P = Pattern.compile("(.*)\\{([0-9a-f]+) ([^ ]+) ([^ ]+) "+N+","+N+"-"+N+","+N+"( #[0-9a-f]+( (.*))?)?\\}");

    private int mUid;
    private String mName;
    private String mFlags0;
    private String mFlags1;
    private String mId;
    private Rect mRect;
    private Vector<View> mChildren = new Vector<View>();

    public View(Module mod, Node node) {
        Matcher m = P.matcher(node.getLine());
        if (!m.matches()) {
            mod.printErr(4, "Cannot parse View: " + node.getLine());
            return;
        }
        mName = m.group(1);
        mUid = Integer.parseInt(m.group(2), 16);
        mFlags0 = m.group(3);
        mFlags1 = m.group(4);
        int x = Integer.parseInt(m.group(5));
        int y = Integer.parseInt(m.group(6));
        int w = Integer.parseInt(m.group(7)) - x;
        int h = Integer.parseInt(m.group(8)) - y;
        mRect = new Rect(x, y, w, h);
        mId = m.group(11);

        for (int i = 0; i < node.getChildCount(); i++) {
            Node child = node.getChild(i);
            View childView = new View(mod, child);
            mChildren.add(childView);
        }
    }

    public int getUid() {
        return mUid;
    }

    public String getName() {
        return mName;
    }

    public String getFlags0() {
        return mFlags0;
    }

    public String getFlags1() {
        return mFlags1;
    }

    public String getId() {
        return mId;
    }

    public Rect getRect() {
        return mRect;
    }

    public int getChildCount() {
        return mChildren.size();
    }

    public View getChild(int i) {
        return mChildren.get(i);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(mName);
        if (mId != null) {
            sb.append("(");
            sb.append(mId);
            sb.append(")");
        }
        sb.append(" ");
        sb.append(mRect.x);
        sb.append(",");
        sb.append(mRect.y);
        sb.append("-");
        sb.append(mRect.w);
        sb.append("*");
        sb.append(mRect.h);
        sb.append(" ");
        sb.append(mFlags0);
        sb.append(" ");
        sb.append(mFlags1);
        return sb.toString();
    }

}
