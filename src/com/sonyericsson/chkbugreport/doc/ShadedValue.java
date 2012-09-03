package com.sonyericsson.chkbugreport.doc;

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
        if (mValue < 1000) {
            r.print("<span class=\"kb\">");
            r.print(mValue);
            r.print("</span>");
        } else {
            long mb = mValue / 1000;
            long kb = mValue % 1000;
            if (mb != 0) {
                r.print(mb);
            }
            r.print("<span class=\"kb\">");
            if (kb < 100) {
                r.print('0');
            }
            if (kb < 10) {
                r.print('0');
            }
            r.print(kb);
            r.print("</span>");
        }
    }


}
