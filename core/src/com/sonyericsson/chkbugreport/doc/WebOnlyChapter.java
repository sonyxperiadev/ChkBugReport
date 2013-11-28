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
package com.sonyericsson.chkbugreport.doc;

import com.sonyericsson.chkbugreport.Context;

import java.io.IOException;

public class WebOnlyChapter extends Chapter {

    private Anchor mAnchor;

    public WebOnlyChapter(Context ctx, String name, final String webModule) {
        super(ctx, name);
        mAnchor = new Anchor(null) {
            @Override
            public String getHRef() {
                return webModule;
            }
        };
    }

    @Override
    public Anchor getAnchor() {
        return mAnchor;
    }

    @Override
    public void render(Renderer r) throws IOException {
        // NOP
    }

    @Override
    public void prepare(Renderer r) {
        // NOP
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public void cleanup() {
        // NOP
    }

}
