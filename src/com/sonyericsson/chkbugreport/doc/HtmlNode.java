package com.sonyericsson.chkbugreport.doc;

import java.io.IOException;

public class HtmlNode extends DocNode {

    private String mTag;
    private String mId;
    private String mStyles;
    private String mTitle;

    public HtmlNode(String tag, DocNode parent) {
        this(tag, parent, null);
    }

    public HtmlNode(String tag) {
        this(tag, null, null);
    }

    public HtmlNode(String tag, String text) {
        this(tag, null, text);
    }

    public HtmlNode(String tag, DocNode parent, String text) {
        mTag = tag;
        if (parent != null) {
            parent.add(this);
        }
        if (text != null) {
            add(text);
        }
    }

    public HtmlNode setId(String string) {
        mId = string;
        return this;
    }

    public HtmlNode setTag(String tag) {
        mTag = tag;
        return this;
    }

    public HtmlNode setTitle(String title) {
        mTitle = title;
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
        if (mTitle != null) {
            r.print(" title=\"");
            r.print(mTitle);
            r.print("\"");
        }
        renderAttrs(r);
        r.print(">");
        super.render(r);
        r.print("</");
        r.print(mTag);
        r.print(">");
    }

    protected void renderAttrs(Renderer r) {
        // NOP
    }

}
