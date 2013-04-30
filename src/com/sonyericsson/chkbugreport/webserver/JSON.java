/*
 * Copyright (C) 2013 Sony Mobile Communications AB
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
package com.sonyericsson.chkbugreport.webserver;

import com.sonyericsson.chkbugreport.webserver.engine.HTTPResponse;

import java.util.Vector;

public class JSON {

    public enum Type {
        INTEGER,
        FLOAT,
        STRING,
        ARRAY,
        OBJECT,
    };

    private String mName = null;
    private Type mType;
    private long mIntValue;
    private double mFloatValue;
    private String mStringValue;
    private Vector<JSON> mObjValues;

    public JSON() {
        this(null, Type.OBJECT);
    }

    private JSON(String name, Type type) {
        mName = name;
        mType = type;
        if (type == Type.ARRAY || type == Type.OBJECT) {
            mObjValues = new Vector<JSON>();
        }
    }

    public JSON addArray(String name) {
        JSON ret = new JSON(name, Type.ARRAY);
        mObjValues.add(ret);
        return ret;
    }

    public JSON add() {
        if (mType != Type.ARRAY) {
            throw new IllegalArgumentException("Array expected");
        }
        JSON ret = new JSON(null, Type.OBJECT);
        mObjValues.add(ret);
        return ret;
    }

    public JSON add(String stringValue) {
        if (mType != Type.ARRAY) {
            throw new IllegalArgumentException("Array expected");
        }
        JSON ret = new JSON(null, Type.STRING);
        ret.mStringValue = stringValue;
        mObjValues.add(ret);
        return ret;
    }

    public JSON add(String name, String value) {
        if (mType != Type.OBJECT) {
            throw new IllegalArgumentException("Object expected");
        }
        JSON ret = new JSON(name, Type.STRING);
        ret.mStringValue = value;
        mObjValues.add(ret);
        return ret;
    }

    public JSON add(String name, long value) {
        if (mType != Type.OBJECT) {
            throw new IllegalArgumentException("Object expected");
        }
        JSON ret = new JSON(name, Type.INTEGER);
        ret.mIntValue = value;
        mObjValues.add(ret);
        return ret;
    }

    public JSON add(String name, double value) {
        if (mType != Type.OBJECT) {
            throw new IllegalArgumentException("Object expected");
        }
        JSON ret = new JSON(name, Type.FLOAT);
        ret.mFloatValue = value;
        mObjValues.add(ret);
        return ret;
    }

    public void writeTo(HTTPResponse resp) {
        resp.addHeader("Content-Type", "application/json");
        StringBuilder sb = new StringBuilder();
        writeTo(sb, "", false);
        resp.println(sb.toString());
    }

    private void writeTo(StringBuilder sb, String indent, boolean inclName) {
        String indent2;
        sb.append(indent);
        if (inclName && mName != null) {
            writeString(mName, sb);
            sb.append(": ");
        }
        switch (mType) {
        case INTEGER:
            sb.append(mIntValue);
            break;
        case FLOAT:
            sb.append(mFloatValue);
            break;
        case STRING:
            writeString(mStringValue, sb);
            break;
        case ARRAY:
            sb.append("[\n");
            indent2 = indent + "  ";
            for (int i = 0; i < mObjValues.size(); i++) {
                mObjValues.get(i).writeTo(sb, indent2, false);
                if (i < mObjValues.size() - 1) {
                    sb.append(",\n");
                } else {
                    sb.append("\n");
                }
            }
            sb.append(indent);
            sb.append("]");
            break;
        case OBJECT:
            sb.append("{\n");
            indent2 = indent + "  ";
            for (int i = 0; i < mObjValues.size(); i++) {
                mObjValues.get(i).writeTo(sb, indent2, true);
                if (i < mObjValues.size() - 1) {
                    sb.append(",\n");
                } else {
                    sb.append("\n");
                }
            }
            sb.append(indent);
            sb.append("}");
            break;
        }
    }

    private void writeString(String str, StringBuilder sb) {
        sb.append('"');
        int l = str.length();
        for (int i = 0; i < l; i++) {
            char c = str.charAt(i);
            if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '\"') {
                sb.append("\\\"");
            } else if (c == '\b') {
                sb.append("\\b");
            } else if (c == '\t') {
                sb.append("\\t");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\'') {
                sb.append("\\'");
            } else {
                sb.append(c);
            }
        }
        sb.append('"');
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        writeTo(sb, "", false);
        return sb.toString();
    }

}
