package com.sonyericsson.chkbugreport;

import java.awt.BorderLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

@SuppressWarnings("serial")
public class Gui extends JFrame implements Report.OutputListener, ActionListener {

    private JButton mBtnAdb;
    private Main mMain;
    private JLabel mDropArea;
    private JLabel mStatus;
    private Extension mAdbExt;

    public Gui(Main main) {
        super("ChkBugReport - (C) 2012 Sony-Ericsson");

        mMain = main;

        JTabbedPane tabs = new JTabbedPane();
        setContentPane(tabs);
        JPanel runPanel = new JPanel(new BorderLayout());
        tabs.addTab("Run", runPanel);
        JPanel runTB = new JPanel();
        runPanel.add(runTB, BorderLayout.NORTH);
        mBtnAdb = new JButton("Fetch from device");
        mBtnAdb.setEnabled(false);
        runTB.add(mBtnAdb);
        mDropArea = new JLabel("Drop a bugreport file here!", JLabel.CENTER);
        runPanel.add(mDropArea, BorderLayout.CENTER);
        mDropArea.setBorder(BorderFactory.createLoweredBevelBorder());
        mDropArea.setTransferHandler(new MyTransferHandler());
        mStatus = new JLabel("Ready.");
        runPanel.add(mStatus, BorderLayout.SOUTH);

        mAdbExt = mMain.findExtension("AdbExtension");
        if (mAdbExt != null) {
            mBtnAdb.setEnabled(true);
            mBtnAdb.addActionListener(this);
        }

        setSize(640, 480);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void enableUI(boolean enable) {
        mDropArea.setEnabled(enable);
        if (mAdbExt != null) {
            mBtnAdb.setEnabled(enable);
        }
    }

    public void loadFile(final String path) {
        enableUI(false);
        new Thread() {

            @Override
            public void run() {
                mMain.loadFile(path);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        enableUI(true);
                    }
                });
            }

        }.start();
    }

    public void loadFromAdb() {
        enableUI(false);
        new Thread() {

            @Override
            public void run() {
                String fileName = "adb://";
                int mode = mMain.getMode();
                Report report = mMain.createReportInstance(fileName, mode);
                try {
                    int ret = mAdbExt.loadReportFrom(report, fileName, mode);
                    if (ret != Main.RET_TRUE) {
                        mMain.onPrint(1, TYPE_ERR, "Failed loading bugreport from device!");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    mMain.onPrint(1, TYPE_ERR, "Failed loading bugreport from device: " + e);
                }
                try {
                    mMain.processFile(report);
                } catch (IOException e) {
                    e.printStackTrace();
                    mMain.onPrint(1, TYPE_ERR, "Failed processing bugreport: " + e);
                }
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        enableUI(true);
                    }
                });
            }

        }.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == mBtnAdb) {
            loadFromAdb();
            return;
        }

    }

    @Override
    public void onPrint(final int level, final int type, final String msg) {
        if (level <= 1) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    mStatus.setText(msg);
                }
            });
        }
    }

    class MyTransferHandler extends TransferHandler {

        @Override
        public boolean importData(TransferSupport support) {
            DataFlavor selDf = null;
            for (DataFlavor df : support.getDataFlavors()) {
                if (df.isMimeTypeEqual("text/uri-list")) {
                    if ("java.lang.String".equals(df.getParameter("class"))) {
                        selDf = df;
                        break;
                    }
                }
            }
            if (selDf != null) {
                String path;
                try {
                    path = (String) support.getTransferable().getTransferData(selDf);
                    URL url = new URL(path);
                    if (url.getProtocol().equals("file")) {
                        loadFile(URLDecoder.decode(url.getPath(), "UTF-8"));
                        return true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return false;
        }

        @Override
        public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
            for (DataFlavor df : transferFlavors) {
                if (df.isMimeTypeEqual("text/uri-list")) {
                    if ("java.lang.String".equals(df.getParameter("class"))) {
                        return true;
                    }
                }
            }
            return false;
        }

    }

}
