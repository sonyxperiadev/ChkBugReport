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

}
