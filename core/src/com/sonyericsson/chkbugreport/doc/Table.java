/*
 * Copyright (C) 2012 Sony Mobile Communications AB
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
package com.sonyericsson.chkbugreport.doc;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.util.Util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

// FIXME: the data should be exported into CSV/DB in "render" step,
// when the links are correct
public class Table extends DocNode {

    public static final int FLAG_NONE           = 0x0000;
    public static final int FLAG_SORT           = 0x0001;
    public static final int FLAG_COL_RESIZE     = 0x0002;
    public static final int FLAG_DND            = 0x0004;
    public static final int FLAG_ALIGN_RIGHT    = 0x0100;

    /* Table information */
    private int mTableFlags;
    private Vector<Column> mColumns = new Vector<Table.Column>();
    private boolean mEmpty = true;

    /* Current state (while processing data) */
    private int mColIdx;
    private String mNextRowStyle;
    private String mNextRowId;
    private TableBody mBody;

    /* For CSV saving */
    private FileOutputStream mCsvF;
    private PrintStream mCsvOut;
    private int mCsvCol;

    /* For DB import */
    private Connection mConn;
    private String mTable;
    private PreparedStatement mSqlInsert;
    private TableRow mRow;
    private String mStyles = "";

    private class Column {

        private String title;
        private String hint;
        private String dbSpec;
        private int flag;

        public Column(String title, String hint, String dbSpec, int flag) {
            this.title = title;
            this.hint = hint;
            this.dbSpec = dbSpec;
            this.flag = flag;
        }
    }

    public class TableHeader extends DocNode {

        @Override
        public void render(Renderer r) throws IOException {
            r.println("<thead>");
            r.println("<tr>");
            for (Column c : mColumns) {
                String title = "";
                if (c.hint != null) {
                    title = " title=\"" + c.hint + "\"";
                }
                r.println("<th" + title + ">" + c.title + "</th>");
            }
            r.println("</tr>");
            r.println("</thead>");
        }
    }

    public class TableBody extends DocNode {

        @Override
        public void render(Renderer r) throws IOException {
            r.println("<tbody>");
            super.render(r);
            r.println("</tbody>");
        }

    }

    public class TableRow extends DocNode {

        private String mStyle;
        private String mId;

        public void setStyle(String style) {
            mStyle = style;
        }

        public void setId(String id) {
            mId = id;
        }

        @Override
        public void render(Renderer r) throws IOException {
            r.print("<tr");
            if (mId != null) {
                r.print(" id=\"" + mId + "\"");
            }
            if (mStyle != null) {
                r.print(" class=\"" + mStyle + "\"");
            }
            r.println(">");
            super.render(r);
            r.println("</tr>");
        }

    }

    public class TableCell extends DocNode {

        private String mStyle;
        private String mHint;
        private String mText;

        public TableCell(String hint, String text, DocNode node) {
            mHint = hint;
            if (node != null) {
                add(node);
            } else {
                mText = text;
            }
        }

        public void addStyle(String style) {
            if (mStyle == null) {
                mStyle = style;
            } else {
                mStyle = mStyle + " " + style;
            }
        }

        @Override
        public void render(Renderer r) throws IOException {
            r.print("<td");
            if (mStyle != null) {
                r.print(" class=\"");
                r.print(mStyle);
                r.print("\"");

            }
            if (mHint != null) {
                r.print(" title=\"");
                r.print(mHint);
                r.print("\"");
            }
            r.print(">");
            if (mText != null) {
                r.print(mText);
            } else {
                super.render(r);
            }
            r.print("</td>");
        }

    }

    public Table() {
        this(FLAG_NONE);
    }

    public Table(int flag) {
        mTableFlags = flag;
    }

    public Table(int flag, DocNode parent) {
        this(flag);
        if (parent != null) {
            parent.add(this);
        }
    }

    public void setCSVOutput(Module br, String csv) {
        if (csv == null) return;
        String fn = br.getRelRawDir() + csv + ".csv";
        try {
            mCsvF = new FileOutputStream(br.getBaseDir() + fn);
            mCsvOut = new PrintStream(mCsvF);
            new Hint(this)
                .add("A CSV format version is saved as: ")
                .add(new Link(fn, fn));
        } catch (IOException e) {
            br.printErr(4, "Failed creating CSV file `" + fn + "': " + e);
            mCsvF = null;
            mCsvOut = null;
        }
    }

    public void setTableName(Module br, String name) {
        mConn = br.getSQLConnection();
        if (mConn != null) {
            mTable = name;
            new Hint(this).add("A table is created in the report database: " + mTable);
        }
    }

    public void addStyle(String s) {
        mStyles += s;
    }

    public void addColumn(String title, int flag) {
        addColumn(title, null, flag);
    }

    public void addColumn(String title, int flag, String dbSpec) {
        addColumn(title, null, flag, dbSpec);
    }

    public void addColumn(String title, String hint, int flag) {
        addColumn(title, hint, flag, null);
    }

    public void addColumn(String title, String hint, int flag, String dbSpec) {
        mColumns.add(new Column(title, hint, dbSpec, flag));
        csvField(Util.stripHtml(title));
    }

    public void begin() {
        TableHeader header = new TableHeader();
        add(header);
        mBody = new TableBody();
        add(mBody);

        if (0 != (mTableFlags & FLAG_SORT)) {
            new Hint(this).add("HINT: Click on the headers to sort the data. Shift+click to sort on multiple columns.");
        }
        if (0 != (mTableFlags & FLAG_DND)) {
            new Hint(this).add("HINT: you can drag and move table rows to reorder them!");
        }
        mColIdx = 0;
        csvEOL();

        // Create db table as well
        dbCreate();
    }

    private void dbCreate() {
        if (mConn == null) {
            return;
        }
        try {
            Statement stat = mConn.createStatement();
            StringBuffer sqlCreate = new StringBuffer();
            StringBuffer sqlInsert = new StringBuffer();
            sqlCreate.append("CREATE TABLE ");
            sqlCreate.append(mTable);
            sqlCreate.append(" (");
            sqlInsert.append("INSERT INTO ");
            sqlInsert.append(mTable);
            sqlInsert.append(" VALUES (");
            for (int i = 0; i < mColumns.size(); i++) {
                if (i > 0) {
                    sqlCreate.append(",");
                    sqlInsert.append(",");
                }
                sqlCreate.append(mColumns.get(i).dbSpec);
                sqlInsert.append("?");
            }
            sqlCreate.append(")");
            sqlInsert.append(")");
            stat.execute(sqlCreate.toString());
            mSqlInsert = mConn.prepareStatement(sqlInsert.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            dbAbort();
        }
    }

    private void dbField(int idx, String value) {
        if (mConn == null) {
            return;
        }
        try {
            if (mSqlInsert != null) {
                mSqlInsert.setString(idx + 1, value);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            dbAbort();
        }
    }

    private void dbEOL() {
        if (mConn == null) {
            return;
        }
        try {
            mSqlInsert.addBatch();
        } catch (SQLException e) {
            e.printStackTrace();
            dbAbort();
        }
    }

    private void dbEnd() {
        if (mConn == null) {
            return;
        }
        try {
            mSqlInsert.executeBatch();
            mSqlInsert.close();
            mConn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        dbAbort();
    }

    private void dbAbort() {
        mConn = null;
        mTable = null;
        mSqlInsert = null;
    }

    public void addData(DocNode node) {
        addData(null, null, node, FLAG_NONE);
    }

    public void addData(String text) {
        addData(null, text, null, FLAG_NONE);
    }

    public void addData(int value) {
        addData(null, Integer.toString(value), null, FLAG_NONE);
    }

    public void addData(long value) {
        addData(null, Long.toString(value), null, FLAG_NONE);
    }

    public void addData(float value) {
        addData(null, Float.toString(value), null, FLAG_NONE);
    }

    public void addData(String text, int flag) {
        addData(null, text, null, flag);
    }

    public void addData(String hint, DocNode node) {
        addData(hint, null, node, FLAG_NONE);
    }

    public void addData(String hint, String text, DocNode node, int flag) {
        String plainText = node == null ? Util.stripHtml(text) : node.getText();
        csvField(plainText);
        dbField(mColIdx, plainText);

        if (mColIdx == 0) {
            mRow = new TableRow();
            mBody.add(mRow);
            if (mNextRowStyle != null) {
                mRow.setStyle(mNextRowStyle);
                mNextRowStyle = null;
            }
            if (mNextRowId != null) {
                mRow.setId(mNextRowId);
                mNextRowId = null;
            }
        }
        TableCell cell = new TableCell(hint, text, node);
        mRow.add(cell);
        Column c = mColumns.get(mColIdx);
        if (0 != (c.flag & FLAG_ALIGN_RIGHT)) {
            cell.addStyle("right");
        }
        mColIdx = (mColIdx + 1) % mColumns.size();
        if (mColIdx == 0) {
            csvEOL();
            dbEOL();
        }
        mEmpty = false;
    }

    public void addSeparator() {
        mBody = new TableBody();
        add(mBody);
    }

    public void end() {
        csvEnd();
        dbEnd();
    }

    public void setNextRowStyle(String style) {
        mNextRowStyle = style;
    }

    public void setNextRowId(String s) {
        mNextRowId = s;
    }

    @Override
    public boolean isEmpty() {
        return mEmpty;
    }

    private void csvField(String text) {
        if (mCsvOut == null) return;
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

    @Override
    public void render(Renderer r) throws IOException {
        String tblCls = mStyles;
        if (0 != (mTableFlags & FLAG_SORT)) {
            tblCls += " tablesorter";
        }
        if (0 != (mTableFlags & FLAG_DND)) {
            tblCls += " tablednd";
        }
        if (0 != (mTableFlags & FLAG_COL_RESIZE)) {
            tblCls += " colResizable";
        }

        r.println("<table class=\"" + tblCls + "\">");
        super.render(r);
        r.println("</table>");
    }

}
