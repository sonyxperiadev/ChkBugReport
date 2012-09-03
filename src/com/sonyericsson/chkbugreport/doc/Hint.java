package com.sonyericsson.chkbugreport.doc;

import java.io.IOException;

public class Hint extends DocNode {

    @Override
    public void render(Renderer r) throws IOException {
        r.println("<div class=\"hint\">(Hint: ");
        super.render(r);
        r.println(")</div>");
    }

}
