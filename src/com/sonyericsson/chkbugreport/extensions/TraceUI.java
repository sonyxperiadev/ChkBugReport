//package com.sonyericsson.chkbugreport.extensions; // FIXME
//
//
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.Vector;
//
//import javax.swing.JButton;
//import javax.swing.JComboBox;
//import javax.swing.JFrame;
//import javax.swing.JPanel;
//
//import com.android.ddmlib.AndroidDebugBridge;
//import com.android.ddmlib.IDevice;
//import com.android.ddmlib.IShellOutputReceiver;
//import com.android.ddmlib.SyncService;
//import com.android.ddmlib.SyncService.ISyncProgressMonitor;
//import com.sonyericsson.chkbugreport.Lines;
//import com.sonyericsson.chkbugreport.Util;
//import com.sonyericsson.chkbugreport.traceview.TraceReport;
//
//public class TraceUI implements ActionListener {
//
//    private TraceReport mReport;
//    private String mFileName;
//    private JComboBox mList;
//    private JButton mBtnStart;
//    private JButton mBtnStop;
//    private JButton mBtnExit;
//    private JFrame mWin;
//    private IDevice mDev;
//
//    public TraceUI(TraceReport br, String fileName) {
//        mReport = br;
//        mFileName = fileName.substring(6); // strip "adb://"
//    }
//
//    public void run() throws IOException {
//        AndroidDebugBridge.init(false);
//        AndroidDebugBridge adb = AndroidDebugBridge.createBridge();
//        if (adb == null) {
//            throw new IOException("Failed connecting to adb. Maybe you need to run it from the command line first.");
//        }
//
//        // Now find a device
//        while (!adb.hasInitialDeviceList()) {
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) { }
//        }
//        IDevice[] devs = adb.getDevices();
//        if (devs == null || devs.length == 0) {
//            throw new IOException("No ADB device found.");
//        }
//        if (devs.length > 1) {
//            throw new IOException("Too many ADB devices found.");
//        }
//        mDev = devs[0];
//
//        // Now generate a filename
//        mFileName = "adb_" + Util.createTimeStamp() + ".prof";
//        mReport.setFileName(mFileName);
//
//        // Fetch the list of processes
//        Lines psOut = adbExec("ps");
//        String zygPid = null;
//        for (int i = 0; i < psOut.getLineCount(); i++) {
//            String name = psOut.getLine(i).substring(55);
//            if (name.equals("zygote")) {
//                zygPid = psOut.getLine(i).substring(10, 15);
//            }
//        }
//        if (zygPid == null) {
//            throw new IOException("Zygote not found!");
//        }
//        Vector<AdbProc> procs = new Vector<AdbProc>();
//        for (int i = 0; i < psOut.getLineCount(); i++) {
//            String line = psOut.getLine(i);
//            String ppid = line.substring(16, 21);
//            if (zygPid.equals(ppid)) {
//                String name = line.substring(55);
//                String pidS = Util.strip(line.substring(10, 15));
//                int pid = Integer.parseInt(pidS);
//                procs.add(new AdbProc(pid, name));
//            }
//        }
//        Collections.sort(procs, new Comparator<AdbProc>() {
//            @Override
//            public int compare(AdbProc o1, AdbProc o2) {
//                return o1.name.compareTo(o2.name);
//            }
//        });
//
//        mWin = new JFrame("Trace process...");
//        mWin.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
//        JPanel panel = new JPanel();
//        mWin.setContentPane(panel);
//
//        mList = new JComboBox(procs);
//        panel.add(mList);
//
//        mBtnStart = new JButton("Start");
//        mBtnStart.addActionListener(this);
//        panel.add(mBtnStart);
//
//        mBtnStop = new JButton("Stop");
//        mBtnStop.addActionListener(this);
//        mBtnStop.setEnabled(false);
//        panel.add(mBtnStop);
//
//        mBtnExit = new JButton("Exit");
//        mBtnExit.addActionListener(this);
//        panel.add(mBtnExit);
//
//        mWin.pack();
//        mWin.setLocationRelativeTo(null);
//        mWin.setVisible(true);
//    }
//
//    private Lines adbExec(String string) {
//        final Lines ret = new Lines("ret");
//        try {
//            mDev.executeShellCommand(string, new IShellOutputReceiver() {
//                private StringBuffer sb = new StringBuffer();
//
//                @Override
//                public boolean isCancelled() {
//                    return false;
//                }
//
//                @Override
//                public void flush() {
//                    if (sb.length() > 0) {
//                        ret.addLine(sb.toString());
//                        sb.delete(0, sb.length());
//                    }
//                }
//
//                @Override
//                public void addOutput(byte[] buff, int offs, int len) {
//                    for (int i = 0; i < len; i++) {
//                        char c = (char)buff[offs + i];
//                        if (c == '\r') continue; // We hate this character
//                        if (c == '\n') {
//                            flush();
//                            continue;
//                        }
//                        sb.append(c);
//                    }
//                }
//            });
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return ret;
//    }
//
//    @Override
//    public void actionPerformed(ActionEvent e) {
//        JButton src = (JButton)e.getSource();
//
//        if (src == mBtnExit) {
//            shutdown(0);
//        }
//
//        if (src == mBtnStart) {
//            mBtnStart.setEnabled(false);
//            mBtnExit.setEnabled(false);
//            mList.setEnabled(false);
//            AdbProc proc = (AdbProc)mList.getSelectedItem();
//            Lines out = adbExec("am profile " + proc.pid + " start /data/profile.dat");
//            if (out.getLineCount() == 0) {
//                // Success
//                mBtnStop.setEnabled(true);
//            } else {
//                System.err.println("Error starting profiling:");
//                for (int i = 0; i < out.getLineCount(); i++) {
//                    System.err.println("> " + out.getLine(i));
//                }
//                shutdown(1);
//            }
//        }
//
//        if (src == mBtnStop) {
//            mBtnStop.setEnabled(false);
//            AdbProc proc = (AdbProc)mList.getSelectedItem();
//            Lines out = adbExec("am profile " + proc.pid + " stop");
//            if (out.getLineCount() == 0) {
//                // Success
//                try {
//                    // Need to wait a bit, so the file is actually saved
//                    Thread.sleep(2000);
//                    SyncService ss = mDev.getSyncService();
//                    ss.pullFile("/data/profile.dat", mFileName, new ISyncProgressMonitor() {
//                        @Override
//                        public void stop() {
//                            System.out.println("File fetched!");
//                        }
//                        @Override
//                        public void startSubTask(String arg0) {
//                        }
//
//                        @Override
//                        public void start(int arg0) {
//                            System.out.println("Fetching file from device...");
//                        }
//
//                        @Override
//                        public boolean isCanceled() {
//                            return false;
//                        }
//
//                        @Override
//                        public void advance(int arg0) {
//                        }
//                    });
//                    // We need to wait here a bit
//                    mReport.load(new FileInputStream(mFileName));
//                    mReport.generate();
//                    shutdown(0);
//                } catch (Exception e1) {
//                    e1.printStackTrace();
//                    shutdown(1);
//                }
//            } else {
//                shutdown(1);
//                System.err.println("Error stopping profiling:");
//                for (int i = 0; i < out.getLineCount(); i++) {
//                    System.err.println("> " + out.getLine(i));
//                }
//            }
//        }
//    }
//
//    private void shutdown(int ret) {
//        mWin.setVisible(false);
//        // Close the ADB connection
//        AndroidDebugBridge.terminate();
//        // Exit the java app
//        System.exit(ret);
//    }
//
//    static class AdbProc {
//        String name;
//        int pid;
//        public AdbProc(int pid, String name) {
//            this.pid = pid;
//            this.name = name;
//        }
//        @Override
//        public String toString() {
//            return name + "(" + pid + ")";
//        }
//    }
//
//}
