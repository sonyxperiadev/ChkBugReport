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

import java.util.HashMap;
import java.util.Vector;

import com.sonyericsson.chkbugreport.Bug;
import com.sonyericsson.chkbugreport.Chapter;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.Report;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.Util;
import com.sonyericsson.chkbugreport.plugins.WindowManagerPlugin.WindowManagerState.Window;
import com.sonyericsson.chkbugreport.util.DumpTree;

public class WindowManagerPlugin extends Plugin {

    private static final String TAG = "[WindowManagerPlugin]";

    private static final String EXTRA_SECTIONS[] = {
        Section.WINDOW_MANAGER_POLICY_STATE,
        Section.WINDOW_MANAGER_SESSIONS,
        Section.WINDOW_MANAGER_TOKENS,
        Section.WINDOW_MANAGER_WINDOWS,
    };

    private boolean mLoaded;
    private Section mSection;
    private EventHubState mEventHubState;
    private WindowManagerState mWindowManagerState;

    @Override
    public int getPrio() {
        return 81;
    }

    @Override
    public void load(Report br) {
        // Reset
        mLoaded = false;
        mSection = null;
        mEventHubState = null;
        mWindowManagerState = null;

        // Load data
        mSection = br.findSection(Section.DUMP_OF_SERVICE_WINDOW);
        if (mSection == null) {
            br.printErr(TAG + "Section not found: " + Section.DUMP_OF_SERVICE_WINDOW + " (aborting plugin)");
            return;
        }

        // Parse the data
        DumpTree dump = new DumpTree(mSection, 0);

        // Parse some extra sections (appeared in ICS)
        for (String sectionName : EXTRA_SECTIONS) {
            Section tmp = br.findSection(sectionName);
            if (tmp != null) {
                DumpTree tmpTree = new DumpTree(tmp, 0);
                DumpTree.Node parent = new DumpTree.Node(sectionName);
                parent.add(tmpTree);
                dump.add(parent);
            }
        }

        // Parse the different chunks
        loadEventHubState(br, dump);
        loadWindowManagerState(br, dump);

        // Done
        mLoaded = true;
    }

    private boolean loadEventHubState(Report br, DumpTree dump) {
        final String nodeKey = "Event Hub State:";
        DumpTree.Node root = dump.find(nodeKey);
        if (root == null) {
            br.printErr("Cannot find node '" + nodeKey + "'");
            return false;
        }

        // Read the rest of the chunk
        mEventHubState = new EventHubState();
        boolean foundDevices = false;

        for (DumpTree.Node node : root) {
            String line = node.getLine();
            String value = Util.getValueAfter(line, ':');
            String key = Util.getKeyBefore(line, ':');
            if ("HaveFirstKeyboard".equals(key)) {
                mEventHubState.haveFirstKeyboard = Util.parseBoolean(value);
            } else if ("FirstKeyboardId".equals(key)) {
                mEventHubState.firstKeyboardId = Util.parseHex(value);
            } else if ("Devices".equals(key)) {
                foundDevices = true;
                for (DumpTree.Node devNode : node) {
                    line = devNode.getLine();
                    value = Util.getValueAfter(line, ':');
                    key = Util.getKeyBefore(line, ':');

                    EventHubState.Device dev = new EventHubState.Device();
                    dev.id = Util.parseHex(key);
                    dev.name = Util.strip(value);
                    mEventHubState.devices.add(dev);

                    for (DumpTree.Node propNode : devNode) {
                        line = propNode.getLine();
                        value = Util.getValueAfter(line, ':');
                        key = Util.getKeyBefore(line, ':');

                        if ("Classes".equals(key)) {
                            dev.classes = Util.parseHex(value);
                        }
                        if ("Path".equals(key)) {
                            dev.path = Util.strip(value);
                        }
                        if ("KeyLayoutFile".equals(key)) {
                            dev.kbLayout = Util.strip(value);
                        }
                    }
                }
            }
        }
        if (!foundDevices) {
            br.printErr("Device list not found in event hub state");
            return false;
        }

        return true;
    }

