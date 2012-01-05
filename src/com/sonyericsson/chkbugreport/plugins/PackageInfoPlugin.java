package com.sonyericsson.chkbugreport.plugins;

import java.util.HashMap;
import java.util.Vector;

import com.sonyericsson.chkbugreport.Chapter;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.Report;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.SectionInputStream;
import com.sonyericsson.chkbugreport.util.TableGen;
import com.sonyericsson.chkbugreport.util.XMLNode;

public class PackageInfoPlugin extends Plugin {

    private static final String TAG = "PackageInfoPlugin";

    private Chapter mCh;
    private XMLNode mPackagesXml;
    private HashMap<Integer, UID> mUIDs = new HashMap<Integer, PackageInfoPlugin.UID>();
    private HashMap<String, Package> mPackages = new HashMap<String, PackageInfoPlugin.Package>();

    public class Package {
        private int mId;
        private String mName;
        private String mPath;
        private int mFlags;
        private UID mUID;

        public Package(int id, String pkg, String path, int flags, UID uidObj) {
            mId = id;
            mName = pkg;
            mPath = path;
            mFlags = flags;
            mUID = uidObj;
            uidObj.add(this);
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

        public int getFlags() {
            return mFlags;
        }

        public UID getUid() {
            return mUID;
        }

    }

    public class UID {
        private int mUid;
        private Vector<Package> mPackages = new Vector<PackageInfoPlugin.Package>();

        public UID(int uid) {
            mUid = uid;
        }

        public void add(Package pkg) {
            mPackages.add(pkg);
        }

        public int getUid() {
            return mUid;
        }

        public int getPackageCount() {
            return mPackages.size();
        }

        public Package getPackage(int idx) {
            return mPackages.get(idx);
        }
    }

    @Override
    public int getPrio() {
        return 1; // Load data ASAP
    }

    @Override
    public void load(Report br) {
        // reset
        mCh = null;
        mPackagesXml = null;

        // Load packages.xml
        Section s = br.findSection(Section.PACKAGE_SETTINGS);
        if (s == null) {
            br.printErr(TAG + "Cannot find section: " + Section.PACKAGE_SETTINGS);
        } else {
            SectionInputStream is = new SectionInputStream(s);
            mPackagesXml = XMLNode.parse(is);

            // Parse XML
            int pkgId = 0;
            for (int i = 0; i < mPackagesXml.getChildCount(); i++) {
                XMLNode child = mPackagesXml.getChild(i);
                if (!"package".equals(child.getName())) continue;

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
                Package pkgObj = new Package(++pkgId, pkg, path, flags, uidObj);
                mPackages.put(pkg, pkgObj);
            }
        }
    }

    @Override
    public void generate(Report br) {
        mCh = new Chapter(br, "Package info");

        // Generate some child chaters
        generatePackageList(br, mCh);

        br.addChapter(mCh);
    }

    private void generatePackageList(Report br, Chapter ch) {

        ch.addLine("<p>Installed packages:</p>");

        TableGen tg = new TableGen(ch, TableGen.FLAG_SORT);
        tg.addColumn("Package", TableGen.FLAG_NONE);
        tg.addColumn("Path", TableGen.FLAG_NONE);
        tg.addColumn("UID", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Flags", TableGen.FLAG_ALIGN_RIGHT);
        tg.begin();

        for (Package pkg : mPackages.values()) {
            tg.addData(pkg.getName());
            tg.addData(pkg.getPath());
            tg.addData(Integer.toString(pkg.getUid().getUid()));
            tg.addData(Integer.toHexString(pkg.getFlags()));
        }

        tg.end();
    }

    private UID getUID(int uid, boolean createIfNeeded) {
        UID ret = mUIDs.get(uid);
        if (ret == null && createIfNeeded) {
            ret = new UID(uid);
            mUIDs.put(uid, ret);
        }
        return ret;
    }

}
