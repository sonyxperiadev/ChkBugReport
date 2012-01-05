package com.sonyericsson.chkbugreport.util;

import java.util.Vector;

import com.sonyericsson.chkbugreport.Lines;

public class TableGen {

    public static final int FLAG_NONE           = 0x0000;
    public static final int FLAG_SORT           = 0x0001;
    public static final int FLAG_ALIGN_RIGHT    = 0x0100;

    private int mTableFlags;
    private Lines mCh;
    private Vector<Column> mColumns = new Vector<TableGen.Column>();
    private int mColIdx;
    private String mNextRowStyle;

    private class Column {
        private String title;
        private int flag;
        public Column(String title, int flag) {
            this.title = title;
            this.flag = flag;
        }
    }

    public TableGen(Lines ch, int flag) {
        mCh = ch;
        mTableFlags = flag;
    }

    public void addColumn(String title, int flag) {
        mColumns.add(new Column(title, flag));
    }

    public void begin() {
        if (0 != (mTableFlags & FLAG_SORT)) {
            mCh.addLine("<div class=\"hint\">(Hint: click on the headers to sort the data. Shift+click to sort on multiple columns.)</div>");
            mCh.addLine("<table class=\"tablesorter\">");
        } else {
            mCh.addLine("<table class=\"\">");
        }
        mCh.addLine("<thead>");
        mCh.addLine("<tr>");
        for (Column c : mColumns) {
            String cls = "";
            if (0 != (c.flag & FLAG_ALIGN_RIGHT)) {
                cls = " right";
            }
            mCh.addLine("<th class=\"" + cls + "\">" + c.title + "</td>");
        }
        mCh.addLine("</tr>");
        mCh.addLine("</thead>");
        mCh.addLine("<tbody>");
        mColIdx = 0;
    }

    public void addData(String text) {
        addData(null, text, FLAG_NONE);
    }

    public void addData(String link, String text, int flag) {
        addData(link, null, text, flag);
    }

    public void addData(String link, String hint, String text, int flag) {
        if (mColIdx == 0) {
            if (mNextRowStyle != null) {
                mCh.addLine("<tr class=\"" + mNextRowStyle + "\">");
                mNextRowStyle = null;
            } else {
                mCh.addLine("<tr>");
            }
        }
        StringBuffer sb = new StringBuffer();
        String cls = "";
        Column c = mColumns.get(mColIdx);
        if (0 != (c.flag & FLAG_ALIGN_RIGHT)) {
            cls = " right";
        }
        sb.append("<td class=\"" + cls + "\">");
        if (link != null) {
            sb.append("<a href=\"");
            sb.append(link);
            sb.append("\"");
            if (hint != null) {
                sb.append(" title=\"");
                sb.append(hint);
                sb.append("\"");
            }
            sb.append(">");
        }
        if (text != null) {
            sb.append(text);
        }
        if (link != null) {
            sb.append("</a>");
        }
        sb.append("</td>");
        mCh.addLine(sb.toString());
        mColIdx = (mColIdx + 1) % mColumns.size();
        if (mColIdx == 0) {
            mCh.addLine("</tr>");
        }
    }

    public void end() {
        mCh.addLine("</tbody>");
        mCh.addLine("<table>");
    }

    public void setNextRowStyle(String style) {
        mNextRowStyle = style;
    }

}
