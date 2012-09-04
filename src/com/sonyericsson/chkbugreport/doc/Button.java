package com.sonyericsson.chkbugreport.doc;

public class Button extends HtmlNode {

    private String mOnClick;

    public Button(String text, String onClick) {
        super("button", text);
        mOnClick = onClick;
    }

    @Override
    protected void renderAttrs(Renderer r) {
        super.renderAttrs(r);
        if (mOnClick != null) {
            r.print(" onClick=\"");
            r.print(mOnClick);
            r.print("\"");
        }
    }

}
