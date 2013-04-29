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
package com.sonyericsson.chkbugreport.util.db;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Vector;

abstract public class DbBackedData<T> {

    private Vector<T> mData = new Vector<T>();
    private Connection mConn;
    private String mTblName;
    private Statement mStmt;
    private Vector<Field> mFields = new Vector<Field>();
    private Vector<DBField> mDBFields = new Vector<DBField>();
    private PreparedStatement mInsert;

    public DbBackedData(Connection conn, String tblName) {
        mConn = conn;
        mTblName = tblName;

        if (mConn == null) {
            // No DB, no data to restore
            return;
        }

        try {
            mStmt = mConn.createStatement();

            // Create table if does not exists yet
            boolean first = true;
            StringBuilder sb = new StringBuilder();
            sb.append("create table if not exists " + mTblName + "(");
            // hack to get generic class type
            ParameterizedType superClass = (ParameterizedType) getClass().getGenericSuperclass();
            @SuppressWarnings("unchecked")
            Class<T> cls = (Class<T>) superClass.getActualTypeArguments()[0];
            for (Field f : cls.getDeclaredFields()) {
                f.setAccessible(true);
                DBField descr = f.getAnnotation(DBField.class);
                if (descr != null) {
                    if (!first) {
                        sb.append(", ");
                    }
                    sb.append(f.getName());
                    sb.append(" ");
                    sb.append(descr.type().sqldecl());
                    mFields.add(f);
                    mDBFields.add(descr);
                    if (descr.type() == DBField.Type.ID && !first) {
                        throw new RuntimeException("Only first field can be ID!");
                    } else if (descr.type() != DBField.Type.ID && first) {
                        throw new RuntimeException("First declared field must be the ID!");
                    }
                    first = false;
                }
            }
            sb.append(")");
            mStmt.execute(sb.toString());

            // Create prepared statement for insertion
            sb = new StringBuilder();
            sb.append("insert into ");
            sb.append(mTblName);
            sb.append("(");
            for (int i = 1 /* skip ID */; i < mFields.size(); i++) {
                if (i > 1) {
                    sb.append(',');
                }
                sb.append(mFields.get(i).getName());
            }
            sb.append(") values (");
            for (int i = 1 /* skip ID */; i < mFields.size(); i++) {
                if (i > 1) {
                    sb.append(',');
                }
                sb.append('?');
            }
            sb.append(")");
            mInsert = mConn.prepareStatement(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            // Create table if does not exists yet
            StringBuilder sb = new StringBuilder();
            sb.append("select * from " + mTblName);
            if (field != null && value != null) {
                sb.append(" where " + field + " == " + value);
            }
            if (mStmt.execute(sb.toString())) {
                ResultSet res = mStmt.getResultSet();
                while (res.next()) {
                    T item = createItem();
                    for (int i = 0; i < mFields.size(); i++) {
                        Field f = mFields.get(i);
                        switch (mDBFields.get(i).type()) {
                        case ID:
                        case LINK:
                        case INT:
                            f.setInt(item, res.getInt(i+1));
                            break;
                        case VARCHAR:
                            f.set(item, res.getString(i+1));
                            break;
                        }
                    }
                    addImpl(item);
                    onLoaded(item);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void add(T item) {
        try {
            // Need to save to database as well
            for (int i = 1 /* skip ID */; i < mFields.size(); i++) {
                Field f = mFields.get(i);
                switch (mDBFields.get(i).type()) {
                case LINK:
                case INT:
                    mInsert.setInt(i, f.getInt(item));
                    break;
                case VARCHAR:
                    mInsert.setString(i, (String)f.get(item));
                    break;
                }
            }
            if (mInsert.executeUpdate() == 1) {
                addImpl(item);
                int id = mInsert.getGeneratedKeys().getInt(1);
                mFields.get(0).setInt(item, id);
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

    public Connection getConnection() {
        return mConn;
    }

    public String getTableName() {
        return mTblName;
    }

}
