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
package com.sonyericsson.chkbugreport;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;

/** Image canvas for Desktop Java */
public class ImageCanvas {

    public static final int BLACK       = 0xff000000;
    public static final int WHITE       = 0xffffffff;
    public static final int YELLOW      = 0xffffff00;
    public static final int DARK_GRAY   = 0xff666666;
    public static final int LIGHT_GRAY  = 0xffbbbbbb;
    public static final int RED         = 0xffff0000;
    public static final int CYAN        = 0xff00ffff;

    private BufferedImage mImg;
    private Graphics2D mG;

    private float mStrokeWidth = 1.0f;

    public ImageCanvas(int width, int height) {
        mImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        mG = (Graphics2D)mImg.getGraphics();
        mG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    public int getWidth() {
        return mImg.getWidth();
    }

    public int getHeight() {
        return mImg.getHeight();
    }

    public void writeTo(File f) throws IOException {
        ImageIO.write(mImg, "png", f);
    }

    public void writeTo(OutputStream os) throws IOException {
        ImageIO.write(mImg, "png", os);
    }

    public float getStrokeWidth() {
        return mStrokeWidth;
    }

    public void setStrokeWidth(float width) {
        mStrokeWidth = width;
        mG.setStroke(new BasicStroke(width));
    }

    public void setColor(int argb) {
        mG.setColor(new Color(argb, true));
    }

    public void drawLine(int x0, int y0, int x1, int y1) {
        mG.drawLine(x0, y0, x1, y1);
    }

    public void fillRect(int x, int y, int w, int h) {
        mG.fillRect(x, y, w, h);
    }

    public void drawRect(int x, int y, int w, int h) {
        mG.drawRect(x, y, w, h);
    }

    public float getFontHeight() {
        return mG.getFontMetrics().getHeight();
    }

    public float getStringWidth(String string) {
        return mG.getFontMetrics().stringWidth(string);
    }

    public float getAscent() {
        return mG.getFontMetrics().getAscent();
    }

    public float getDescent() {
        return mG.getFontMetrics().getDescent();
    }

    public void drawString(String string, float x, float y) {
        mG.drawString(string, x, y);
    }

    public void translate(float dx, float dy) {
        mG.translate(dx, dy);
    }

    public void rotate(float degrees) {
        mG.rotate(degrees);
    }

}
