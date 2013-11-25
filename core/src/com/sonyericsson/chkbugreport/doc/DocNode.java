/*
 * Copyright (C) 2012-2013 Sony Mobile Communications AB
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
import java.util.Vector;

/**
 * The DocNode class represents a piece of text or data from the generated report.
 * It can contain child nodes (for example a list can contain the list items as child nodes).
 * Currently it needs to support html rendering, but in the future it could support other formats
 * as well.
 */
public class DocNode {

    private Vector<DocNode> mChildren;
    private DocNode mParent = null;

    public DocNode() {
    }

    public DocNode(DocNode parent) {
        if (parent != null) {
            parent.add(this);
        }
    }

    public int getChildCount() {
        if (mChildren == null) {
            return 0;
        }
        return mChildren.size();
    }

    public DocNode getChild(int idx) {
        if (mChildren == null) {
            return null;
        }
        return mChildren.get(idx);
    }

    public DocNode getParent() {
        return mParent;
    }

    public DocNode add(DocNode child) {
        if (mChildren == null) {
            mChildren = new Vector<DocNode>();
        }
        mChildren.add(child);
        child.mParent = this;
        return this;
    }

    public DocNode add(String text) {
        return add(new SimpleText(text));
    }

    public DocNode addln(String text) {
        return add(new SimpleText(text + '\n'));
    }

    public DocNode addBefore(DocNode child, DocNode ref) {
        if (mChildren != null) {
            for (int i = 0; i < mChildren.size(); i++) {
                if (mChildren.get(i) == ref) {
                    mChildren.insertElementAt(child, i);
                    child.mParent = this;
                    return this;
                }
            }
        }
        return add(child);
    }

    public void remove(DocNode child) {
        if (mChildren.remove(child)) {
            if (mChildren.isEmpty()) {
                mChildren = null;
            }
            child.mParent = null;
        }
    }

    /**
     * This method is called when all the data is collected/generated and
     * it's time to render the content. This method is used to calculate the
     * generate file names and links.
     * @param r The Renderer
     */
    public void prepare(Renderer r) {
        if (mChildren == null) {
            return;
        }
        for (DocNode child : mChildren) {
            child.prepare(r);
        }
    }

    /**
     * Renders this node and all the child nodes
     * @param r The Renderer
     */
    public void render(Renderer r) throws IOException {
        renderChildren(r);
    }

    /**
     * Render all the child nodes
     * @param r The Renderer
     */
    protected void renderChildren(Renderer r) throws IOException {
        if (mChildren == null) {
            return;
        }
        for (DocNode child : mChildren) {
            child.render(r);
        }
    }

    public String getText() {
        if (mChildren == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        for (DocNode child : mChildren) {
            sb.append(child.getText());
        }
        return sb.toString();
    }

    public boolean isEmpty() {
        if (mChildren == null) {
            return true;
        }
        return mChildren.isEmpty();
    }

    public DocNode copy() {
        throw new RuntimeException("Not supported for this DocNode type (" + getClass().getSimpleName() + ")!");
    }

}
