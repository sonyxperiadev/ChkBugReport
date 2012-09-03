package com.sonyericsson.chkbugreport.doc;

import java.io.IOException;

public class HtmlNode extends DocNode {

    private String mTag;
    private String mId;
    private String mStyles;

    public HtmlNode(String tag, DocNode parent) {
        mTag = tag;
        if (parent != null) {
            parent.add(this);
        }
    }

    public HtmlNode(String tag) {
        mTag = tag;
    }

    public HtmlNode setId(String string) {
        mId = string;
        return this;
    }

    public HtmlNode setTag(String tag) {
        mTag = tag;
        return this;
    }

    public HtmlNode addStyle(String style) {
        if (mStyles == null) {
            mStyles = style;
        } else {
            mStyles = mStyles + " " + style;
        }
        return this;
    }

    @Override
    public void render(Renderer r) throws IOException {
        r.print("<");
        r.print(mTag);
        if (mId != null) {
            r.print(" id=\"");
            r.print(mId);
            r.print("\"");
        }
        if (mStyles != null) {
            r.print(" class=\"");
            r.print(mStyles);
            r.print("\"");
        }
        r.print(">");
        super.render(r);
        r.print("</");
        r.print(mTag);
        r.print(">");
    }

}
