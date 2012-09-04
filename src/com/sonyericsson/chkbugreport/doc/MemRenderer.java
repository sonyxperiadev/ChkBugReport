package com.sonyericsson.chkbugreport.doc;

import com.sonyericsson.chkbugreport.Module;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;


public class MemRenderer implements Renderer {

    private PrintStream mOut = null;
    private ByteArrayOutputStream mData = new ByteArrayOutputStream();
    private Module mMod;

    public MemRenderer(Module mod) {
        mMod = mod;
    }

    @Override
    public MemRenderer addLevel(Chapter ch) {
        return null; // Not supported
    }

    @Override
    public int getLevel() {
        return 1;
    }

    @Override
    public void begin() throws FileNotFoundException {
        mOut = new PrintStream(mData);
    }

    @Override
    public void end() {
        mOut.close();
    }

    @Override
    public void print(String string) {
        mOut.print(string);
    }

    @Override
    public void println(String string) {
        mOut.println(string);
    }

    @Override
    public void print(long v) {
        mOut.print(v);
    }

    @Override
    public void print(char c) {
        mOut.print(c);
    }

    @Override
    public String getFileName() {
        return null; // Not supported
    }

    @Override
    public MemRenderer getParent() {
        return null; // Not supported
    }

    @Override
    public boolean isStandalone() {
        return true; // Not supported
    }

    @Override
    public Module getModule() {
        return mMod;
    }

    @Override
    public Chapter getChapter() {
        return null; // Not supported
    }

    public byte[] getData() {
        return mData.toByteArray();
    }

}
