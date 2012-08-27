package com.sonyericsson.chkbugreport;

public class GuessedValue<T> {

    private T mValue;
    private int mCertainty = 0;

    public GuessedValue(T defValue) {
        mValue = defValue;
    }

    public boolean set(T value, int certainty) {
        if (certainty > mCertainty) {
            mValue = value;
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
