package com.sonyericsson.chkbugreport.doc;


public class Block extends HtmlNode {

    public Block(DocNode parent) {
        super("div", parent);
    }

    public Block() {
        super("div", null);
    }


}
