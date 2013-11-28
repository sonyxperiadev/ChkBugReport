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
package com.sonyericsson.chkbugreport.doc;

import com.sonyericsson.chkbugreport.Context;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;

/**
 * A Chapter represents an output file unit.
 * The Chapters are organized in a tree structure, and they have a header, with some
 * utility plugins (for example to show extra help, or to "pop-out" the chapter into a separate
 * window).
 */
public class Chapter extends DocNode implements ChapterParent {

    /** Child chapters. */
    private Vector<Chapter> mSubChapters = new Vector<Chapter>();
    /** The chapter's title. This will be shown both in the TOC and in the header. */
    private SimpleText mName;
    /** The chapter's icon. This will be shown in the TOC, if specified. */
    private Icon mIcon;
    /** Reference to the Context of the Module generating this chapter */
    private Context mContext;
    /** The output renderer responsible for this chapter */
    private Renderer mRenderer;
    /** The anchor of this chapter, so other parts of the output report can link to this chapter */
    private Anchor mAnchor;
    /** Sequenctially generated unique ID of this chapter (in order to generate unique file names) */
    private int mId;
    /** The pre-content part of the chapter, containing the header, popup button, other buttons, etc */
    private DocNode mInit;
    /** The Chapter's header object */
    private Header mHeader;
    /** The "pop-out" link in the header */
    private Link mPopout;
    /** Force chapter to be saved by itself in a file */
    private boolean mStandalone;

    /* package */ Chapter(Context ctx) {
        mContext = ctx;
    }

    public Chapter(Context ctx, String name) {
        this(ctx, name, null);
    }

    public Chapter(Context ctx, String name, Icon icon) {
        this(ctx);
        mName = new SimpleText(name);
        mIcon = icon;
        mInit = new DocNode(this);
        mInit.add(mAnchor = new Anchor("ch" + mId));
        mPopout = new Link(mAnchor, null);
        mPopout.add(new Img("ic_pop_out.png"));
        mPopout.setTarget("_blank");
        mInit.add(new Block().addStyle("btn-pop-out").add(mPopout));
        mInit.add(mHeader = new Header(mName));
        mId = ctx.allocChapterId();
    }

    public void addCustomHeaderView(DocNode customView) {
        mInit.addBefore(new Block().addStyle("header-custom-view").add(customView), mHeader);
    }

    public void addButton(String link, String img, String style, boolean newWindow) {
        if (style == null) {
            style = "btn-header";
        }
        Link btn = new Link(link, null);
        btn.add(new Img(img));
        if (newWindow) {
            btn.setTarget("_blank");
        }
        mInit.addBefore(new Block().addStyle(style).add(btn), mHeader);
    }

    public void removePopout() {
        mInit.remove(mPopout.getParent());
    }

    public void addHelp(String text) {
        addHelp(new SimpleText(text));
    }

    public void addHelp(DocNode node) {
        // Add the button to show the dialog
        addButton("javascript:$('#dialog').dialog()", "ic_help.png", null, false);
        // Add the dialog content
        Block b = new Block(mInit);
        b.setId("dialog");
        b.addStyle("dialog");
        b.add(node);
    }

    public Context getContext() {
        return mContext;
    }

    public Anchor getAnchor() {
        return mAnchor;
    }

    public String getFileName() {
        return mRenderer.getFileName();
    }

    public String getName() {
        return mName == null ? null : mName.getText();
    }

    public SimpleText getNameNode() {
        return mName;
    }

    public void setName(String name) {
        mName.setText(name);
    }

    public void setNameFlags(int flags, boolean set) {
        mName.setFlags(flags, set);
    }

    public Icon getIcon() {
        return mIcon;
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
        // Generate sub-toc
        mRenderer = r.addLevel(this);
        if (mRenderer.isStandalone() && getChapterCount() > 0) {
            List list = new List(List.TYPE_UNORDERED);
            for (Chapter child : mSubChapters) {
                Link link = new Link(child.getAnchor(), null);
                if (child.getIcon() != null) {
                    link.add(child.getIcon());
                }
                link.add(child.getNameNode().copy());
                if (child instanceof WebOnlyChapter) {
                    new Span(list).addStyle("ws").add(link);
                } else {
                    list.add(link);
                }
            }
            new Accordion(this, Accordion.FLAG_COLLAPSE).add(new SimpleText("Jump to:"), list);
        }
        // Do actual prepare
        super.prepare(mRenderer);
        for (Chapter child : mSubChapters) {
            child.prepare(mRenderer);
        }
    }

    /**
     * Forces the chapter to be rendered in separate file
     */
    public void setStandalone(boolean standalone) {
        mStandalone = standalone;
    }

    /**
     * Returns true of the chapter is forced to be rendered in separate file.
     * Note: if this method returns false, the chapter might still be rendered
     * in separate file, depending on it's level in the document tree!
     */
    public boolean shouldBeStandalone() {
        return mStandalone;
    }

    @Override
    public void render(Renderer r) throws IOException {
        mContext.printOut(2, "Writing chapter: " + getFullName() + "...");
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
        Chapter parent = (getParent() instanceof Chapter) ? (Chapter)getParent() : null;
        if (parent != null) {
            String ret = parent.getFullName();
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

    public void sort() {
        // Now sort by name
        Collections.sort(mSubChapters, new Comparator<Chapter>(){
            @Override
            public int compare(Chapter o1, Chapter o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
    }

}
