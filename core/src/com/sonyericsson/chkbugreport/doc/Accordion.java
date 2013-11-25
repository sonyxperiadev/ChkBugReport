package com.sonyericsson.chkbugreport.doc;

public class Accordion extends Block {

    /** Default settings */
    public static final int FLAG_NONE       = 0;
    /** User can collapse all items */
    public static final int FLAG_COLLAPSE   = 1;
    /** User can re-order items */
    public static final int FLAG_SORT       = 2;

    public Accordion(DocNode parent, int flags) {
        super(parent);
        String css = "auto-accordion";
        if (0 != (flags & FLAG_COLLAPSE)) {
            css += "-collapse";
        }
        if (0 != (flags & FLAG_SORT)) {
            css += "-sort";
        }
        addStyle(css);
    }

    public void add(DocNode header, DocNode body) {
        super.add(new HtmlNode("h3").add(header));
        super.add(new Block().add(body));
    }

    @Override
    public DocNode add(DocNode child) {
        throw new RuntimeException("Not allowed!");
    }

    @Override
    public DocNode add(String text) {
        throw new RuntimeException("Not allowed!");
    }

    @Override
    public DocNode addln(String text) {
        throw new RuntimeException("Not allowed!");
    }

    @Override
    public DocNode addBefore(DocNode child, DocNode ref) {
        throw new RuntimeException("Not allowed!");
    }

}
