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
package com.sonyericsson.chkbugreport.doc;

import com.sonyericsson.chkbugreport.Module;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;

public class Chapter extends DocNode implements ChapterParent {

    private Vector<Chapter> mSubChapters = new Vector<Chapter>();
    private Chapter mParent = null;
    private String mName;
    private Icon mIcon;
    private Renderer mRenderer;
    private Module mMod;
    private Anchor mAnchor;
    private int mId;
    private Header mHeader;
    private DocNode mInit;
    private Link mPopout;

    /* package */ Chapter(Module mod) {
        mMod = mod;
    }

    public Chapter(Module mod, String name) {
        this(mod, name, null);
    }

    public Chapter(Module mod, String name, Icon icon) {
        this(mod);
        mName = name;
        mIcon = icon;
        mInit = new DocNode(this);
        mInit.add(mAnchor = new Anchor("ch" + mId));
        mPopout = new Link(mAnchor, null);
        mPopout.add(new Img("ic_pop_out.png"));
        mPopout.setTarget("_blank");
        mInit.add(new Block().addStyle("btn-pop-out").add(mPopout));
        mInit.add(mHeader = new Header(mName));
        mId = mMod.getDocument().allocChapterId();
    }

    public void removePopout() {
        mInit.remove(mPopout.getParent());
    }

    public Module getModule() {
        return mMod;
    }

    public Anchor getAnchor() {
        return mAnchor;
    }

    public String getName() {
        return mName;
    }

    public Icon getIcon() {
        return mIcon;
    }

    public void setName(String name) {
        mName = name;
        mHeader.setName(name);
    }

    @Override
    public void addChapter(Chapter ch) {
        if (ch == null) {
            throw new NullPointerException();
        }
        mSubChapters.add(ch);
    }

    public void insertChapter(int pos, Chapter ch) {
        if (ch == null) {
            throw new NullPointerException();
        }
        mSubChapters.insertElementAt(ch, pos);
    }

    @Override
    public int getChapterCount() {
        return mSubChapters.size();
    }

    @Override
    public Chapter getChapter(int idx) {
        return mSubChapters.get(idx);
    }

    @Override
    public Chapter getChapter(String name) {
        for (Chapter ch : mSubChapters) {
            if (name.equals(ch.getName())) {
                return ch;
            }
        }
        return null;
    }

    @Override
    public void prepare(Renderer r) {
        mRenderer = r.addLevel(this);

        if (isStandalone() && getChapterCount() > 0) {
            List list = new List(List.TYPE_UNORDERED);
            for (Chapter child : mSubChapters) {
                Link link = new Link(child.getAnchor(), null);
                if (child.getIcon() != null) {
                    link.add(child.getIcon());
                }
                link.add(child.getName());
                list.add(link);
            }
            new Block(this).addStyle("box")
                .add("Jump to:")
                .add(list);
        }
        super.prepare(mRenderer);
        for (Chapter child : mSubChapters) {
            child.prepare(mRenderer);
        }
    }

    private boolean isStandalone() {
        return mRenderer.isStandalone();
    }

    @Override
    public void render(Renderer r) throws IOException {
        mMod.printOut(2, "Writing chapter: " + getFullName() + "...");
        mRenderer.begin();

        // This will render the own content of this chapter
        super.render(mRenderer);

        // This will render the subchapters
        for (Chapter child : mSubChapters) {
            child.render(mRenderer);
        }

        mRenderer.end();
    }

    public String getFullName() {
        if (mParent != null) {
            String ret = mParent.getFullName();
            if (ret != null) {
                return ret + "/" + getName();
            }
        }
        return getName();
    }

    @Override
    public boolean isEmpty() {
        // Note: there is a default child
        return mSubChapters.isEmpty() && getChildCount() <= 1;
    }

    public int getId() {
        return mId;
    }

    public void sort(Comparator<Chapter> comparator) {
        Collections.sort(mSubChapters, comparator);
    }

    public void cleanup() {
        Iterator<Chapter> i = mSubChapters.iterator();
        while (i.hasNext()) {
            Chapter subCh = i.next();
            // First cleanup recursively
            subCh.cleanup();
            // Then remove the subchapter if it's empty
            if (subCh.isEmpty()) {
                i.remove();
            }
        }
    }



}
