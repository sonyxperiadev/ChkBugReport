package com.sonyericsson.chkbugreport.doc;

import java.io.IOException;

public class Header extends DocNode {

    private String mName;
    private int mLevel;

    public Header(String name) {
        mName = name;
    }

    public void setName(String name) {
        mName = name;
    }

    @Override
    public void prepare(Renderer r) {
        mLevel = r.getLevel();
        mLevel = Math.min(6, mLevel);
        mLevel = Math.max(1, mLevel);
    }

    @Override
    public void render(Renderer r) throws IOException {
        r.println("<h" + mLevel + ">" + mName + "</h" + mLevel + ">");
    }

}
