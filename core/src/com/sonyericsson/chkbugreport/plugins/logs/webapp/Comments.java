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
import com.sonyericsson.chkbugreport.plugins.logs.LogLine;
import com.sonyericsson.chkbugreport.util.HtmlUtil;
import com.sonyericsson.chkbugreport.util.SaveFile;
import com.sonyericsson.chkbugreport.util.SavedData;
import com.sonyericsson.chkbugreport.webserver.JSON;
import com.sonyericsson.chkbugreport.webserver.engine.HTTPRequest;
import com.sonyericsson.chkbugreport.webserver.engine.HTTPResponse;

public class Comments extends SavedData<Comment> {

    public Comments(SaveFile saveFile, String prefix) {
        super(saveFile, prefix + "_comments");
        load();
    }

    @Override
    protected Comment createItem() {
        return new Comment(0, null);
    }

    public void collectLogs(LogLine ll, DocNode log) {
        for (Comment c : getData()) {
            if (c.getLogLineId() == ll.id) {
                new Block(log).addStyle("log-comment").setId("l" + ll.id + "," + c.getId())
                    .add(c.getComment());
            }
        }
    }

    private long extractLineId(String id) {
        if (id == null) return -1;
        if (!id.startsWith("l")) return -1;
        id = id.substring(1);
        int idx = id.indexOf(',');
        if (idx < 0) {
            return Long.parseLong(id);
        } else {
            return Long.parseLong(id.substring(0, idx));
        }
    }

    private int extractCommentId(String id) {
        if (id == null) return -1;
        if (!id.startsWith("l")) return -1;
        id = id.substring(1);
        int idx = id.indexOf(',');
        if (idx < 0) {
            return -1;
        } else {
            return Integer.parseInt(id.substring(idx + 1));
        }
    }

    public Comment findById(int id) {
        for (Comment c : getData()) {
            if (id == c.getId()) {
                return c;
            }
        }
        return null;
    }

    public void addComment(Module mod, HTTPRequest req, HTTPResponse resp) {
        JSON json = new JSON();
        String comment = req.getArg("comment");
        long logLineId = extractLineId(req.getArg("id"));
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
            json.add("id", c.getId());
        }
        json.writeTo(resp);
    }

    public void updateComment(Module mod, HTTPRequest req, HTTPResponse resp) {
        JSON json = new JSON();
        String comment = req.getArg("comment");
        int commentId = extractCommentId(req.getArg("id"));
        Comment c = findById(commentId);
        if (comment == null || comment.length() == 0) {
            json.add("err", 400);
            json.add("msg", "Cannot add empty text!");
        } else if (c == null) {
            json.add("err", 400);
            json.add("msg", "Cannot find comment to update!");
        } else {
            c.setComment(comment);
            update(c);
            json.add("err", 200);
            json.add("msg", "Comment updated!");
            json.add("comment", HtmlUtil.escape(comment));
            json.add("id", c.getId());
        }
        json.writeTo(resp);
    }

    public void deleteComment(Module mod, HTTPRequest req, HTTPResponse resp) {
        JSON json = new JSON();
        int commentId = extractCommentId(req.getArg("id"));
        Comment c = findById(commentId);
        if (c == null) {
            json.add("err", 400);
            json.add("msg", "Cannot find comment to delete!");
        } else {
            delete(c);
            json.add("err", 200);
            json.add("msg", "Comment deleted!");
        }
        json.writeTo(resp);
    }

}