    private boolean loadWindowManagerState(Report br, DumpTree dump) {
        final String nodeKey1 = "Current Window Manager state:";
        final String nodeKey2 = Section.WINDOW_MANAGER_WINDOWS;
        DumpTree.Node root = dump.find(nodeKey1);
        if (root == null) {
            root = dump.find(nodeKey2);
        }
        if (root == null) {
            br.printErr("Cannot find node '" + nodeKey1 + "' or '" + nodeKey2 + "'");
            return false;
        }

        // Read the rest of the chunk
        // TODO: read the data (if it's useful), for now we ignore it
        mWindowManagerState = new WindowManagerState();
        WindowManagerState.Window win = null;
        int winIdx = 0;
        for (DumpTree.Node node : root) {
            String line = node.getLine();
            if (line.startsWith("Window #")) {
                win = new WindowManagerState.Window();
                win.idx = winIdx++;
                mWindowManagerState.windows.add(win);
                win.num = Util.parseInt(Util.extract(line, "#", " "));
                String winDescr = Util.extract(line, "{", "}");
                int idx0 = winDescr.indexOf(' ');
                int idx1 = winDescr.lastIndexOf(' ');
                win.id = Util.parseHex(winDescr, 0, idx0);
                win.name = winDescr.substring(idx0 + 1, idx1);
                win.paused = Util.parseBoolean(Util.extract(winDescr, "paused=", null));

                for (DumpTree.Node propNode : node) {
                    line = propNode.getLine();
                    // If we got here, we need to append more properties to the window
                    if (line.startsWith("mAttrs=")) {
                        parseWindowAttr(win, Util.extract(line, "{", "}"));
                    } else if (line.startsWith("mSurface=")) {
                        String descr = Util.extract(line, "(", ")");
                        win.surfaceId = Util.extract(descr, "identity=", " ");
                    } else if (line.startsWith("mViewVisibility=")) {
                        win.visibity = Util.parseHex(Util.extract(line, "mViewVisibility=", " "));
                    } else if (line.startsWith("mBaseLayer=")) {
                        String value = Util.extract(line, "mAnimLayer=", " ");
                        value = Util.extract(value, "=", " ");
                        win.animLayer = Util.parseInt(value);
                    } else if (line.startsWith("mAttachedWindow=")) {
                        String descr = Util.extract(line, "{", "}");
                        int idx = descr.indexOf(' ');
                        win.parentId = Util.parseHex(descr, 0, idx);
                    }
                }
            }
        }

        // Lookup parents
        for (int i = 0; i < mWindowManagerState.windows.size(); i++) {
            win = mWindowManagerState.windows.get(i);
            if (win.parentId != 0) {
                win.parent = mWindowManagerState.lookupWin(win.parentId);
            }
        }

        return true;
    }

    private void parseWindowAttr(Window win, String attrs) {
        String values[] = attrs.split(" ");
        for (String s : values) {
            if (s.startsWith("flags=")) {
                win.flags = Util.parseHex(Util.extract(attrs, "flags=", " "));
            } else if (s.startsWith("or=")) {
                win.or = Util.parseInt(Util.extract(attrs, "or=", " "));
            } else if (s.startsWith("fmt=")) {
                win.fmt = Util.parseInt(Util.extract(attrs, "fmt=", " "));
            }
        }
    }

    @Override
    public void generate(Report br) {
        if (!mLoaded) return;

        // Generate the report
        Chapter mainCh = new Chapter(br, "WindowManager");
        br.addChapter(mainCh);

        if (mWindowManagerState != null && mWindowManagerState.windows != null) {
            generateWindowList(br, mainCh);
        }
    }

    public void generateWindowList(Report br, Chapter mainCh) {
        // Generate window list (for now)
        String anchor = "windowlist";
        Chapter ch = mainCh;
        ch.addLine("<div class=\"hint\">(Under construction)</div>");
        ch.addLine("<p><a name=\"" + anchor + "\">Window list:</a></p>");
        ch.addLine("<div class=\"winlist\">");
        int count = mWindowManagerState.windows.size();
        for (int i = 0; i < count; i++) {
            WindowManagerState.Window win = mWindowManagerState.windows.get(i);
            String att = null;
            if (win.parent != null) {
                if (win.parent.idx == win.idx - 1) {
                    att = "prev";
                } else if (win.parent.idx < win.idx) {
                    att = "before";
                } else if (win.parent.idx == win.idx + 1) {
                    att = "next";
                } else if (win.parent.idx > win.idx) {
                    att = "after";
                }
            }
            if (att != null) {
                att = "<div class=\"winlist-icon winlist-icon-att-" + att + "\"> </div>";
            } else {
                att = "";
            }
            String vis = "vis";
            if (win.visibity == 4) {
                vis = "invis";
            } else if (win.visibity == 8) {
                vis = "gone";
            }
            String icon = "<div class=\"winlist-icon winlist-icon-item\"> </div>";
            String hint = "|";
            if (i == 0) {
                hint = "top";
            } else if (i == 1) {
                hint = "^";
            } else if (i == count - 1) {
                hint  = "bottom";
            }
            hint = "<div style=\"color: #ccc; width: 2cm; float: left; text-align: center;\">" + hint + "</div>";
            ch.addLine(hint + "<div class=\"winlist-" + vis + "\">" + icon + att + Util.simplifyComponent(win.name) + "</div>");
        }
        ch.addLine("</div>");

        // Check for possible errors
        checkDuplicatedWindows(br, mainCh, anchor);
        checkWrongOrder(br, mainCh, anchor);
    }

