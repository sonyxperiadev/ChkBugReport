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

import com.sonyericsson.chkbugreport.BugReportModule.SourceFile;
import com.sonyericsson.chkbugreport.settings.BoolSetting;
import com.sonyericsson.chkbugreport.settings.Setting;
import com.sonyericsson.chkbugreport.settings.Settings;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

@SuppressWarnings("serial")
/* package */ class Gui extends JFrame implements OutputListener, ActionListener {

    private JButton mBtnAdb;
    private JButton mBtnExec;
    private Main mMain;
    private JComponent mDropArea;
    private JLabel mStatus;
    private Plugin mAdbExt;
    private BugReportModule mMod;

    public Gui(Main main) {
        super("ChkBugReport - (C) 2012 Sony-Ericsson");

        // Change window/application icon
        try {
            setIconImage(ImageIO.read(getClass().getResourceAsStream("/app_icon.png")));
        } catch (IOException e) {
            // Ignore error
        }

        mMain = main;
        mMod = (BugReportModule) mMain.getModule();

        JTabbedPane tabs = new JTabbedPane();
        setContentPane(tabs);

        JPanel runPanel = new JPanel(new BorderLayout());
        tabs.addTab("Run", runPanel);
        JPanel runTB = new JPanel();
        runPanel.add(runTB, BorderLayout.NORTH);
        mBtnAdb = new JButton("Fetch from device");
        mBtnAdb.setEnabled(false);
        runTB.add(mBtnAdb);
        mBtnExec = new JButton("Process");
        mBtnExec.setEnabled(false);
        mBtnExec.addActionListener(this);
        runTB.add(mBtnExec);
        mDropArea = new DropArea();
        runPanel.add(mDropArea, BorderLayout.CENTER);
        mDropArea.setBorder(BorderFactory.createLoweredBevelBorder());
        mDropArea.setTransferHandler(new MyTransferHandler());
        mStatus = new JLabel("Ready.");
        runPanel.add(mStatus, BorderLayout.SOUTH);

        mAdbExt = mMod.getPlugin("AdbExtension");
        if (mAdbExt != null) {
            mBtnAdb.setEnabled(true);
            mBtnAdb.addActionListener(this);
        }

        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        tabs.addTab("Settings", settingsPanel);
        buildSettings(settingsPanel);

        setSize(640, 480);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void buildSettings(JPanel settingsPanel) {
        final Settings settings = mMain.getSettings();
        for (Setting setting : settings) {
            if (setting instanceof BoolSetting) {
                final BoolSetting bs = (BoolSetting) setting;
                final JCheckBox chk = new JCheckBox(setting.getDescription());
                settingsPanel.add(chk);
                chk.setSelected(bs.get());
                chk.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        bs.set(chk.isSelected());
                        settings.save();
                    }
                });
            }
        }

    }

    private void enableUI(boolean enable) {
        mDropArea.setEnabled(enable);
        if (mAdbExt != null) {
            mBtnAdb.setEnabled(enable);
        }
    }

    public void loadFile(final String path) {
        new AsyncTask() {

            private String mErr;

            @Override
            public void before() {
                enableUI(false);
            }

            @Override
            public void run() {
                try {
                    mMod.addFile(path, null, false);
                } catch (final Exception e) {
                    mErr = e.getMessage();
                }
            }

            @Override
            public void after() {
                enableUI(true);
                mBtnExec.setEnabled(true);
                mBtnAdb.setEnabled(false);
                if (mErr != null) {
                    JOptionPane.showMessageDialog(Gui.this,
                            "Error loading file: " + mErr, "Error...",
                            JOptionPane.ERROR_MESSAGE);
                }
            }

        }.exec();
    }

    private void doProcessing() {
        new AsyncTask() {

            private String mErr;

            @Override
            public void before() {
                enableUI(false);
            }

            @Override
            public void run() {
                try {
                    mMod.generate();
                    mMain.openBrowserIfNeeded();
                } catch (final Exception e) {
                    mErr = e.getMessage();
                }
            }

            @Override
            public void after() {
                enableUI(true);
                mBtnExec.setEnabled(false);
                mBtnAdb.setEnabled(false);
                if (mErr != null) {
                    JOptionPane.showMessageDialog(Gui.this,
                            "Error processing file: " + mErr, "Error...",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.exec();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == mBtnAdb) {
            loadFile("adb://");
            return;
        }
        if (src == mBtnExec) {
            doProcessing();
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

    class DropArea extends JComponent {

        @Override
        public void paint(Graphics oldG) {
            Graphics2D g = (Graphics2D) oldG;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Insets inset = getBorder().getBorderInsets(this);

            // Clear background
            int w = getWidth();
            int h = getHeight();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);
            int x = inset.left, y = inset.top;
            w -= inset.left + inset.right;
            h -= inset.top + inset.bottom;

            // Render border
            paintBorder(oldG);

            // Render bugreport marker
            SourceFile sfBr = mMod.getSource();
            String header = "Bugreport: " + mMod.getSectionCount() + " sections";
            Color col = (sfBr == null) ? null : new Color(0x80c080);
            drawBox(g, col, header, mMod.getFileName(), x, y, w, h);

            // Render the child items
            int headerH = 2 * g.getFontMetrics().getHeight();
            x += 20;
            y += 20 + headerH;
            w -= 40;
            h -= 40 + headerH;

            Color boxColor = new Color(0x80c0ff);
            for (int i = 0; i < mMod.getSourceCount(); i++) {
                SourceFile sf = mMod.getSource(i);
                drawBox(g, boxColor, sf.mType, sf.mName, x, y, w, 40 + headerH);
                y += 40 + headerH;
            }

            // Render drop test
            if (sfBr == null && mMod.getSourceCount() == 0) {
                String s = "Drop files here!";
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 48));
                g.setColor(new Color(0x80000000, true));
                int sw = g.getFontMetrics().stringWidth(s);
                g.drawString(s, (w - sw) / 2, h / 2);
            }
        }

        private void drawBox(Graphics2D g, Color fill, String header, String fileName, int x, int y, int w, int h) {
            FontMetrics fm = g.getFontMetrics();
            if (fill == null) {
                Stroke save = g.getStroke();
                float[] dash = { 3.0f };
                g.setColor(Color.BLACK);
                g.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10.0f, dash, 0.0f));
                g.drawRoundRect(x + 10, y + 10, w - 20, h - 20, 20, 20);
                g.setStroke(save);
            } else {
                g.setColor(fill.brighter());
                g.fillRoundRect(x + 10, y + 10, w - 20, h - 20, 20, 20);
                g.setColor(fill);
                g.fillRoundRect(x + 10, y + 10, w - 20, 5 + fm.getHeight(), 20, 20);
                g.fillRect(x + 10, y + 10 + fm.getHeight(), w - 20, 5);
                Stroke save = g.getStroke();
                g.setColor(Color.BLACK);
                g.setStroke(new BasicStroke(3));
                g.drawRoundRect(x + 10, y + 10, w - 20, h - 20, 20, 20);
                g.setStroke(save);
            }
            x += 20;
            y += 15;
            w -= 40;
            g.setColor(Color.BLACK);
            g.drawString(header, x, y + fm.getAscent());
            y += fm.getHeight();
            g.drawLine(x - 10, y, x + w + 10, y);
            g.setColor(Color.BLACK);
            g.drawString(fileName, x, y + fm.getAscent());
        }

    }

}
