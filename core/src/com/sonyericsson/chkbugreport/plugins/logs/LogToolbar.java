/*
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
package com.sonyericsson.chkbugreport.plugins.logs;

import com.sonyericsson.chkbugreport.doc.Block;
import com.sonyericsson.chkbugreport.doc.Button;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.doc.HtmlNode;

/**
 * This Document node will add a toolbar for a log, so the user
 * can hide/show/mark lines, for easier reading.
 */
public class LogToolbar extends Block {

    public LogToolbar(DocNode node) {
        super(node);
        addStyle("log-toolbar");
        add("RegExp:");
        add(new HtmlNode("input").setId("regexp"));
        add(new Button("Hide", "javascript:ltbProcessLines(function(l){l.hide();})"));
        add(new Button("Show", "javascript:ltbProcessLines(function(l){l.show();})"));
        add(new Button("Mark", "javascript:ltbProcessLines(function(l){l.addClass('log-mark');})"));
        add(new Button("Unmark", "javascript:ltbProcessLines(function(l){l.removeClass('log-mark');})"));
        add(new Button("Line wrap", "javascript:ltbToggleLineWrap()"));
        add("Note: changes are not saved!");
    }

}
