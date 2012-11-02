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
        add("Note: changes are not saved!");
    }

}
