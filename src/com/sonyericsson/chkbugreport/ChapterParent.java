package com.sonyericsson.chkbugreport;

import com.sonyericsson.chkbugreport.doc.Chapter;

public interface ChapterParent {

    void addChapter(Chapter ch);

    int getChapterCount();

    Chapter getChapter(int idx);

    Chapter getChapter(String s);

}
