package com.sonyericsson.chkbugreport.doc;

import com.sonyericsson.chkbugreport.Util;

public class Anchor extends DocNode {

    private String mName;
    private String mFileName;
    private String mPrefix;

    public Anchor(String name) {
        mName = name;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getName() {
        return mPrefix + mName;
    }

    public String getFileName() {
        return mFileName;
    }

    @Override
    public void prepare(Renderer r) {
        mFileName = r.getFileName();
        mPrefix = "";
        while (mFileName == null && r != null) {
            mPrefix = r.getChapter().getId() + "$";
            r = r.getParent();
            mFileName = r.getFileName();
        }
        Util.assertNotNull(mFileName);
    }

    @Override
    public void render(Renderer r) {
        r.println("<a name=\"" + mPrefix + mName + "\"></a>");
    }

}
