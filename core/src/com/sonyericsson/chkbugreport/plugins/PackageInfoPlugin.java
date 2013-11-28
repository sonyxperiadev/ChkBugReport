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
package com.sonyericsson.chkbugreport.plugins;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.doc.Anchor;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.doc.HtmlNode;
import com.sonyericsson.chkbugreport.doc.Link;
import com.sonyericsson.chkbugreport.doc.List;
import com.sonyericsson.chkbugreport.doc.Para;
import com.sonyericsson.chkbugreport.doc.PreText;
import com.sonyericsson.chkbugreport.doc.Table;
import com.sonyericsson.chkbugreport.util.XMLNode;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

public class PackageInfoPlugin extends Plugin {

    private static final String TAG = "[PackageInfoPlugin]";

    private Chapter mCh;
    private Chapter mChPackages;
    private Chapter mChUids;
    private Chapter mChPermissions;
    private XMLNode mPackagesXml;
    private HashMap<Integer, UID> mUIDs = new HashMap<Integer, PackageInfoPlugin.UID>();
    private HashMap<String, PackageInfo> mPackages = new HashMap<String, PackageInfoPlugin.PackageInfo>();
    private HashMap<String, Vector<UID>> mPermissions = new HashMap<String, Vector<UID>>();

    @SuppressWarnings("serial")
    public class Permissions extends Vector<String> {
    }

    public class PackageInfo {
        private int mId;
        private String mName;
        private String mPath;
        private String mOrigPath;
        private int mFlags;
        private UID mUID;
        private Permissions mPermissions = new Permissions();

        public PackageInfo(int id, String pkg, String path, int flags, UID uidObj) {
            mId = id;
            mName = pkg;
            mPath = path;
            mOrigPath = null;
            mFlags = flags;
            mUID = uidObj;
            uidObj.add(this);
        }

        public Permissions getPermissions() {
            return mPermissions;
        }

        public int getId() {
            return mId;
        }

        public String getName() {
            return mName;
        }

        public String getPath() {
            return mPath;
        }

        public String getOrigPath() {
            return mOrigPath;
        }

        void setOrigPath(String origPath) {
            mOrigPath = origPath;
        }

        public int getFlags() {
            return mFlags;
        }

        public UID getUid() {
            return mUID;
        }

        public void dumpInfo(DocNode out) {
            DocNode box = new PreText(out).addStyle("box");
            box.addln("Name:        " + mName);
            box.addln("Path:        " + mPath);
            box.addln("OrigPath:    " + mOrigPath);
            box.addln("Flags:       " + "0x" + Integer.toHexString(mFlags));
            box.addln("Permissions: ");
            for (String perm : mPermissions) {
                box.addln("             " + perm);
            }
        }

    }

    public class UID {
        private int mUid;
        private Vector<PackageInfo> mPackages = new Vector<PackageInfoPlugin.PackageInfo>();
        private Permissions mPermissions = new Permissions();
        private String mName;
        private Chapter mChapter;

        public UID(int uid) {
            mUid = uid;
        }

        public Permissions getPermissions() {
            return mPermissions;
        }

        public void add(PackageInfo pkg) {
            mPackages.add(pkg);
            if (mName == null) {
                mName = pkg.getName();
            }
        }

        public String getName() {
            return mName;
        }

        public int getUid() {
            return mUid;
        }

        public int getPackageCount() {
            return mPackages.size();
        }

        public PackageInfo getPackage(int idx) {
            return mPackages.get(idx);
        }

        public String getFullName() {
            if (mName == null) {
                return Integer.toString(mUid);
            } else {
                return mName + "(" + Integer.toString(mUid) + ")";
            }
        }

        void setName(String name) {
            mName = name;
        }

        void setChapter(Chapter cch) {
            mChapter = cch;
        }

        Chapter getChapter() {
            return mChapter;
        }
    }

    @Override
    public int getPrio() {
        return 1; // Load data ASAP
    }

