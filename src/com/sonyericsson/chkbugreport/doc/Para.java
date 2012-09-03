package com.sonyericsson.chkbugreport.doc;



public class Para extends HtmlNode {

    public Para() {
        super("p");
    }

    public Para(DocNode parent) {
        super("p", parent);
    }

}
