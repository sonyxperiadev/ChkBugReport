package com.sonyericsson.chkbugreport.doc;

import java.io.IOException;

public class List extends DocNode {

    public static final int TYPE_UNORDERED = 0;
    public static final int TYPE_ORDERED = 1;

    private int mType;

    public List() {
        this(TYPE_UNORDERED);
    }

    public List(int type) {
        mType = type;
    }

    @Override
    public void render(Renderer r) throws IOException {
        r.println(mType == TYPE_UNORDERED ? "<ul>" : "<ol>");
        int cnt = getChildCount();
        for (int i = 0; i < cnt; i++) {
            r.println("<li>");
            getChild(i).render(r);
            r.println("</li>");
        }
        r.println(mType == TYPE_UNORDERED ? "</ul>" : "</ol>");
    }


}
