package com.sonyericsson.chkbugreport.plugins.stacktrace;

public class StackTraceItem {

    public static final String STYLE_ERR = "stacktrace-err";
    public static final String STYLE_BUSY = "stacktrace-busy";

    private String mMethod;
    private String mFileName;
    private int mLine;
    private String mStyle = "";

    public StackTraceItem(String method, String fileName, int line) {
        mMethod = method;
        mFileName = fileName;
        mLine = line;
    }

    public String getStyle() {
        return mStyle;
    }

    public void setStyle(String style) {
        mStyle = style;
    }

    public String getMethod() {
        return mMethod;
    }

    public String getFileName() {
        return mFileName;
    }

    public int getLine() {
        return mLine;
    }

}
