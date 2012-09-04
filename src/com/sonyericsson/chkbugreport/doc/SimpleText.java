package com.sonyericsson.chkbugreport.doc;


public class SimpleText extends DocNode {

    private String mText;

    public SimpleText(String text) {
        mText = text;
    }

    @Override
    public void render(Renderer r) {
        r.print(mText);
    }

}
