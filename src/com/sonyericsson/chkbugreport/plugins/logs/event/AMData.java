package com.sonyericsson.chkbugreport.plugins.logs.event;

/**
 * A data representing an AM (ActivityManager) event
 */
public class AMData {

    public static final int BORN = 1;
    public static final int DIE = 2;
    public static final int ON_CREATE = 3;
    public static final int ON_DESTROY = 4;
    public static final int ON_PAUSE = 5;
    public static final int ON_RESTART = 6;
    public static final int ON_RESUME = 7;
    public static final int SCHEDULE_SERVICE_RESTART = 8;

    public static final int PROC = 0;
    public static final int SERVICE = 1;
    public static final int ACTIVITY = 2;

    private int mType;
    private int mAction;
    private int mPid;
    private String mComponent;
    private long mTS;

    public AMData(int type, int action, int pid, String component, long ts) {
        mType = type;
        mAction = action;
        mPid = pid;
        mComponent = component;
        mTS = ts;
    }

    public int getType() {
        return mType;
    }

    public int getPid() {
        return mPid;
    }

    public int getAction() {
        return mAction;
    }

    public String getComponent() {
        return mComponent;
    }

    public long getTS() {
        return mTS;
    }

    public void setPid(int pid) {
        mPid = pid;
    }

    public void setComponent(String component) {
        mComponent = component;
    }

}
