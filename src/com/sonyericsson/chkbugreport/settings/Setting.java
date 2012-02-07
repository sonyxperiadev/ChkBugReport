package com.sonyericsson.chkbugreport.settings;

import java.util.Properties;

public abstract class Setting {

    private Settings mOwner;
    private String mId;
    private String mDescr;

    public Setting(Settings owner, String id, String descr) {
        mOwner = owner;
        mId = id;
        mDescr = descr;
        owner.add(this);
    }

    public Settings getOwner() {
        return mOwner;
    }

    public String getId() {
        return mId;
    }

    public String getDescription() {
        return mDescr;
    }

    abstract public void load(Properties props);

    abstract public void store(Properties props);

}
