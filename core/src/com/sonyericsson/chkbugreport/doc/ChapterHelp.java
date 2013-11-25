package com.sonyericsson.chkbugreport.doc;


public class ChapterHelp {

    private Block mNode;

    public ChapterHelp(Chapter ch) {
        mNode = new Block();
        ch.addHelp(mNode);
    }

    public ChapterHelp addText(String text) {
        new Para(mNode).add(text);
        return this;
    }

    public ChapterHelp addHint(String text) {
        new Hint(mNode).add("NOTE: " + text);
        return this;
    }

    public ChapterHelp addSeeAlso(String link, String text) {
        new Block(mNode).addStyle("see-also").add("See also: ").add(new Link(link, text).setTarget("_blank"));
        return this;
    }

}
