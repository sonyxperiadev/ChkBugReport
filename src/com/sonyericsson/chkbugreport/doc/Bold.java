package com.sonyericsson.chkbugreport.doc;

public class Bold extends HtmlNode {

    public Bold() {
        super("b");
    }

    public Bold(DocNode parent) {
        super("b", parent);
    }

    public Bold(String text) {
        super("b", text);
    }

    public Bold(DocNode parent, String text) {
        super("b", parent, text);
    }

}
