package com.sonyericsson.chkbugreport.plugins;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.doc.Block;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.Img;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class ScreenShotPlugin extends Plugin {

    @Override
    public int getPrio() {
        return 5;
    }

    @Override
    public void reset() {
        // TODO Auto-generated method stub

    }

    @Override
    public void load(Module br) {
        // NOP
    }

    @Override
    public void generate(Module br) {
        BufferedImage img = (BufferedImage)br.getMetaInfo("screenshot");
        if (img == null) return;

        String fn = "screenshot.png";
        try {
            ImageIO.write(img, "png", new File(br.getBaseDir() + fn));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Chapter ch = new Chapter(br, "Screen shot");
        br.addChapter(ch);
        new Block(ch).add("Screenshot (" + img.getWidth() + "*" + img.getHeight() + "):");
        ch.add(new Img(fn));
    }

}
