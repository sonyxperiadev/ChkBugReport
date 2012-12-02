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
package com.sonyericsson.chkbugreport.chart;

import java.awt.Color;

public class Marker {

    public enum Type { X, Y };

    private Type mType;
    private long mValue;
    private Color mColor;

    public Marker(Type type, long value, Color color) {
        mType = type;
        mValue = value;
        mColor = color;
    }

    public Type getType() {
        return mType;
    }

    public long getValue() {
        return mValue;
    }

    public Color getColor() {
        return mColor;
    }

}
