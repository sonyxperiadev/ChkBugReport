package com.sonyericsson.chkbugreport.doc;

import java.io.IOException;

public class Img extends DocNode {

    private String mFn;

    public Img(String fn) {
        mFn = fn;
    }

    @Override
    public void render(Renderer r) throws IOException {
        r.println("<img src=\"" + mFn + "\"/>");
    }

}
