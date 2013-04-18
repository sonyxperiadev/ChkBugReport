package com.sonyericsson.chkbugreport.plugins.logs.kernel;

import java.util.HashMap;
import java.util.HashSet;

public class Packet {

    public boolean ok = false;
    public String prefix;
    public Packet ref;
    public String in;
    public String out;
    public String src;
    public String dst;
    public String proto;
    public int len;
    public long ts;
    public long realTs;
    public KernelLogLine log;

    private HashMap<String, String> mAttrs = new HashMap<String, String>();
    private HashSet<String> mFlags = new HashSet<String>();

    public void put(String key, String value) {
        mAttrs.put(key, value);
        if ("IN".equals(key)) {
            in = value;
        } else if ("OUT".equals(key)) {
            out = value;
        } else if ("SRC".equals(key)) {
            src = value;
        } else if ("DST".equals(key)) {
            dst = value;
        } else if ("PROTO".equals(key)) {
            proto = value;
        } else if ("LEN".equals(key)) {
            len = Integer.parseInt(value);
        }
    }

    public String getAttr(String key) {
        return mAttrs.get(key);
    }

    public void addFlag(String f) {
        mFlags.add(f);
    }

    public boolean hasFlag(String f) {
        return mFlags.contains(f);
    }

    public String getIface() {
        if (in != null && in.length() != 0) {
            return in;
        }
        if (out != null && out.length() != 0) {
            return out;
        }
        return null;
    }

    public boolean isInput() {
        return in != null && in.length() > 0;
    }

    public boolean isOutput() {
        return out != null && out.length() > 0;
    }

    public void check() {
        // TODO: we could add some sanity check here
        ok = true;
    }

}
