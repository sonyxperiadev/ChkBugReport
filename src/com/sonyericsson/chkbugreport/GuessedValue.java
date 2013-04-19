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
package com.sonyericsson.chkbugreport;

/**
 * Stores a value which is guessed. Whenever somebody is setting this value, it must
 * also specify the certainty. The value with the highest certainty wins.
 */
public class GuessedValue<T> {

    private T mValue;
    private int mCertainty = 0;

    public GuessedValue(T defValue) {
        mValue = defValue;
    }

    public boolean set(T value, int certainty) {
        if (certainty > mCertainty) {
            mValue = value;
            mCertainty = certainty;
            return true;
        }
        return false;
    }

    public T get() {
        return mValue;
    }

    public int getCertainty() {
        return mCertainty;
    }

}