    @Override
    public void reset() {
        // reset
        mCh = null;
        mChPackages = null;
        mChUids = null;
        mChPermissions = null;
        mPackagesXml = null;
        mUIDs.clear();
        mPackages.clear();
        mPermissions.clear();
    }

    @Override
    public void load(Module br) {
        // Load packages.xml
        Section s = br.findSection(Section.PACKAGE_SETTINGS);
        if (s == null) {
            br.printErr(3, TAG + "Cannot find section: " + Section.PACKAGE_SETTINGS);
            return;
        }

        if (s.getLineCount() == 0 || s.getLine(0).startsWith("***")) {
            br.printErr(4, TAG + "Cannot parse section: " + Section.PACKAGE_SETTINGS);
            return;
        }

        // Need some cleanup in the section, removing the final empty and "[...]" lines
        int l = s.getLineCount();
        while (l > 0) {
            String line = s.getLine(l - 1);
            if (line.length() == 0 || (line.startsWith("[") && line.endsWith("]"))) {
                s.removeLine(--l);
            } else {
                break;
            }
        }

        InputStream is = s.createInputStream();
        mPackagesXml = XMLNode.parse(is);
        mCh = new Chapter(br.getContext(), "Package info");
        br.addChapter(mCh);
        mChPackages = new Chapter(br.getContext(), "Packages");
        mCh.addChapter(mChPackages);
        mChUids = new Chapter(br.getContext(), "UserIDs");
        mCh.addChapter(mChUids);
        mChPermissions = new Chapter(br.getContext(), "Permissions");
        mCh.addChapter(mChPermissions);

        // Create the UID for the kernel/root manually
        getUID(0, true).setName("kernel/root");

        // Parse XML for shared-user tags
        for (XMLNode child : mPackagesXml.getChildren("shared-user")) {
            String name = child.getAttr("name");
            String sUid = child.getAttr("userId");
            int uid = Integer.parseInt(sUid);

            UID uidObj = getUID(uid, true);
            uidObj.setName(name);
            collectPermissions(uidObj.getPermissions(), uidObj, child);
        }

        // Parse XML for packages
        int pkgId = 0;
        for (XMLNode child : mPackagesXml.getChildren("package")) {
            String pkg = child.getAttr("name");
            String path = child.getAttr("codePath");
            String sUid = child.getAttr("userId");
            if (sUid == null) {
                sUid = child.getAttr("sharedUserId");
            }
            int uid = Integer.parseInt(sUid);
            String sFlags = child.getAttr("flags");
            int flags = (sFlags == null) ? 0 : Integer.parseInt(sFlags);

            UID uidObj = getUID(uid, true);
            PackageInfo pkgObj = new PackageInfo(++pkgId, pkg, path, flags, uidObj);
            mPackages.put(pkg, pkgObj);

            collectPermissions(pkgObj.getPermissions(), uidObj, child);
        }

        // Parse XML for updated-package tags
        for (XMLNode child : mPackagesXml.getChildren("updated-package")) {
            String pkg = child.getAttr("name");
            String path = child.getAttr("codePath");

            PackageInfo pkgObj = mPackages.get(pkg);
            if (pkgObj != null) {
                pkgObj.setOrigPath(path);
                collectPermissions(pkgObj.getPermissions(), pkgObj.getUid(), child);
            } else {
                System.err.println("Could not find package for updated-package item: " + pkg);
            }
        }

        // Create a chapter to each UID, so we can link to it from other plugins
        for (UID uid : mUIDs.values()) {
            Chapter cch = new Chapter(br.getContext(), uid.getFullName());
            uid.setChapter(cch);
            mChUids.addChapter(cch);
        }
    }

    private void collectPermissions(Permissions perm, UID uid, XMLNode pkgNode) {
        XMLNode node = pkgNode.getChild("perms");
        if (node != null) {
            for (XMLNode item : node.getChildren("item")) {
                String permission = item.getAttr("name");
                perm.add(permission);

                // Store each UID who has this permission
                Vector<UID> uids = mPermissions.get(permission);
                if (uids == null) {
                    uids = new Vector<PackageInfoPlugin.UID>();
                    mPermissions.put(permission, uids);
                }
                if (!uids.contains(uid)) {
                    uids.add(uid);
                }
            }
            Collections.sort(perm);
        }
    }

