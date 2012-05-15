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

import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

public class Chapter extends Lines {

    private int mId;
    private int mLevel = 0;
    private Vector<Chapter> mChildren = new Vector<Chapter>();
    private Chapter mParent = null;

    public Chapter(Report report, String name) {
        super(name);
        mId = report.allocChapterId();
    }

    public void addChapter(Chapter child) {
        child.setParent(this);
        mChildren.add(child);
    }

    public void removeChapter(Chapter child) {
        child.setParent(null);
        mChildren.remove(child);
    }

    public void insertChapter(int idx, Chapter child) {
        child.setParent(this);
        mChildren.add(idx, child);
    }

    private void setParent(Chapter parent) {
        mParent = parent;
        if (parent != null) {
            mLevel = parent.mLevel + 1;
        }
        for (Chapter child : mChildren) {
            child.setParent(this);
        }
    }

    public Chapter getParent() {
        return mParent;
    }

    public int getId() {
        return mId;
    }

    public int getChildCount() {
        return mChildren.size();
    }

    public Chapter getChild(int idx) {
        return mChildren.get(idx);
    }

    public String getAnchor() {
        if (mParent == null) {
            return String.format("ch%03d", mId);
        } else {
            return String.format("%s_%03d", mParent.getAnchor(), mId);
        }
    }

    public String getFullName() {
        if (mParent != null) {
            String ret = mParent.getFullName();
            if (ret != null) {
                return ret + "/" + getName();
            }
        }
        return getName();
    }

    public int getLevel() {
        return mLevel;
    }

    public Chapter getTopLevelChapter() {
        if (mLevel == 1) return this;
        if (mParent == null) return null;
        return mParent.getTopLevelChapter();
    }

    public boolean isEmpty() {
        return getChildCount() == 0 && getLineCount() == 0;
    }

    public void sort(Comparator<Chapter> comparator) {
        Collections.sort(mChildren, comparator);
    }

}
