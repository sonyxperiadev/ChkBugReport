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
package com.sonyericsson.chkbugreport.doc;

import java.io.IOException;

/**
 * Special node which prints a stack trace as a comment.
 * It's useful to find the location in source which generated the surrounding text.
 */
public class InternalRef extends DocNode {

    private String mComment;

    public InternalRef() {
        Throwable ref = new Throwable();
        ref.fillInStackTrace();
        StringBuilder sb = new StringBuilder();
        sb.append("<!--\n");
        for (StackTraceElement item : ref.getStackTrace()) {
            sb.append(item.toString() + "\n");
        }
        sb.append("-->\n");
        mComment = sb.toString();
    }

    @Override
    public void render(Renderer r) throws IOException {
        r.print(mComment);
    }

}
