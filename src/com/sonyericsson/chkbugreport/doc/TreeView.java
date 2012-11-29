package com.sonyericsson.chkbugreport.doc;

import com.sonyericsson.chkbugreport.util.HtmlUtil;

import java.io.IOException;

public class TreeView extends DocNode {

    private String mLine;
    private int mLevel;

    public TreeView(String line, int level) {
        mLine = line;
        mLevel = level;
    }

    @Override
    public void render(Renderer r) throws IOException {
        if (mLevel == 0) {
            r.println("<div class=\"tree\">");
        }
        if (mLine != null) {
            r.println("<span>" + HtmlUtil.escape(mLine) + "</span>");
        }
        int cnt = getChildCount();
        if (cnt > 0) {
            r.println("<ul>");
            for (int i = 0; i < cnt; i++) {
                r.println("<li>");
                getChild(i).render(r);
                r.println("</li>");
            }
            r.println("</ul>");
        }
        if (mLevel == 0) {
            r.println("</div>");
        }
    }

}
