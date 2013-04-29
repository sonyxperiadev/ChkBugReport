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
package com.sonyericsson.chkbugreport.plugins.logs.webapp;

public class Filter {

    public enum Action {
        HIDE,
        SHOW,
        COLOR,
    };

    private String mTag;
    private String mMsg;
    private String mLine;
    private Action mAction;
    private int mActionArg;

    public Filter(String tag, String msg, String line, Action action, int actionArg) {
        mTag = tag;
        mMsg = msg;
        mLine = line;
        mAction = action;
        mActionArg = actionArg;
    }

    public String getTag() {
        return mTag;
    }

    public String getMsg() {
        return mMsg;
    }

    public String getLine() {
        return mLine;
    }

    public Action getAction() {
        return mAction;
    }

    public int getActionArg() {
        return mActionArg;
    }

}
