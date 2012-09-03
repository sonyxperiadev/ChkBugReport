package com.sonyericsson.chkbugreport.doc;

import com.sonyericsson.chkbugreport.Util;

import java.io.IOException;

public class ShadedValue extends DocNode {

    private long mValue;

    public ShadedValue(long value) {
        mValue = value;
    }

    @Override
    public String getText() {
        return Long.toString(mValue);
    }

    @Override
    public void render(Renderer r) throws IOException {
        r.print(Util.shadeValue(mValue));
    }

}
