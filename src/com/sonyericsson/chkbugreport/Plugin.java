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
package com.sonyericsson.chkbugreport;

public abstract class Plugin {

    /**
     * Return the priority of this plugin.
     * Lower value means it will run sooner.
     * @return The priority of this plugin (lower = sooner)
     */
    public abstract int getPrio();

    /**
     * Parses the input and load into memory.
     * At this phase there might be some other plugins which
     * haven't been run yet. So if this plugin depends data from another
     * plugin, it might need to postpone some processing.
     * Also at this step the plugin must reset it's state.
     * @param br The reference to the current bugreport.
     */
    public abstract void load(Report br);

    /**
     * Save the collected info in the bugreport.
     * @param br The reference to the current bugreport.
     */
    public abstract void generate(Report br);
}
