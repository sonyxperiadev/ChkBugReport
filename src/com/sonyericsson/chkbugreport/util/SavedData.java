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

import com.sonyericsson.chkbugreport.util.SaveFile.ResultSet;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Iterator;
import java.util.Vector;

abstract public class SavedData<T> implements Iterable<T> {

    private Vector<T> mData = new Vector<T>();
    private SaveFile mSaveFile;
    private String mTblName;
    private Vector<Field> mFields = new Vector<Field>();
    private Vector<SavedField> mDBFields = new Vector<SavedField>();

    public SavedData(SaveFile conn, String tblName) {
        mSaveFile = conn;
        mTblName = tblName;

        if (mSaveFile == null) {
            // No DB, no data to restore
            return;
        }

        // Scan fields
        boolean first = true;
        // hack to get generic class type
        ParameterizedType superClass = (ParameterizedType) getClass().getGenericSuperclass();
        @SuppressWarnings("unchecked")
        Class<T> cls = (Class<T>) superClass.getActualTypeArguments()[0];
        for (Field f : cls.getDeclaredFields()) {
            f.setAccessible(true);
            SavedField descr = f.getAnnotation(SavedField.class);
            if (descr != null) {
                mFields.add(f);
                mDBFields.add(descr);
                if (descr.type() == SavedField.Type.ID && !first) {
                    throw new RuntimeException("Only first field can be ID!");
                } else if (descr.type() != SavedField.Type.ID && first) {
                    throw new RuntimeException("First declared field must be the ID!");
                }
                first = false;
            }
        }
    }

    @Override
    public Iterator<T> iterator() {
        return mData.iterator();
    }

    final protected Vector<T> getData() {
        return mData;
    }

    public void load() {
        load(null, null);
    }

    public void load(String field, long value) {
        load(field, Long.toString(value));
    }

    public void load(String field, String value) {
        try {
            ResultSet res = mSaveFile.select(mTblName);
            for (XMLNode node : res) {
                if (field != null && value != null) {
                    if (!value.equals(node.getAttr(field))) {
                        continue;
                    }
                }
                T item = createItem();
                for (int i = 0; i < mFields.size(); i++) {
                    Field f = mFields.get(i);
                    switch (mDBFields.get(i).type()) {
                    case ID:
                    case LINK:
                    case INT:
                        setLongField(f, item, node);
                        break;
                    case VARCHAR:
                        setStringField(f, item, node);
                        break;
                    }
                }
                addImpl(item);
                onLoaded(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setLongField(Field f, T item, XMLNode node) throws Exception {
        String val = node.getAttr(f.getName());
        if (val != null) {
            if (f.getType().getName().equals("int")) {
                f.setInt(item, Integer.parseInt(val));
            } else {
                f.setLong(item, Long.parseLong(val));
            }
        }
    }

    private void setStringField(Field f, T item, XMLNode node) throws Exception {
        String val = node.getAttr(f.getName());
        if (val != null) {
            if (f.getType().isEnum()) {
                // Special case: need to convert the string to enum
                Object objVal = f.getType().getMethod("valueOf", String.class).invoke(null, val);
                f.set(item, objVal);
            } else {
                f.set(item, val);
            }
        }
    }

    private void setFromField(XMLNode node, T item, int i) throws IllegalAccessException {
        Field f = mFields.get(i);
        switch (mDBFields.get(i).type()) {
        case ID:
        case LINK:
        case INT:
            node.addAttr(f.getName(), Long.toString(f.getLong(item)));
            break;
        case VARCHAR:
            node.addAttr(f.getName(), f.get(item).toString());
            break;
        }
    }

    public void add(T item) {
        try {
            // Need to save to database as well
            XMLNode node = mSaveFile.insert(mTblName, mFields.get(0).getName());
            for (int i = 1 /* skip ID */; i < mFields.size(); i++) {
                setFromField(node, item, i);
            }
            Field fId = mFields.get(0);
            setLongField(fId, item, node);
            mSaveFile.commit(mTblName);
            addImpl(item);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void update(T item) {
        if (!mData.contains(item)) return; // quick sanity check
        try {
            // Need to save to database as well
            XMLNode node = mSaveFile.findById(mTblName, mFields.get(0).getName(), mFields.get(0).getInt(item));
            if (node != null) {
                for (int i = 1 /* skip ID */; i < mFields.size(); i++) {
                    setFromField(node, item, i);
                }
                mSaveFile.commit(mTblName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void delete(T item) {
        try {
            if (mData.remove(item)) {
                mSaveFile.delete(mTblName, mFields.get(0).getName(), mFields.get(0).getInt(item));
                mSaveFile.commit(mTblName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    abstract protected T createItem();

    protected void onLoaded(T item) {
        // NOP
    }

    private void addImpl(T item) {
        mData.add(item);
    }

    public SaveFile getSaveFile() {
        return mSaveFile;
    }

    public String getTableName() {
        return mTblName;
    }

}
