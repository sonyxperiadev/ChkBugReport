package com.sonyericsson.chkbugreport.doc;

import java.io.IOException;

public class Icon extends DocNode {

    public static final int TYPE_FILE = 0;
    public static final int TYPE_SMALL = 1;
    public static final int TYPE_BIG = 2;

    private String mName;
    private int mType;

    public Icon(int type , String name) {
        mType = type;
        mName = name;
    }

    @Override
    public void render(Renderer r) throws IOException {
        if (mType == TYPE_SMALL) {
            r.println("<div class=\"builtin-icon builtin-icon-" + mName + "\"></div>");
        } else if (mType == TYPE_BIG) {
            r.println("<div class=\"builtin-big-icon builtin-icon-" + mName + "\"></div>");
        } else {
            r.print("<img src=\"" + mName + "\"/>");
        }
    }

}
