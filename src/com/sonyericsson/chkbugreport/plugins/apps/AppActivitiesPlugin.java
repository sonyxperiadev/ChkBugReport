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
import com.sonyericsson.chkbugreport.doc.Img;
import com.sonyericsson.chkbugreport.doc.Para;
import com.sonyericsson.chkbugreport.doc.TreeView;
import com.sonyericsson.chkbugreport.util.DumpTree;
import com.sonyericsson.chkbugreport.util.DumpTree.Node;
import com.sonyericsson.chkbugreport.util.Rect;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

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
        Node views = actNode.find("View Hierarchy:");
        if (views == null) return;
        act.setViewHierarchy(new View(mod, views.getChild(0)));

    }

    @Override
    public void generate(Module mod) {
        if (!mLoaded) return;

        // Create the chapter
        Chapter ch = mod.findOrCreateChapter("Applications/Running");
        for (Task task : mTasks) {
            Chapter chTask = new Chapter(mod, task.getName());
            ch.addChapter(chTask);
            for (int i = 0; i < task.getActivityCount(); i++) {
                Activity act = task.getActivity(i);
                Chapter chAct = new Chapter(mod, act.getName());
                chTask.addChapter(chAct);

                String fn = "views-" + act.getPid();
                if (renderViews(mod, fn, act)) {
                    chAct.add(new Para().add("View hierarchy: "));
                    chAct.add(new Img(fn));
                    TreeView tree = generateTreeView(act);
                    if (tree != null) {
                        chAct.add(tree);
                    }
                }
            }
        }
    }

    private TreeView generateTreeView(Activity act) {
        View views = act.getViewHierarchy();
        if (views == null) return null;
        return generateTreeView(views, 0);
    }

    private TreeView generateTreeView(View views, int level) {
        TreeView ret = new TreeView(views.toString(), level);
        for (int i = 0; i < views.getChildCount(); i++) {
            View child = views.getChild(i);
            ret.add(generateTreeView(child, level + 1));
        }
        return ret;
    }

    private boolean renderViews(Module mod, String fn, Activity act) {
        View views = act.getViewHierarchy();
        if (views == null) return false;

        // Decide on a good enough size
        int outW, outH, size = 512;
        float scale;
        Rect rect = views.getRect();
        if (rect.w < rect.h) {
            scale = size * 1.0f / rect.h;
            outH = size;
            outW = rect.w * outH / rect.h;
        } else {
            scale = size * 1.0f / rect.w;
            outW = size;
            outH = rect.h * outW / rect.w;
        }

        // Render the composited image
        BufferedImage img = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) img.getGraphics();
        g.scale(scale, scale);
        renderView(g, -rect.x, -rect.y, views);
        try {
            ImageIO.write(img, "png", new File(mod.getBaseDir() + fn));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void renderView(Graphics2D g, int dx, int dy, View views) {
        char v = views.getFlags0().charAt(0);
        if (v != 'V') {
            // View not visible!
            return;
        }
        Rect r = views.getRect();
        int x0 = dx + r.x;
        int y0 = dy + r.y;
        g.setColor(new Color(0x0800ff00, true));
        g.fillRect(x0, y0, r.w, r.h);
        g.setColor(new Color(0x4000ff00, true));
        g.drawRect(x0, y0, r.w, r.h);

        for (int i = 0; i < views.getChildCount(); i++) {
            View child = views.getChild(i);
            renderView(g, x0, y0, child);
        }
    }

}
