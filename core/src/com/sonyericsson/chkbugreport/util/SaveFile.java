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
package com.sonyericsson.chkbugreport.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Helper class to save and restore data.
 * You can see this as a fake database, which contains tables, with mandatory primary keys.
 * Or as a registry. The reason it's not using the database is backwards compatbility: upgrading
 * databases is too much of a hassle, so this class uses xml files instead.
 */
public class SaveFile {

    private File mFile;
    private HashMap<String, XMLNode> mData = new HashMap<String, XMLNode>();
    private byte mBuffer[] = new byte[0x10000];

    public SaveFile(String fn) throws IOException {
        mFile = new File(fn);

        if (mFile.exists()) {
            load();
        }
    }

    private void load() throws IOException {
        ZipFile zip = new ZipFile(mFile);
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry e = entries.nextElement();
            if (!e.isDirectory() && e.getName().endsWith(".xml")) {
                load(zip, e);
            }
        }
        zip.close();
    }

    private void load(ZipFile zip, ZipEntry e) throws IOException {
        String name = e.getName();
        InputStream is = zip.getInputStream(e);
        XMLNode xml = XMLNode.parse(is);
        is.close();
        mData.put(name, xml);
    }

    public void commit(String tblName) {
        tblName += ".xml";
        XMLNode tbl = mData.get(tblName);
        if (tbl == null) {
            return;
        }
        try {
            // Create a new zip file, copy other entries, update this entry
            File tmpFile = new File(mFile.getAbsolutePath() + ".tmp");
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(tmpFile));
            if (mFile.exists()) {
                ZipFile zip = new ZipFile(mFile);
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry e = entries.nextElement();
                    if (!e.isDirectory() && !e.getName().equals(tblName)) {
                        out.putNextEntry(e);
                        copy(zip.getInputStream(e), out);
                        out.closeEntry();
                    }
                }
                zip.close();
            }
            ZipEntry e = new ZipEntry(tblName);
            out.putNextEntry(e);
            tbl.dump(out);
            out.closeEntry();
            out.close();
            tmpFile.renameTo(mFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void copy(InputStream in, ZipOutputStream out) throws IOException {
        int read;
        while (-1 != (read = in.read(mBuffer))) {
            out.write(mBuffer, 0, read);
        }
        in.close();
    }

    public ResultSet select(String tblName) {
        tblName += ".xml";
        ResultSet ret = new ResultSet();
        XMLNode tbl = mData.get(tblName);
        if (tbl != null) {
            for (XMLNode node : tbl.getChildren("row")) {
                ret.add(node);
            }
        }
        return ret;
    }

    public XMLNode insert(String tblName, String idAttr) {
        tblName += ".xml";
        XMLNode tbl = mData.get(tblName);
        if (tbl == null) {
            tbl = new XMLNode("table");
            tbl.addAttr("seq", "0");
            mData.put(tblName, tbl);
        }
        XMLNode ret = new XMLNode("row");
        String seq = tbl.getAttr("seq");
        ret.addAttr(idAttr, seq);
        tbl.addAttr("seq", Integer.toString(Integer.parseInt(seq) + 1));
        tbl.add(ret);
        return ret;
    }

    public XMLNode findById(String tblName, String idAttr, int id) {
        tblName += ".xml";
        XMLNode tbl = mData.get(tblName);
        if (tbl == null) {
            return null;
        }
        for (XMLNode ret : tbl.getChildren("row")) {
            if (id == Integer.parseInt(ret.getAttr(idAttr))) {
                return ret;
            }
        }
        return null;
    }

    public boolean delete(String tblName, String idAttr, int id) {
        tblName += ".xml";
        XMLNode tbl = mData.get(tblName);
        if (tbl == null) {
            return false;
        }
        for (XMLNode ret : tbl.getChildren("row")) {
            if (id == Integer.parseInt(ret.getAttr(idAttr))) {
                tbl.remove(ret);
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("serial")
    public class ResultSet extends Vector<XMLNode> {

    }

}
