/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XMLNode {

    private String mName;
    private XMLNode mParent;
    private Vector<XMLNode> mChildren = new Vector<XMLNode>();
    private HashMap<String, String> mAttrs = new HashMap<String, String>();

    public XMLNode(String name) {
        mName = name;
    }

    public void add(XMLNode child) {
        if (child.mParent != null) {
            child.mParent.remove(child);
        }
        child.mParent = this;
        mChildren.add(child);
    }

    public void remove(XMLNode child) {
        if (child.mParent == this) {
            mChildren.remove(child);
            child.mParent = null;
        }
    }

    public String getName() {
        return mName;
    }

    public int getChildCount() {
        return mChildren.size();
    }

    public XMLNode getChild(int idx) {
        return mChildren.get(idx);
    }

    public XMLNode getParent() {
        return mParent;
    }

    public void addAttr(String key, String val) {
        mAttrs.put(key, val);
    }

    public String getAttr(String key) {
        return mAttrs.get(key);
    }

    public static XMLNode parse(InputStream is) {
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            XMLNodeHandler handler = new XMLNodeHandler();
            parser.parse(is, handler);
            return handler.mRoot;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static class XMLNodeHandler extends DefaultHandler {

        public XMLNode mRoot;
        public XMLNode mCur;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            XMLNode node = new XMLNode(qName);
            if (mRoot == null) {
                mRoot = node;
            }
            if (mCur != null) {
                mCur.add(node);
            }
            mCur = node;
            for (int i = 0; i < attributes.getLength(); i++) {
                node.addAttr(attributes.getQName(i), attributes.getValue(i));
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (mCur != null) {
                mCur = mCur.getParent();
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (mCur != null) {
                XMLNode node = new XMLNode(null);
                node.addAttr("text", new String(ch, start, length));
                mCur.add(node);
            }
        }

    }

}
