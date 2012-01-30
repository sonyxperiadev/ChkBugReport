package com.sonyericsson.chkbugreport;

import java.io.IOException;

public class Extension {

    public static final int RET_NOP     = -1;
    public static final int RET_FALSE   =  0;
    public static final int RET_TRUE    = +1;

    public int loadReportFrom(Report report, String fileName, int mode) throws IOException {
        return RET_NOP;
    }

}
