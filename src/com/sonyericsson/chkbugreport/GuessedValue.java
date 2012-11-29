package com.sonyericsson.chkbugreport;

/**
 * Stores a value which is guessed. Whenever somebody is setting this value, it must
 * also specify the certainty. The value with the highest certainty wins.
 */
/* package */ class GuessedValue<T> {

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
