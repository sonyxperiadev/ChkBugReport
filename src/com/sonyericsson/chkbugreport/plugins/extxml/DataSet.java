package com.sonyericsson.chkbugreport.plugins.extxml;

import java.awt.Color;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;


public class DataSet implements Iterable<Data> {

    public enum Type {
        PLOT,
        STATE,
        EVENT,
    }

    private static final Color DEF_COLOR = new Color(0x80000000, true);

    private String mId;
    private String mName;
    private Type mType;
    private Vector<Data> mDatas = new Vector<Data>();
    private Vector<Color> mColors = new Vector<Color>();

    private int mMax;
    private int mMin;
    private boolean mMinFixed;
    private boolean mMaxFixed;

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

    public void addData(Data data) {
        if (mDatas.isEmpty()) {
            if (!mMinFixed) mMin = data.value;
            if (!mMaxFixed) mMax = data.value;
        } else {
            if (!mMinFixed) mMin = Math.min(mMin, data.value);
            if (!mMaxFixed) mMax = Math.max(mMax, data.value);
        }
        mDatas.add(data);
    }

    public int getDataCount() {
        return mDatas.size();
    }

    public Data getData(int idx) {
        return mDatas.get(idx);
    }

    public int getMin() {
        return mMin;
    }

    public int getMax() {
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
        mColors.add(new Color(rgba, true));
    }

    public Color getColor(int idx) {
        if (idx < 0 || idx >= mColors.size()) {
            return DEF_COLOR;
        }
        return mColors.get(idx);
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

}
