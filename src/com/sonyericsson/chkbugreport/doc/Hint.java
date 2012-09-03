package com.sonyericsson.chkbugreport.doc;

import java.io.IOException;

public class Hint extends DocNode {

    public Hint() {
    }

    public Hint(DocNode parent) {
        if (parent != null) {
            parent.add(this);
        }
    }

    @Override
    public void render(Renderer r) throws IOException {
        r.println("<div class=\"hint\">(Hint: ");
        super.render(r);
        r.println(")</div>");
    }

}
