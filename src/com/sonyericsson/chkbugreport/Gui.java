package com.sonyericsson.chkbugreport;

import java.awt.BorderLayout;
import java.awt.datatransfer.DataFlavor;
import java.net.URL;

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
public class Gui extends JFrame {

    private JButton mBtnAdb;
    private Main mMain;
    private JLabel mDropArea;

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
        runTB.add(mBtnAdb);
        mDropArea = new JLabel("Drop a bugreport file here!", JLabel.CENTER);
        runPanel.add(mDropArea, BorderLayout.CENTER);
        mDropArea.setBorder(BorderFactory.createLoweredBevelBorder());
        mDropArea.setTransferHandler(new MyTransferHandler());

        setSize(640, 480);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    public void loadFile(final String path) {
        mDropArea.setEnabled(false);
        new Thread() {

            @Override
            public void run() {
                mMain.loadFile(path);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        mDropArea.setEnabled(true);
                    }
                });
            }

        }.start();
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
                        loadFile(url.getPath());
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
