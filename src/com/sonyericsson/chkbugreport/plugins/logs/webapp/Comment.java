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

import com.sonyericsson.chkbugreport.util.SavedField;
import com.sonyericsson.chkbugreport.util.SavedField.Type;

public class Comment {

    @SavedField(type = Type.ID)
    private int mId;
    @SavedField(type = Type.INT)
    private long mLogLineId;
    @SavedField(type = Type.VARCHAR)
    private String mComment;

    public Comment(long logLineId, String comment) {
        mLogLineId = logLineId;
        mComment = comment;
    }

    public int getId() {
        return mId;
    }

    public long getLogLineId() {
        return mLogLineId;
    }

    public void setLogLineId(long logLineId) {
        this.mLogLineId = logLineId;
    }

    public String getComment() {
        return mComment;
    }

    public void setComment(String comment) {
        this.mComment = comment;
    }


}
