package com.sonyericsson.chkbugreport.doc;

import com.sonyericsson.chkbugreport.Module;

import java.io.FileNotFoundException;

public interface Renderer {

    public Renderer addLevel(Chapter ch);

    public int getLevel();

    public void begin() throws FileNotFoundException;

    public void end();

    public void print(String string);

    public void println(String string);

    public void print(long v);

    public String getFileName();

    public Renderer getParent();

    public boolean isStandalone();

    public Module getModule();

    public Chapter getChapter();

}
