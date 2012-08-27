package com.sonyericsson.chkbugreport.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Vector;

import com.sonyericsson.chkbugreport.Lines;
import com.sonyericsson.chkbugreport.Report;
import com.sonyericsson.chkbugreport.Util;

public class TableGen {

    public static final int FLAG_NONE           = 0x0000;
    public static final int FLAG_SORT           = 0x0001;
    public static final int FLAG_ALIGN_RIGHT    = 0x0100;

    private int mTableFlags;
    private Lines mCh;
    private Vector<Column> mColumns = new Vector<TableGen.Column>();
    private int mColIdx;
    private String mNextRowStyle;
    private boolean mEmpty = true;
    private FileOutputStream mCsvF;
    private PrintStream mCsvOut;
    private int mCsvCol;

    private class Column {
        private String title;
        private String hint;
        private int flag;
        public Column(String title, String hint, int flag) {
            this.title = title;
            this.hint = hint;
            this.flag = flag;
        }
    }

    public TableGen(Lines ch, int flag) {
        mCh = ch;
        mTableFlags = flag;
    }

    public void setCSVOutput(Report br, String csv) {
        if (csv == null) return;
        String fn = br.getRelRawDir() + csv + ".csv";
        try {
            mCsvF = new FileOutputStream(br.getBaseDir() + fn);
            mCsvOut = new PrintStream(mCsvF);
            mCh.addLine("<div class=\"hint\">(Hint: a CSV format version is saved as: <a href=\"" + fn + "\">" + fn + "</a>)</div>");
        } catch (IOException e) {
            br.printErr(4, "Failed creating CSV file `" + fn + "': " + e);
            mCsvF = null;
            mCsvOut = null;
        }
    }

    public void addColumn(String title, int flag) {
        addColumn(title, null, flag);
    }

    public void addColumn(String title, String hint, int flag) {
        mColumns.add(new Column(title, hint, flag));
        csvField(title);
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
            String title = "";
            if (c.hint != null) {
                title = " title=\"" + c.hint + "\"";
            }
            mCh.addLine("<th class=\"" + cls + "\"" + title + ">" + c.title + "</td>");
        }
        mCh.addLine("</tr>");
        mCh.addLine("</thead>");
        mCh.addLine("<tbody>");
        mColIdx = 0;
        csvEOL();
    }

    public void addData(String text) {
        addData(null, text, FLAG_NONE);
    }

    public void addData(int value) {
        addData(null, Integer.toString(value), FLAG_NONE);
    }

    public void addData(long value) {
        addData(null, Long.toString(value), FLAG_NONE);
    }

    public void addData(String link, String text, int flag) {
        addData(link, null, text, flag);
    }

    public void addData(String link, String hint, String text, int flag) {
        csvField(text);
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
        sb.append("<td class=\"");
        sb.append(cls);
        sb.append("\"");
        if (hint != null) {
            sb.append(" title=\"");
            sb.append(hint);
            sb.append("\"");
        }
        sb.append(">");
        if (link != null) {
            sb.append("<a href=\"");
            sb.append(link);
            sb.append("\"");
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
            csvEOL();
        }
        mEmpty = false;
    }

    public void addSeparator() {
        mCh.addLine("</tbody>");
        mCh.addLine("<tbody>");
    }

    public void end() {
        mCh.addLine("</tbody>");
        mCh.addLine("<table>");
        csvEnd();
    }

    public void setNextRowStyle(String style) {
        mNextRowStyle = style;
    }

    public boolean isEmpty() {
        return mEmpty;
    }

    private void csvField(String text) {
        if (mCsvOut == null) return;
        text = Util.stripHtml(text);
        if (mCsvCol > 0) {
            mCsvOut.print(',');
        }
        mCsvOut.print('"');
        mCsvOut.print(text.replace("\"", "\"\""));
        mCsvOut.print('"');
        mCsvCol++;
    }

    private void csvEOL() {
        if (mCsvOut == null) return;
        mCsvOut.print("\r\n");
        mCsvCol = 0;
    }

    private void csvEnd() {
        if (mCsvOut == null) return;
        try {
            mCsvOut.close();
            mCsvF.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
