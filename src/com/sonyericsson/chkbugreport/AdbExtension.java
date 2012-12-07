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
package com.sonyericsson.chkbugreport;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.RawImage;
import com.sonyericsson.chkbugreport.util.Util;

import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;

/* package */ class AdbExtension extends Plugin {

    @Override
    public int getPrio() {
        return 0;
    }

    @Override
    public void reset() {
    }

    @Override
    public void load(Module mod) {
    }

    @Override
    public void generate(Module mod) {
    }

    @Override
    public boolean handleFile(Module module, String fileName, String type) {
        try {
            if (fileName.startsWith("adb://")) {
                loadFromADB((BugReportModule) module, fileName);
                return true;
            }
        } catch (IOException e) {
            throw new IllegalParameterException(e.getMessage());
        }
        return false;
    }

    public void loadFromADB(BugReportModule br, String fileName) throws IOException {
        fileName = fileName.substring(6); // strip "adb://"
        AndroidDebugBridge.init(false);
        AndroidDebugBridge adb = AndroidDebugBridge.createBridge();
        if (adb == null) {
            throw new IOException("Failed connecting to adb. Maybe you need to run it from the command line first.");
        }

        // Now find a device
        while (!adb.hasInitialDeviceList()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) { }
        }
        IDevice[] devs = adb.getDevices();
        if (devs == null || devs.length == 0) {
            throw new IOException("No ADB device found.");
        }
        if (devs.length > 1) {
            throw new IOException("Too many ADB devices found.");
        }
        IDevice dev = devs[0];

        // Now generate a filename
        fileName = "adb_" + Util.createTimeStamp() + ".txt";
        br.setFileName(fileName);

        // Fetch the bugreport and save it locally
        br.printOut(1, "Capturing bugreport from device to " + fileName + " ...");
        final FileOutputStream fos = new FileOutputStream(fileName);
        try {
            dev.executeShellCommand("bugreport", new IShellOutputReceiver() {
                @Override
                public boolean isCancelled() { return false; }
                @Override
                public void flush() { }
                @Override
                public void addOutput(byte[] buff, int offs, int len) {
                    try {
                        fos.write(buff, offs, len);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            // Still continue, since we might have a partial bugreport
        }
        fos.close();

        // Read the bugreport
        br.addFile(fileName, null, false);

        // This is a placeholder to add other sections (which we can have only with adb)
        // For example we can save a screenshot
        try {
            RawImage ss = dev.getScreenshot();
            if (ss != null) {
                br.addInfo("screenshot", convertImage(ss));
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Not a big issue, just continue without a screenshot
        }

        // Close the ADB connection
        AndroidDebugBridge.terminate();
    }

    private BufferedImage convertImage(RawImage img) {
        int w = img.width;
        int h = img.height;
        int size = w * h;
        int argb[] = new int[size];
        int bytesPerPixel = img.bpp / 8;

        // First, need to detect the alpha channel, which will probably be
        // the one with the highest average value (since the screenshot
        // should be opaque)
        long sums[] = new long[4];
        for (int i = 0; i < size; i++) {
            int value = img.getARGB(i * bytesPerPixel);
            sums[0] += (value >> 24) & 0xff;
            sums[1] += (value >> 16) & 0xff;
            sums[2] += (value >>  8) & 0xff;
            sums[3] += (value >>  0) & 0xff;
        }
        boolean isArgb = (sums[0] > sums[1]) && (sums[0] > sums[2]) && (sums[0] > sums[3]);

        for (int i = 0; i < size; i++) {
            int value = img.getARGB(i * bytesPerPixel);
            int r, g, b;
            if (isArgb) {
                r = (value >> 16) & 0xff;
                g = (value >>  8) & 0xff;
                b = (value >>  0) & 0xff;
            } else {
                r = (value >> 24) & 0xff;
                g = (value >> 0) & 0xff;
                b = (value >> 8) & 0xff;
            }
            argb[i] = (r << 16) | (g << 8) | (b << 0) | 0xff000000;
        }
        BufferedImage bimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        bimg.setRGB(0, 0, w, h, argb, 0, w);
        return bimg;
    }

}
