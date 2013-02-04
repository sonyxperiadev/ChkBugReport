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
package com.sonyericsson.chkbugreport.plugins.extxml;

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.SimpleText;
import com.sonyericsson.chkbugreport.util.XMLNode;

/**
 * This plugin parses and "executes" and xml file.
 * This makes it possible to create plugins which parses the log easily,
 * without needing to recompile the application.
 */
public class ExtXMLPlugin extends Plugin {

    private XMLNode mXml;

    public ExtXMLPlugin(XMLNode xml) {
        mXml = xml;
        if (!xml.getName().equals("plugin")) {
            throw new RuntimeException("Invalid root tag");
        }
    }

    @Override
    public int getPrio() {
        // Let them run these at the end, when the rest of the data is parsed
        return 99;
    }

    @Override
    public void reset() {
        // NOP
    }

    @Override
    public void hook(Module mod) {
        // Execute the "hook" tag
        for (XMLNode hook : mXml) {
            if (!"hook".equals(hook.getName())) continue;
            String into = hook.getAttr("into");
            if (into == null) {
                mod.printErr(4, "Missing 'into' attribute in hook tag, ignoring whole tag!");
                continue;
            }
            Plugin dst = mod.getPlugin(into);
            if (dst == null) {
                mod.printErr(4, "Cannot find plugin to hook into: " + into);
                continue;
            }
            dst.onHook(mod, hook);
        }
    }

    @Override
    public void load(Module mod) {
        // Execute the "load" tag
        XMLNode load = mXml.getChild("load");
        if (load == null) {
            // <load> tag is missing, do nothing.
            return;
        }

        for (XMLNode chTag : load) {
            String tag = chTag.getName();
            if (tag == null) continue;
            // NOTE: nothing is support here yet
            mod.printErr(4, "Unknown tag is found in <load>, ignoreing it: " + tag);
        }
    }

    @Override
    public void generate(Module mod) {
        // Find the generate tag
        XMLNode gen = mXml.getChild("generate");
        if (gen == null) {
            // <generate> tag is missing, do nothing.
            return;
        }

        for (XMLNode chTag : gen) {
            String tag = chTag.getName();
            if (tag == null) continue;
            if ("chapter".equals(tag)) {
                Chapter ch = mod.findOrCreateChapter(chTag.getAttr("name"));
                // Now execute each child tag
                for (XMLNode code : chTag) {
                    exec(mod, ch, code);
                }
            } else {
                mod.printErr(4, "A non-chapter tag is found in <generate>, ignoreing it: " + tag);
            }
        }
    }

    private void exec(Module mod, Chapter ch, XMLNode code) {
        String type = code.getName();
        if (type == null) {
            // NOP
        } else if ("text".equals(type)) {
            // This is trivial
            if (code.getChildCount() == 1) {
                String text = code.getChild(0).getAttr("text");
                ch.add(new SimpleText(text));
            }
        } else if ("logchart".equals(type)) {
            // Creating a chart based on the log is a bit more complex, so let's delegate it
            new LogChart(mod, ch, code).exec();
        } else if ("log".equals(type)) {
            // Create a log
            new Log((BugReportModule) mod, ch, code).exec();
        } else {
            mod.printErr(4, "Unknown code tag in logchart: " + code.getName());
        }

    }

}
