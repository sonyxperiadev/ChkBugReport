package com.sonyericsson.chkbugreport.doc;

import com.sonyericsson.chkbugreport.Util;

import java.io.IOException;

public class Link extends DocNode {

    private Anchor mAnchor;
    private String mAnchorText;
    private String mText;
    private String mTarget;
    private String mTitle;

    public Link(Anchor anchor, String text) {
        mAnchor = anchor;
        mAnchorText = null;
        mText = text;
    }

    public Link(String anchor, String text) {
        mAnchor = null;
        mAnchorText = anchor;
        mText = text;
    }

    public Link setTarget(String target) {
        mTarget = target;
        return this;
    }

    @Override
    public void render(Renderer r) throws IOException {
        r.print("<a href=\"");
        if (mAnchor == null) {
            r.print(mAnchorText);
        } else {
            String fn = mAnchor.getFileName();
            String name = mAnchor.getName();
            Util.assertNotNull(fn);
            if (name == null) {
                r.print(fn);
            } else {
                r.print(fn + "#" + name);
            }
        }
        r.print("\"");
        if (mTarget != null) {
            r.print(" target=\"");
            r.print(mTarget);
            r.print("\"");
        }
        if (mTitle != null) {
            r.print(" title=\"");
            r.print(mTitle);
            r.print("\"");
        }
        r.print(">");
        if (mText == null) {
            super.render(r);
        } else {
            r.print(mText);
        }
        r.print("</a>");
    }

    public void setTitle(String string) {
        mTitle = string;
    }

}
