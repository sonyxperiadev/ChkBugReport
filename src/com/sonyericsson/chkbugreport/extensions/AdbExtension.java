package com.sonyericsson.chkbugreport.extensions;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.RawImage;
import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.Extension;
import com.sonyericsson.chkbugreport.Main;
import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Util;
import com.sonyericsson.chkbugreport.traceview.TraceReport;

public class AdbExtension extends Extension {

    @Override
    public int loadReportFrom(Module report, String fileName, int mode) throws IOException {
        // Try special devices, like "adb://"
        if (fileName.startsWith("adb://")) {
            if (mode == Main.MODE_BUGREPORT) {
                BugReportModule br = (BugReportModule)report;
                loadFromADB(br, fileName);
                return Main.RET_TRUE; // Done
            } else if (mode == Main.MODE_TRACEVIEW) {
                TraceReport tr = (TraceReport)report;
                new TraceUI(tr, fileName).run();
                return Main.RET_WAIT; // Do not exit yet, GUI still running
            }
        }

        return Main.RET_NOP;
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
        final FileInputStream fis = new FileInputStream(fileName);
        br.load(fis);
        fis.close();

        // This is a placeholder to add other sections (which we can have only with adb)
        // For example we can save a screenshot
        try {
            RawImage ss = dev.getScreenshot();
            if (ss != null) {
                br.addMetaInfo("screenshot", convertImage(ss));
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
