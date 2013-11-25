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
package com.sonyericsson.chkbugreport.util;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class XMLNode implements Iterable<XMLNode> {

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

    public XMLNode getChild(String name) {
        for (XMLNode child : mChildren) {
            if (name.equals(child.getName())) {
                return child;
            }
        }
        return null;
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
        XMLNodeHandler handler = new XMLNodeHandler();
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(is, handler);
            return handler.mRoot;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Iterator<XMLNode> iterator() {
        return mChildren.iterator();
    }

    public Iterable<XMLNode> getChildren(String name) {
        return new ChildNameFilterIterator(name);
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
                node.addAttr(attributes.getQName(i), unescape(attributes.getValue(i)));
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
                node.addAttr("text", unescape(new String(ch, start, length)));
                mCur.add(node);
            }
        }

    }

    class ChildNameFilterIterator implements Iterable<XMLNode>, Iterator<XMLNode> {

        private int mIdx;
        private int mCount;

        public ChildNameFilterIterator(String name) {
            mName = name;
            mIdx = -1;
            mCount = getChildCount();
        }

        @Override
        public Iterator<XMLNode> iterator() {
            return this;
        }

        @Override
        public boolean hasNext() {
            for (int i = mIdx + 1; i < mCount; i++) {
                XMLNode child = getChild(i);
                if (mName.equals(child.getName())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public XMLNode next() {
            do {
                if (++mIdx >= mCount) {
                    return null;
                }
                XMLNode child = getChild(mIdx);
                if (mName.equals(child.getName())) {
                    return child;
                }
            } while (true);
        }

        @Override
        public void remove() {
            throw new RuntimeException("ChildNameFilterIterator.remove() is not implemented yet!");
        }

    }

    public void dump(OutputStream out) {
        PrintStream ps = new PrintStream(out);
        dump(ps, "");
    }

    private void dump(PrintStream ps, String indent) {
        ps.print(indent);
        ps.print('<');
        ps.print(mName);
        for (Entry<String, String> attr : mAttrs.entrySet()) {
            ps.print(' ');
            ps.print(attr.getKey());
            ps.print("=\"");
            ps.print(escape(attr.getValue()));
            ps.print("\"");
        }
        if (mChildren.isEmpty()) {
            ps.println(" />");
        } else {
            ps.println(" >");
            String indent2 = indent + "  ";
            for (XMLNode child : mChildren) {
                if (child.getName() != null) {
                    child.dump(ps, indent2);
                }
            }
            ps.print(indent);
            ps.print("</");
            ps.print(mName);
            ps.println(">");
        }
    }

    private static String escape(String str) {
        return HtmlUtil.escape(str);
    }

    private static String unescape(String str) {
        return HtmlUtil.unescape(str);
    }

}