    @Override
    public void generate(Module br) {
        if (mChPackages != null) {
            generatePackageList(br, mChPackages);
        }
        if (mChPermissions != null) {
            generatePermissionList(br, mChPermissions);
        }
    }

    private void generatePermissionList(Module br, Chapter ch) {
        Vector<String> tmp = new Vector<String>();
        for (String s : mPermissions.keySet()) {
            tmp.add(s);
        }
        Collections.sort(tmp);

        for (String s : tmp) {
            new HtmlNode("h2", ch).add(s);
            List list = new List(List.TYPE_UNORDERED, ch);
            Vector<UID> uids = mPermissions.get(s);
            for (UID uid : uids) {
                int cnt = uid.getPackageCount();
                if (cnt == 0) {
                    list.add(uid.getFullName());
                } else {
                    for (int i = 0; i < cnt; i++) {
                        PackageInfo pkg = uid.getPackage(i);
                        list.add(uid.getFullName() + ": " + pkg.getName());
                    }
                }
            }
        }

    }

    private void generatePackageList(Module br, Chapter ch) {

        // Create a chapter for each user id
        Vector<UID> tmp = new Vector<PackageInfoPlugin.UID>();
        for (UID uid : mUIDs.values()) {
            tmp.add(uid);
        }
        Collections.sort(tmp, new Comparator<UID>() {
            @Override
            public int compare(UID o1, UID o2) {
                return o1.mUid - o2.mUid;
            }
        });
        for (UID uid : tmp) {
            Chapter cch = uid.getChapter();

            new Para(cch).add("Packages:");
            int cnt = uid.getPackageCount();
            for (int i = 0; i < cnt; i++) {
                PackageInfo pkg = uid.getPackage(i);
                pkg.dumpInfo(cch);
            }

            new Para(cch).add("Permissions:");
            DocNode permList = new PreText(cch).addStyle("box");
            for (String perm : uid.getPermissions()) {
                permList.addln("             " + perm);
            }
        }

        // Create a ToC for the packages
        new Para(ch).add("Installed packages:");

        Table tg = new Table(Table.FLAG_SORT, ch);
        tg.setCSVOutput(br, "package_list");
        tg.setTableName(br, "package_list");
        tg.addColumn("Package", null, Table.FLAG_NONE, "pkg varchar");
        tg.addColumn("Path", null, Table.FLAG_NONE, "path varchar");
        tg.addColumn("UID", null, Table.FLAG_ALIGN_RIGHT, "uid int");
        tg.addColumn("Flags", null, Table.FLAG_ALIGN_RIGHT, "flags int");
        tg.begin();

        for (PackageInfo pkg : mPackages.values()) {
            Anchor a = getAnchorToUid(pkg.getUid());
            tg.addData(new Link(a, pkg.getName()));
            tg.addData(pkg.getPath());
            tg.addData(Integer.toString(pkg.getUid().getUid()));
            tg.addData(Integer.toHexString(pkg.getFlags()));
        }

        tg.end();
    }

    public Anchor getAnchorToUid(UID uid) {
        if (uid == null || uid.getChapter() == null) {
            return null;
        }
        return uid.getChapter().getAnchor();
    }

    public UID getUID(int uid) {
        return getUID(uid, false);
    }

    private UID getUID(int uid, boolean createIfNeeded) {
        UID ret = mUIDs.get(uid);
        if (ret == null && createIfNeeded) {
            ret = new UID(uid);
            mUIDs.put(uid, ret);
        }
        return ret;
    }

    public boolean isEmpty() {
        return mPackages.isEmpty();
    }

    public Collection<PackageInfo> getPackages() {
        return mPackages.values();
    }

}