    private void checkDuplicatedWindows(Report br, Chapter mainCh, String anchor) {
        // Check for possible errors based on the window list (like duplicate windows)
        HashMap<String, WindowCount> counts = new HashMap<String, WindowCount>();
        for (WindowManagerState.Window win : mWindowManagerState.windows) {
            String name = win.name;
            if (name.equals("SurfaceView")) continue; // This can have many instances
            WindowCount wc = counts.get(name);
            if (wc == null) {
                wc = new WindowCount();
                wc.name = name;
                wc.count = 1;
                counts.put(name, wc);
            } else {
                wc.count++;
            }
        }
        Bug bug = null;
        for (WindowCount wc : counts.values()) {
            if (wc.count > 1) {
                if (bug == null) {
                    bug = new Bug(Bug.PRIO_MULTIPLE_WINDOWS, 0, "Multiple window instances found");
                    bug.addLine("<p>There are multiple window instances with the same name!");
                    bug.addLine("This can be normal in some cases, but it could also point to a memory/window/activity leak!</p>");
                    bug.addLine("<ul>");
                }
                bug.addLine("<li>" + wc.name + " (x" + wc.count + ")</li>");
            }
        }
        if (bug != null) {
            bug.addLine("</ul>");
            bug.addLine("<p class=\"hint\"><a href=\"" + br.createLinkTo(mainCh, anchor) + "\">(Link to window list)</a></p>");
            br.addBug(bug);
        }
    }

    private void checkWrongOrder(Report br, Chapter mainCh, String anchor) {
        // Check for possible errors based on the window list (like duplicate windows)
        Bug bug = null;
        int lastLayer = -1;
        for (WindowManagerState.Window win : mWindowManagerState.windows) {
            if (lastLayer != -1) {
                if (lastLayer < win.animLayer) {
                    // Create the bug if needed
                    if (bug == null) {
                        bug = new Bug(Bug.PRIO_WRONG_WINDOW_ORDER, 0, "Wrong window order");
                        bug.addLine("<p>The order of the windows does not match their layers!");
                        bug.addLine("When this happens, the user might see one window on top, but interact with another one.");
                        bug.addLine("The following windows are placed incorrectly (too low):</p>");
                        bug.addLine("<ul>");
                    }
                    bug.addLine("<li>" + win.name + " (" + win.animLayer + " > " + lastLayer + ")</li>");
                }
            }
            lastLayer = win.animLayer;
        }
        if (bug != null) {
            bug.addLine("</ul>");
            bug.addLine("<p class=\"hint\"><a href=\"" + br.createLinkTo(mainCh, anchor) + "\">(Link to window list)</a></p>");
            br.addBug(bug);
        }
    }

    static class WindowCount {
        String name;
        int count;
    }

    static class EventHubState {

        static class Device {
            public String kbLayout;
            public String path;
            public int classes;
            public String name;
            public int id;
        }

        public int firstKeyboardId;
        public boolean haveFirstKeyboard;
        public Vector<Device> devices = new Vector<Device>();

    }

    static class WindowManagerState {

        static class Window {

            public int idx;
            public Window parent;
            public int parentId;
            public int visibity;
            public String surfaceId;
            public int fmt;
            public int or;
            public int flags;
            public boolean paused;
            public String name;
            public int id;
            public int num;
            public int animLayer;

        }

        public Vector<Window> windows = new Vector<Window>();

        public Window lookupWin(int id) {
            for (Window win : windows) {
                if (win.id == id) {
                    return win;
                }
            }
            return null;
        }
    }

}
