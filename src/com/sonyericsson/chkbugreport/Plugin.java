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
package com.sonyericsson.chkbugreport;

import com.sonyericsson.chkbugreport.util.XMLNode;
import com.sonyericsson.chkbugreport.webserver.ChkBugReportWebServer;

public abstract class Plugin {

    /**
     * Return the priority of this plugin.
     * Lower value means it will run sooner.
     * @return The priority of this plugin (lower = sooner)
     */
    public abstract int getPrio();

    /**
     * The plugin must reset it's state, i.e. it must forget everything from a previous run.
     */
    public abstract void reset();

    /**
     * Placeholder for hooking into other plugins.
     * Currently only the external (xml) plugins are using this.
     * @param mod The module instance
     */
    public void hook(Module mod) {
        // NOP
    }

    /**
     * Called when an external (xml) plugin want's to hook into this plugin.
     * @param mod The module instance
     * @param hook The xml file describing the hook
     */
    public void onHook(Module mod, XMLNode hook) {
        mod.printErr(4, "onHook not implemented in plugin: " + this);
    }

    /**
     * Parses the input and load into memory.
     * At this phase there might be some other plugins which
     * haven't been run yet. So if this plugin depends on data from another
     * plugin, it might need to postpone some processing.
     * @param mod The reference to the current bugreport.
     */
    public abstract void load(Module mod);

    /**
     * Save the collected info in the bugreport.
     * @param mod The reference to the current bugreport.
     */
    public abstract void generate(Module mod);

    /**
     * Placeholder for plugins to execute code after the report is created.
     * @param mod The reference to the current bugreport.
     */
    public void finish(Module mod) {
        // NOP
    }

    /**
     * Detect the type of the section based on the snippet.
     * @param module The active module
     * @param buff The buffer containing the beginning of a section/file
     * @param offs The offset of the data in the buffer
     * @param len The length of the data in the buffer
     * @return The type of the file (the name of the section as it would appear in the bugreport)
     */
    public void autodetect(Module module, byte[] buff, int offs, int len, GuessedValue<String> type) {
        // NOP
    }

    /**
     * Allow a plugin to handle a file itself.
     * This could be used to create plugins which load files from other sources
     * @param module The module which needs the file
     * @param fileName The name of the input file
     * @param type The type of the file (i.e. the associated section name)
     * @return true if the file was handled by this module and should not be processed further
     */
    public boolean handleFile(Module module, String fileName, String type) {
        return false;
    }

    /**
     * Allow the plugin to register itself to the webserver
     * @param ws The webserver instance
     */
    public void setWebServer(ChkBugReportWebServer ws) {
        // NOP
    }

}
