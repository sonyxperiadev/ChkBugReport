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

import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.doc.Renderer;
import com.sonyericsson.chkbugreport.util.Rect;
import com.sonyericsson.chkbugreport.util.Util;

/**
 * This class generates the low level javascript code to render and display the view hierarchy
 */
public class ViewHierarchyGenerator extends DocNode {

    private View mViews;

    public ViewHierarchyGenerator(View views) {
        mViews = views;
    }

    @Override
    public void render(Renderer r) {
        try {
            renderBE(r);
        } catch (Throwable e) {
            System.err.println("Error running ViewHierarchyGenerator.render:");
            e.printStackTrace();
        }
    }

    private void renderBE(Renderer r) {
        // Precalculate canvas size and scale factor
        // Decide on a good enough size
        int outW, outH, size = 512;
        float scale;
        Rect rect = mViews.getRect();
        if (rect.w < rect.h) {
            scale = size * 1.0f / rect.h;
            outH = size;
            outW = rect.w * outH / rect.h;
        } else {
            scale = size * 1.0f / rect.w;
            outW = size;
            outH = rect.h * outW / rect.w;
        }

        // Render the skeleton
        r.println("<div class='view-hierarchy'>");
        r.println("<table border='0'><tr><td valign='top'>");
        r.println(String.format("<canvas id='canvas' style='cursor: crosshair' onClick='onCanvasClick(this,event)', width='%d' height='%d' />", outW, outH));
        r.println("<style type='text/css'>.tree span { cursor: pointer; }</style>");

        // Write the script
        r.println("<script type='text/javascript'>");
        r.println("var outW=" + outW + ";");
        r.println("var outH=" + outH + ";");
        r.println("var scale=" + scale + ";");
        r.println("var views = ");
        dumpViewJson(r, mViews, "");
        r.println(";");
        r.println("");
        Util.printResource(r, "appactivities.js");
        r.println("</script>");

        // Render tree view
        r.println("</td><td valign='top'>");
        r.println("<div class='tree' style='overflow-y: auto; height: 512px'>");
        r.println("<span></span>");
        r.println("<ul><li>");
        dumpViewTree(r, mViews);
        r.println("</li></ul>");
        r.println("</div>");
        r.println("</td></tr></table>");

        r.println("</div>");
    }

    private void dumpViewTree(Renderer r, View view) {
        Rect rc = view.getRect();
        String id = (view.getId() == null) ? "" : "(" + view.getId() + ")";
        r.println(String.format("<span id='n%d' onClick='onClick(%d)'>%s%s [%s %s] (%d,%d-%d*%d)</span>",
                view.getUid(), view.getUid(),
                view.getName(), id, view.getFlags0(), view.getFlags1(), rc.x, rc.y, rc.w, rc.h));
        int cnt = view.getChildCount();
        if (cnt > 0) {
            r.println("<ul>");
            for (int i = 0; i < cnt; i++) {
                r.println("<li>");
                dumpViewTree(r, view.getChild(i));
                r.println("</li>");
            }
            r.println("</ul>");
        }
    }

    private void dumpViewJson(Renderer r, View view, String indent) {
        String pref = indent + "  ";
        r.println(String.format("%s{", indent));
        r.println(String.format("%s'id' : '%s',", pref, view.getId()));
        r.println(String.format("%s'uid' : %s,", pref, view.getUid()));
        r.println(String.format("%s'name' : '%s',", pref, view.getName()));
        r.println(String.format("%s'flags0' : '%s',", pref, view.getFlags0()));
        r.println(String.format("%s'flags1' : '%s',", pref, view.getFlags1()));
        r.println(String.format("%s'x' : %s,", pref, view.getRect().x));
        r.println(String.format("%s'y' : %s,", pref, view.getRect().y));
        r.println(String.format("%s'w' : %s,", pref, view.getRect().w));
        r.println(String.format("%s'h' : %s,", pref, view.getRect().h));
        r.println(String.format("%s'children' : [", pref, view.getRect().h));
        int cnt = view.getChildCount();
        for (int i = 0; i < cnt; i++) {
            dumpViewJson(r, view.getChild(i), pref);
            if (i < cnt - 1) {
                r.println(",");
            } else {
                r.println("");
            }
        }
        r.println(String.format("%s]", pref));
        r.print(String.format("%s}", indent));
    }

}
