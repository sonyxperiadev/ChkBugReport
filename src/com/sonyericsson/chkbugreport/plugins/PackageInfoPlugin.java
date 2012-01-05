package com.sonyericsson.chkbugreport.plugins;

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
    private XMLNode mPackages;

    @Override
    public int getPrio() {
        return 1; // Load data ASAP
    }

    @Override
    public void load(Report br) {
        // reset
        mCh = null;
        mPackages = null;

        // Load packages.xml
        Section s = br.findSection(Section.PACKAGE_SETTINGS);
        if (s == null) {
            br.printErr(TAG + "Cannot find section: " + Section.PACKAGE_SETTINGS);
        } else {
            SectionInputStream is = new SectionInputStream(s);
            mPackages = XMLNode.parse(is);
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
        if (mPackages == null) return;

        ch.addLine("<p>Installed packages:</p>");

        TableGen tg = new TableGen(ch, TableGen.FLAG_SORT);
        tg.addColumn("Package", TableGen.FLAG_NONE);
        tg.addColumn("Path", TableGen.FLAG_NONE);
        tg.addColumn("UID", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Flags", TableGen.FLAG_ALIGN_RIGHT);
        tg.begin();

        for (int i = 0; i < mPackages.getChildCount(); i++) {
            XMLNode child = mPackages.getChild(i);
            if (!"package".equals(child.getName())) continue;

            String pkg = child.getAttr("name");
            String path = child.getAttr("codePath");
            String sUid = child.getAttr("userId");
            if (sUid == null) {
                sUid = child.getAttr("sharedUserId");
            }
            // int uid = Integer.parseInt(sUid);
            String sFlags = child.getAttr("flags");
            // int flags = (sFlags == null) ? 0 : Integer.parseInt(sFlags);

            tg.addData(pkg);
            tg.addData(path);
            tg.addData(sUid);
            tg.addData(sFlags);
        }

        tg.end();
    }

}
