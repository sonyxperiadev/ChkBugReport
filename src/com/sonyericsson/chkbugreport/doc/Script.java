package com.sonyericsson.chkbugreport.doc;

import com.sonyericsson.chkbugreport.util.LineReader;

import java.io.IOException;
import java.io.InputStream;

public class Script extends DocNode {

    private String mResName;
    private StringBuilder mContent;

    public Script(DocNode parent) {
        super(parent);
        mContent = new StringBuilder();
    }

    public Script(DocNode parent, String jsResName) {
        super(parent);
        mResName = jsResName;
    }

    public void println(String line) {
        mContent.append(line);
        mContent.append('\n');
    }

    @Override
    public void render(Renderer r) throws IOException {
        r.println("<script type=\"text/javascript\">");
        if (mResName != null) {
            dumpResource(r, mResName);
        } else if (mContent != null) {
            r.print(mContent.toString());
        }
        r.println("</script>");
    }

    private void dumpResource(Renderer r, String resName) {
        try {
            InputStream is = getClass().getResourceAsStream("/" + resName);
            if (is != null) {
                LineReader br = new LineReader(is);
                String line;
                while (null != (line = br.readLine())) {
                    r.println(line);
                }
                is.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
