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

/**
 * Interface used to catch output from ChkBugReport execution.
 */
public interface OutputListener {

    /** Constant used for log messages targeted to the standard output */
    public static final int TYPE_OUT = 0;
    /** Constant used for log messages targeted to the error output */
    public static final int TYPE_ERR = 1;

    /**
     * Called when a new log message should be printed.
     *
     * <p>The level should specify the detail level of this message, and not the priority.
     * Explanation:</p>
     *
     * <ul>
     * <li>level 1 means that this message can be shown only once per runtime.
     * In other words this shows which main step of the processing is being executed.</li>
     * <li>level 2 can be shown once per plugin and there can be only one of these
     * when executing a certain step of the plugin. In other words these are intended
     * to show which plugin is executing right now.</li>
     * <li>level 3 can be shown once per plugin run. These are intended to show which step
     * of the plugin is being executed.</li>
     * <li>level 4 can be shown several times per plugin run. For example if the plugin
     * processes log files, these can be parsing errors, since several lines in the log
     * can have wrong format.</li>
     * <li>level 5 is used as a generic low-prio message</li>
     *
     * @param level The detail level of the message
     * @param type The output stream of the message
     * @param msg The message body
     */
    public void onPrint(int level, int type, String msg);
}

