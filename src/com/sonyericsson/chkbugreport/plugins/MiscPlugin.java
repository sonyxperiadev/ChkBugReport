/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
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
package com.sonyericsson.chkbugreport.plugins;

import com.sonyericsson.chkbugreport.BugReport;
import com.sonyericsson.chkbugreport.Chapter;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.Report;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.Util;
import com.sonyericsson.chkbugreport.util.DumpTree;

public class MiscPlugin extends Plugin {

    private static final String TAG = "[MiscPlugin]";

    @Override
    public int getPrio() {
        return 99;
    }

    @Override
    public void load(Report br) {
        // NOP
    }


    @Override
    public void generate(Report rep) {
        BugReport br = (BugReport) rep;
        convertToTreeView(br, Section.APP_ACTIVITIES, "App activities");
        convertToTreeView(br, Section.APP_SERVICES, "App services");
        convertToTreeView(br, Section.DUMP_OF_SERVICE_PACKAGE, "Dump of package service");
    }

    private void convertToTreeView(BugReport br, String secName, String chName) {
        // Load data
        Section section = br.findSection(secName);
        if (section == null) {
            br.printErr(3, TAG + "Section not found: " + secName + " (ignoring)");
            return;
        }

        // Parse the data
        DumpTree dump = new DumpTree(section, 0);
        Chapter ch = new Chapter(br, chName);
        br.addChapter(ch);

        ch.addLine("<div class=\"hint\">(Under construction! For now it contains the raw data in a tree-view.)</div>");
        ch.addLine("<div class=\"tree\">");
        convertToTreeView(dump.getRoot(), ch);
        ch.addLine("</div>");

    }

    private void convertToTreeView(DumpTree.Node node, Chapter ch) {
        String line = node.getLine();
        if (line != null) {
            ch.addLine("<span>" + Util.escape(line) + "</span>");
        }
        int cnt = node.getChildCount();
        if (cnt > 0) {
            ch.addLine("<ul>");
            for (int i = 0; i < cnt; i++) {
                DumpTree.Node child = node.getChild(i);
                ch.addLine("<li>");
                convertToTreeView(child, ch);
                ch.addLine("</li>");
            }
            ch.addLine("</ul>");
        }
    }
}
