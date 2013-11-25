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
package com.sonyericsson.chkbugreport.plugins;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.Hint;
import com.sonyericsson.chkbugreport.doc.TreeView;
import com.sonyericsson.chkbugreport.util.DumpTree;

public class MiscPlugin extends Plugin {

    private static final String TAG = "[MiscPlugin]";

    @Override
    public int getPrio() {
        return 99;
    }

    @Override
    public void reset() {
        // NOP
    }

    @Override
    public void load(Module mod) {
        // NOP
    }

    @Override
    public void generate(Module mod) {
        convertToTreeView(mod, Section.APP_ACTIVITIES, "ActivityManager/App activities");
        convertToTreeView(mod, Section.APP_SERVICES, "ActivityManager/App services");
        convertToTreeView(mod, Section.DUMP_OF_SERVICE_PACKAGE, "ActivityManager/Dump of package service");
    }

    private void convertToTreeView(Module mod, String secName, String chName) {
        // Load data
        Section section = mod.findSection(secName);
        if (section == null) {
            mod.printErr(3, TAG + "Section not found: " + secName + " (ignoring)");
            return;
        }

        // Parse the data
        DumpTree dump = new DumpTree(section, 0);
        Chapter ch = mod.findOrCreateChapter(chName);
        new Hint(ch).add("Under construction! For now it contains the raw data in a tree-view.");
        ch.add(convertToTreeView(dump.getRoot(), 0));
    }

    private TreeView convertToTreeView(DumpTree.Node node, int level) {
        TreeView ret = new TreeView(node.getLine(), level++);
        for (DumpTree.Node child : node) {
            ret.add(convertToTreeView(child, level));
        }
        return ret;
    }

}
