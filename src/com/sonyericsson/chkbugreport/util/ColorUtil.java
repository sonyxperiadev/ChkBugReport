/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
 * Copyright (C) 2012-2013 Sony Mobile Communications AB
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
package com.sonyericsson.chkbugreport.util;


/**
 * Contains various helper methods to process colors.
 */
public final class ColorUtil {

    private static final int[] COLORS = {
        0xff0000, 0x00ff00, 0x0000ff, 0x00ffff, 0xff00ff, 0xffff00,
        0xff8000, 0x80ff00, 0x8000ff, 0xff0080, 0x00ff80, 0x0080ff,
        0xff8080, 0x80ff80, 0x8080ff,
        0x800000, 0x008000, 0x000080, 0x008080, 0x800080, 0x808000,
    };

    /**
     * Return a (possibly) unique color to render data #idx
     * @param idx
     * @return An RGB color value
     */
    public static int getColor(int idx) {
        return COLORS[idx % COLORS.length];
    }

    public static int getColorShade(long value, long maxValue, int rgb0, int rgb1) {
        int a0 = aof(rgb0);
        int r0 = rof(rgb0);
        int g0 = gof(rgb0);
        int b0 = bof(rgb0);

        int a1 = aof(rgb1);
        int r1 = rof(rgb1);
        int g1 = gof(rgb1);
        int b1 = bof(rgb1);

        int a = (int) (a0 + (a1 - a0) * value / maxValue);
        int r = (int) (r0 + (r1 - r0) * value / maxValue);
        int g = (int) (g0 + (g1 - g0) * value / maxValue);
        int b = (int) (b0 + (b1 - b0) * value / maxValue);

        return rgb(a, r, g, b);
    }

    private static int rgb(int a, int r, int g, int b) {
        int ret = 0;
        ret |= (a & 0xff) << 24;
        ret |= (r & 0xff) << 16;
        ret |= (g & 0xff) <<  8;
        ret |= (b & 0xff) <<  0;
        return ret;
    }

    public static int aof(int rgb) {
        return (rgb >> 24) & 0xff;
    }

    public static int rof(int rgb) {
        return (rgb >> 16) & 0xff;
    }

    public static int gof(int rgb) {
        return (rgb >> 8) & 0xff;
    }

    public static int bof(int rgb) {
        return (rgb >> 0) & 0xff;
    }

}
