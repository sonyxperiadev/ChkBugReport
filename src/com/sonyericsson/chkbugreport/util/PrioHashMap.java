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

import java.util.HashMap;

public class PrioHashMap<K,V> {

    private static class Value<V> {
        int prio;
        V value;
    }

    private HashMap<K, Value<V>> mMap = new HashMap<K, Value<V>>();

    public boolean put(K key, V value, int prio) {
        if (value == null) return false;
        Value<V> v = mMap.get(key);
        if (v == null) {
            v = new Value<V>();
            v.prio = prio;
            v.value = value;
            mMap.put(key, v);
            return true;
        } else if (prio > v.prio) {
            v.prio = prio;
            v.value = value;
            mMap.put(key, v);
            return true;
        }
        return false;
    }

    public V get(K key) {
        Value<V> v = mMap.get(key);
        if (v == null) {
            return null;
        }
        return v.value;
    }

}
