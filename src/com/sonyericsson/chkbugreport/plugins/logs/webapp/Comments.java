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

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.doc.Block;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.plugins.logs.LogLineBase;
import com.sonyericsson.chkbugreport.util.HtmlUtil;
import com.sonyericsson.chkbugreport.util.db.DbBackedData;
import com.sonyericsson.chkbugreport.webserver.JSON;
import com.sonyericsson.chkbugreport.webserver.engine.HTTPRequest;
import com.sonyericsson.chkbugreport.webserver.engine.HTTPResponse;

import java.sql.Connection;

public class Comments extends DbBackedData<Comment> {

    public Comments(Connection conn, String prefix) {
        super(conn, prefix + "_comments");
        load();
    }

    @Override
    protected Comment createItem() {
        return new Comment(0, null);
    }

    public void collectLogs(LogLineBase ll, DocNode log) {
        for (Comment c : getData()) {
            if (c.getLogLineId() == ll.id) {
                new Block(log).addStyle("log-comment").setId("l" + ll.id).add(c.getComment());
            }
        }
    }

    public void addComment(Module mod, HTTPRequest req, HTTPResponse resp) {
        JSON json = new JSON();
        String lid = req.getArg("id");
        String comment = req.getArg("comment");
        long logLineId = (lid != null && lid.startsWith("l")) ? Long.parseLong(lid.substring(1)) : -1;
        if (comment == null || comment.length() == 0) {
            json.add("err", 400);
            json.add("msg", "Cannot add empty text!");
        } else if (logLineId < 0) {
            json.add("err", 400);
            json.add("msg", "Not a log line!");
        } else {
            Comment c = new Comment(logLineId, comment);
            add(c);
            json.add("err", 200);
            json.add("msg", "Comment created!");
            json.add("comment", HtmlUtil.escape(comment));
        }
        json.writeTo(resp);
    }


}
