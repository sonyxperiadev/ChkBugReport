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
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.util.DumpTree;
import com.sonyericsson.chkbugreport.util.DumpTree.Node;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppActivitiesPlugin extends Plugin {

    private static final String TAG = "[ViewHierarchyPlugin]";

    private boolean mLoaded;

    private Vector<Task> mTasks = new Vector<Task>();

    @Override
    public int getPrio() {
        return 85;
    }

    @Override
    public void reset() {
        mTasks.clear();
        mLoaded = false;
    }

    @Override
    public void load(Module mod) {
        Section sec = mod.findSection(Section.APP_ACTIVITIES);
        if (sec == null) {
            mod.printErr(3, TAG + "Section not found: " + Section.APP_ACTIVITIES + " (aborting plugin)");
            return;
        }

        DumpTree tree = new DumpTree(sec, 0);
        Node root = tree.getRoot();
        Pattern pTask = Pattern.compile("TASK (.+) id=([0-9]+)");
        Pattern pAct = Pattern.compile("ACTIVITY (.+) ([0-9a-f]+) pid=([0-9]+)");

        // Cycle through tasks
        for (int i = 0; i < root.getChildCount(); i++) {
            Node taskNode = root.getChild(i);
            Matcher m = pTask.matcher(taskNode.getLine());
            if (!m.matches()) continue;
            String taskName = m.group(1);
            int taskId = Integer.parseInt(m.group(2));
            Task task = new Task(taskName, taskId);
            mTasks.add(task);

            // Cycle through activities
            for (int j = 0; j < taskNode.getChildCount(); j++) {
                Node actNode = taskNode.getChild(j);
                m = pAct.matcher(actNode.getLine());
                if (!m.matches()) continue;

                String actName = m.group(1);
                int pid = Integer.parseInt(m.group(3));
                Activity act = new Activity(actName, pid, task);
                task.add(act);

                parseViewHierarchy(mod, act, actNode);
            }
        }
        mLoaded = true;
    }

    private void parseViewHierarchy(Module mod, Activity act, Node actNode) {
        Node views = actNode.find("View Hierarchy:", false);
        if (views == null) return;
        act.setViewHierarchy(new View(mod, views.getChild(0)));

    }

    @Override
    public void generate(Module mod) {
        if (!mLoaded) return;

        // Create the chapter
        Chapter ch = mod.findOrCreateChapter("Applications/Running");
        for (Task task : mTasks) {
            Chapter chTask = new Chapter(mod.getContext(), task.getName());
            chTask.setStandalone(true);
            ch.addChapter(chTask);
            for (int i = 0; i < task.getActivityCount(); i++) {
                Activity act = task.getActivity(i);
                Chapter chAct = new Chapter(mod.getContext(), act.getName());
                chAct.setStandalone(true);
                chTask.addChapter(chAct);

                View views = act.getViewHierarchy();
                if (views != null && views.getRect().w > 0 && views.getRect().h > 0) {
                    chAct.add(new ViewHierarchyGenerator(views));
                }
            }
        }
    }

}
