/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
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


import com.sonyericsson.chkbugreport.util.XMLNode;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;


public class DataSet implements Iterable<Data> {

    public enum Type {
        PLOT,
        MINIPLOT,
        STATE,
        EVENT,
    }

    private static final HashMap<String,DataSet.Type> TYPE_TBL;
    static {
        TYPE_TBL = new HashMap<String, DataSet.Type>();
        TYPE_TBL.put("plot", DataSet.Type.PLOT);
        TYPE_TBL.put("miniplot", DataSet.Type.MINIPLOT);
        TYPE_TBL.put("state", DataSet.Type.STATE);
        TYPE_TBL.put("event", DataSet.Type.EVENT);
    }

    private static final int DEF_COLOR = 0x80000000;

    private String mId;
    private String mName;
    private Type mType;
    private Vector<Data> mDatas = new Vector<Data>();
    private Vector<Integer> mColors = new Vector<Integer>();

    private long mMax;
    private long mMin;
    private boolean mMinFixed;
    private boolean mMaxFixed;

    private int[] mGuessMap;

    private int mAxisId;

    private long mFirstTs = Long.MAX_VALUE;
    private long mLastTs = Long.MIN_VALUE;

    public DataSet(Type type, String name) {
        mType = type;
        mName = name;
    }

    public DataSet(Type type, String name, int col) {
        this(type, name);
        mColors.add(col);
    }

    public void setId(String id) {
        if (id == null) throw new NullPointerException();
        mId = id;
    }

    public String getId() {
        return mId;
    }

    public void setName(String name) {
        if (name == null) throw new NullPointerException();
        mName = name;
    }

    public String getName() {
        return mName;
    }

    public void setType(Type type) {
        mType = type;
    }

    public Type getType() {
        return mType;
    }

    public void insertData(Data data) {
        checkMinMax(data);
        mDatas.add(0, data);
    }

    public void addData(Data data) {
        checkMinMax(data);
        mDatas.add(data);
    }

    private void checkMinMax(Data data) {
        if (mDatas.isEmpty()) {
            if (!mMinFixed) mMin = data.value;
            if (!mMaxFixed) mMax = data.value;
        } else {
            if (!mMinFixed) mMin = Math.min(mMin, data.value);
            if (!mMaxFixed) mMax = Math.max(mMax, data.value);
        }
        mFirstTs = Math.min(mFirstTs, data.time);
        mLastTs = Math.max(mLastTs, data.time);
    }

    public long getFirstTs() {
        return mFirstTs;
    }

    public long getLastTs() {
        return mLastTs;
    }

    public int getDataCount() {
        return mDatas.size();
    }

    public Data getData(int idx) {
        return mDatas.get(idx);
    }

    public int getAxisId() {
        return mAxisId;
    }

    public void setAxisId(int id) {
        mAxisId = id;
    }

    public long getMin() {
        return mMin;
    }

    public long getMax() {
        return mMax;
    }

    public void sort() {
        Collections.sort(mDatas, new Comparator<Data>(){
            @Override
            public int compare(Data o1, Data o2) {
                if (o1.time < o2.time) return -1;
                if (o1.time > o2.time) return +1;
                return 0;
            }
        });
    }

    public void addColor(int argb) {
        mColors.add(argb);
    }

    public void addColor(String rgb) {
        if (rgb.startsWith("#")) {
            rgb = rgb.substring(1);
        }
        int a = 0x80, r = 0x00, g = 0x00, b = 0x00;
        switch (rgb.length()) {
            case 4: // #argb
                a = Integer.parseInt(rgb.substring(0, 1), 16);
                a = a | (a << 4);
                rgb = rgb.substring(1);
            case 3: // #rgb
                r = Integer.parseInt(rgb.substring(0, 1), 16);
                r = r | (r << 4);
                g = Integer.parseInt(rgb.substring(1, 2), 16);
                g = g | (g << 4);
                b = Integer.parseInt(rgb.substring(2, 3), 16);
                b = b | (b << 4);
                break;
            case 8: // #aarrggbb
                a = Integer.parseInt(rgb.substring(0, 2), 16);
                rgb = rgb.substring(2);
            case 6: // #rrggbb
                r = Integer.parseInt(rgb.substring(0, 2), 16);
                g = Integer.parseInt(rgb.substring(2, 4), 16);
                b = Integer.parseInt(rgb.substring(4, 6), 16);
                break;
            default:
                throw new RuntimeException("Cannot parse color: " + rgb);
        }
        int rgba = (a << 24) | (r << 16) | (g << 8) | b;
        mColors.add(rgba);
    }

    public int getColor(long idx) {
        if (idx < 0 || idx >= mColors.size()) {
            return DEF_COLOR;
        }
        return mColors.get((int) idx);
    }

    @Override
    public Iterator<Data> iterator() {
        return mDatas.iterator();
    }

    public void setMin(int value) {
        mMin = value;
        mMinFixed = true;
    }

    public void setMax(int value) {
        mMax = value;
        mMaxFixed = true;
    }

    public void setGuessMap(String attr) {
        String f[] = attr.split(",");
        int cnt = f.length;
        mGuessMap = new int[cnt];
        for (int i = 0; i < cnt; i++) {
            mGuessMap[i] = Integer.parseInt(f[i]);
        }
    }

    public int getGuessFor(int newState) {
        if (mGuessMap == null || newState < 0 || newState >= mGuessMap.length) {
            return -1;
        }
        return mGuessMap[newState];
    }

    public boolean isEmpty() {
        return mDatas.isEmpty();
    }

    public static DataSet parse(XMLNode node) {
        String name = node.getAttr("name");
        Type type = TYPE_TBL.get(node.getAttr("type"));
        DataSet ds = new DataSet(type, name);
        ds.setId(node.getAttr("id"));

        // Parse optional color array
        String attr = node.getAttr("colors");
        if (attr != null) {
            for (String rgb : attr.split(",")) {
                ds.addColor(rgb);
            }
        }

        // Parse optional min/max values
        attr = node.getAttr("min");
        if (attr != null) {
            ds.setMin(Integer.parseInt(attr));
        }
        attr = node.getAttr("max");
        if (attr != null) {
            ds.setMax(Integer.parseInt(attr));
        }

        // Parse optional guess map, used to guess the previous state from the current one
        attr = node.getAttr("guessmap");
        if (attr != null) {
            ds.setGuessMap(attr);
        }

        // Parse optional axis id attribute
        attr = node.getAttr("axis");
        if (attr != null) {
            ds.setAxisId(Integer.parseInt(attr));
        }

        return ds;
    }

}
