/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
 * Copyright (C) 2012-2013 Sony Mobile Communications AB
 *
 * This file is part of ChkBugReport.
 *
 * ChkBugReport is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * ChkBugReport is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ChkBugReport.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.sonyericsson.chkbugreport.plugins;

import com.sonyericsson.chkbugreport.ImageCanvas;
import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.doc.Block;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.Img;

import java.io.File;
import java.io.IOException;

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
        ImageCanvas img = (ImageCanvas)br.getInfo("screenshot");
        if (img == null) return;

        String fn = "screenshot.png";
        try {
            img.writeTo(new File(br.getBaseDir() + fn));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Chapter ch = new Chapter(br.getContext(), "Screen shot");
        br.addChapter(ch);
        new Block(ch).add("Screenshot (" + img.getWidth() + "*" + img.getHeight() + "):");
        ch.add(new Img(fn));
    }

}
