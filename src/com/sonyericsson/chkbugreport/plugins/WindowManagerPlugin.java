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

public class WindowManagerPlugin extends Plugin {

    private static final String TAG = "[WindowManagerPlugin]";
    private boolean mLoaded;
    private int mLineIdx;
    private Section mSection;
    private EventHubState mEventHubState;
    private WindowManagerState mWindowManagerState;

    @Override
    public int getPrio() {
        return 81;
    }

    private String readLine() {
        if (mLineIdx < mSection.getLineCount()) {
            return mSection.getLine(mLineIdx++);
        } else {
            return null;
        }
    }

    private void rewind(int delta) {
        mLineIdx = Math.max(0, mLineIdx - delta);
    }

    @Override
    public void load(Report br) {
        // Reset
        mLoaded = false;
        mLineIdx = 0;
        mSection = null;
        mEventHubState = null;
        mWindowManagerState = null;

        // Load data
        mSection = br.findSection(Section.DUMP_OF_SERVICE_WINDOW);
        if (mSection == null) {
            br.printErr(TAG + "Section not found: " + Section.DUMP_OF_SERVICE_WINDOW + " (aborting plugin)");
            return;
        }

        // Parse the different chunks
        if (loadEventHubState(br)) {
            // Probably 2.3
            loadInputReaderState(br);
            loadInputDispatcherState(br);
            loadWindowManagerState(br);
        } else if (loadInputState(br)) {
            // Maybe 2.2
            loadWindowManagerState(br);
        } else {
            // Unknown android... either very old or very new
            // Still let's tyr to parse it
            loadWindowManagerState(br);
        }

        // Done
        mLoaded = true;
    }

    private boolean checkChunk(Report br, String header) {
        // Load first line, and validate
        while (true) {
            String line = Util.strip(readLine());
            if (line != null && line.length() == 0) {
                continue; // skip empty lines
            }
            if (line == null || !line.equals(header)) {
                br.printErr("Cannot find '" + header + "' chunk.");
                rewind(1);
                return false;
            }
            return true;
        }
    }

    private boolean loadEventHubState(Report br) {
        if (!checkChunk(br, "Event Hub State:")) {
            return false;
        }
        // Read the rest of the chunk
        mEventHubState = new EventHubState();
        boolean foundDevices = false;
        String line = null;
        while (null != (line = readLine())) {
            if (line.length() == 0) {
                break;
            } else if (line.startsWith("  HaveFirstKeyboard:")) {
                mEventHubState.haveFirstKeyboard = Util.parseBoolean(line, line.indexOf(':') + 1, line.length());
            } else if (line.startsWith("  FirstKeyboardId:")) {
                mEventHubState.firstKeyboardId = Util.parseHex(line, line.indexOf(':') + 1, line.length());
            } else if (line.startsWith("  Devices:")) {
                foundDevices = true;
                break;
            } else {
                // Unknown info, ignore
            }
        }
        if (!foundDevices) {
            br.printErr("Device list not found in event hub state");
            return false;
        }
        while (null != (line = readLine())) {
            if (line.length() == 0) {
                break;
            }
            EventHubState.Device dev = new EventHubState.Device();
            int idx = line.indexOf(':');
            if (idx < 0) {
                br.printErr("Cannot parse device list in event hub state");
                return false;
            }
            dev.id = Util.parseHex(line, 0, idx);
            dev.name = Util.strip(line.substring(idx + 1));
            mEventHubState.devices.add(dev);

            while (null != (line = readLine())) {
                if (!line.startsWith("      ")) {
                    rewind(1);
                    break;
                }
                if (line.startsWith("      Classes:")) {
                    dev.classes = Util.parseHex(line, line.indexOf(':') + 1, line.length());
                }
                if (line.startsWith("      Path:")) {
                    dev.path = Util.strip(line.substring(line.indexOf(':') + 1));
                }
                if (line.startsWith("      KeyLayoutFile:")) {
                    dev.kbLayout = Util.strip(line.substring(line.indexOf(':') + 1));
                }
            }
        }

        return true;
    }

    private boolean loadInputReaderState(Report br) {
        if (!checkChunk(br, "Input Reader State:")) {
            return false;
        }
        String line = null;
        // Read the rest of the chunk
        // TODO: read the data (if it's useful), for now we ignore it
        while (null != (line = readLine())) {
            if (line.length() == 0) {
                break;
            }
        }
        return true;
    }

    private boolean loadInputDispatcherState(Report br) {
        if (!checkChunk(br, "Input Dispatcher State:")) {
            return false;
        }
        String line = null;
        // Read the rest of the chunk
        // TODO: read the data (if it's useful), for now we ignore it
        while (null != (line = readLine())) {
            if (line.length() == 0) {
                break;
            }
        }
        return true;
    }

    private boolean loadInputState(Report br) {
        if (!checkChunk(br, "Input State:")) {
            return false;
        }
        String line = null;
        // Read the rest of the chunk
        // TODO: read the data (if it's useful), for now we ignore it
        boolean ok = false;
        while (null != (line = readLine())) {
            line = Util.strip(line);
            if (line.contains("Default keyboard:")) {
                ok = true;
            }
            if (ok && line.length() == 0) {
                break;
            }
        }
        return true;
    }

    private boolean loadWindowManagerState(Report br) {
        if (!checkChunk(br, "Current Window Manager state:")) {
            return false;
        }
        String line = null;
        // Read the rest of the chunk
        // TODO: read the data (if it's useful), for now we ignore it
        mWindowManagerState = new WindowManagerState();
        WindowManagerState.Window win = null;
        int winIdx = 0;
        while (null != (line = readLine())) {
            if (line.length() <= 2) {
                break;
            }
            if (line.startsWith("  Window")) {
                win = new WindowManagerState.Window();
                win.idx = winIdx++;
                mWindowManagerState.windows.add(win);
                win.num = Util.parseInt(Util.extract(line, "#", " "));
                String descr = Util.extract(line, "{", "}");
                int idx0 = descr.indexOf(' ');
                int idx1 = descr.lastIndexOf(' ');
                win.id = Util.parseHex(descr, 0, idx0);
                win.name = descr.substring(idx0 + 1, idx1);
                win.paused = Util.parseBoolean(Util.extract(descr, "paused=", null));
                continue;
            }
            // If we got here, we need to append more properties to the window
            if (line.startsWith("    mAttrs=")) {
                parseWindowAttr(win, Util.extract(line, "{", "}"));
            } else if (line.startsWith("    mSurface=")) {
                String descr = Util.extract(line, "(", ")");
                win.surfaceId = Util.extract(descr, "identity=", " ");
            } else if (line.startsWith("    mViewVisibility=")) {
                win.visibity = Util.parseHex(Util.extract(line, "mViewVisibility=", " "));
            } else if (line.startsWith("    mAttachedWindow=")) {
                String descr = Util.extract(line, "{", "}");
                int idx = descr.indexOf(' ');
                win.parentId = Util.parseHex(descr, 0, idx);
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
        for (WindowManagerState.Window win : mWindowManagerState.windows) {
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
            ch.addLine("<div class=\"winlist-" + vis + "\">" + icon + att + Util.simplifyComponent(win.name) + "</div>");
        }
        ch.addLine("</div>");

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
