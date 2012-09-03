package com.sonyericsson.chkbugreport.doc;

import java.io.IOException;

public class Link extends DocNode {

    private Anchor mAnchor;
    private String mAnchorText;
    private String mText;
    private String mTarget;

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
            r.print(mAnchor.getFileName() + "#" + mAnchor.getName());
        }
        r.print("\"");
        if (mTarget != null) {
            r.print(" target=\"");
            r.print(mTarget);
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

}
