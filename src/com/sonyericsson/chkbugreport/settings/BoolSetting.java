package com.sonyericsson.chkbugreport.settings;

import java.util.Properties;

public class BoolSetting extends Setting {

    private boolean mValue;

    public BoolSetting(boolean defValue, Settings owner, String id, String descr) {
        super(owner, id, descr);
        mValue = defValue;
    }

    public boolean get() {
        return mValue;
    }

    public void set(boolean b) {
        mValue = b;
    }

    @Override
    public void load(Properties props) {
        String value = props.getProperty(getId());
        if (value != null) {
            if ("true".equals(value)) {
                mValue = true;
            }
            if ("false".equals(value)) {
                mValue = false;
            }
        }
    }

    @Override
    public void store(Properties props) {
        props.setProperty(getId(), mValue ? "true" : "false");
    }

}
