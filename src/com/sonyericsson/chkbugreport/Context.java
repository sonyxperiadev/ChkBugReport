package com.sonyericsson.chkbugreport;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Context {

    // Time window markers
    private TimeWindowMarker mTimeWindowStart = new TimeWindowMarker();
    private TimeWindowMarker mTimeWindowEnd = new TimeWindowMarker();

    public void parseTimeWindow(String timeWindow) {
        try {
            Matcher m = Pattern.compile("(.*)\\.\\.(.*)").matcher(timeWindow);
            if (!m.matches()) {
                throw new IllegalArgumentException("Incorrect time window range");
            }
            mTimeWindowStart = new TimeWindowMarker(m.group(1));
            mTimeWindowEnd = new TimeWindowMarker(m.group(2));
        } catch (Exception e) {
            System.err.println("Error parsing timewindow: `" + timeWindow + "«: " + e);
            System.exit(1);
        }
    }

    public TimeWindowMarker getTimeWindowStart() {
        return mTimeWindowStart;
    }

    public TimeWindowMarker getTimeWindowEnd() {
        return mTimeWindowEnd;
    }

}
