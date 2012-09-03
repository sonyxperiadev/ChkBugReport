package com.sonyericsson.chkbugreport.doc;

import com.sonyericsson.chkbugreport.Util;

public class Anchor extends DocNode {

    private String mName;
    private String mFileName;

    public Anchor(String name) {
        mName = name;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    public String getFileName() {
        return mFileName;
    }

    @Override
    public void prepare(Renderer r) {
        mFileName = r.getFileName();
        Util.assertNotNull(mFileName);
    }

    @Override
    public void render(Renderer r) {
        r.println("<a name=\"" + mName + "\"/>");
    }

}
