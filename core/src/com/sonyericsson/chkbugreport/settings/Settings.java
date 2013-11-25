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
package com.sonyericsson.chkbugreport.settings;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import com.sonyericsson.chkbugreport.util.Util;

public class Settings implements Iterable<Setting> {

    private static final String PROPERTIES_FILE_NAME = Util.PRIVATE_DIR_NAME + "/config";

    private Vector<Setting> mSettings = new Vector<Setting>();

    public void add(Setting setting) {
        mSettings.add(setting);
    }

    public void load() {
        // Load some system properties
        try {
            String homeDir = System.getProperty("user.home");
            Properties props = new Properties();
            props.load(new FileReader(homeDir + "/" + PROPERTIES_FILE_NAME));
            for (Setting setting : mSettings) {
                setting.load(props);
            }
        } catch (IOException e) {
            // Just ignore any error heres
        }
    }

    public void save() {
        // Load some system properties
        try {
            String homeDir = System.getProperty("user.home");
            Properties props = new Properties();
            for (Setting setting : mSettings) {
                setting.store(props);
            }
            // Also make sure the directory is ready
            File f = new File(homeDir + "/" + PROPERTIES_FILE_NAME);
            f.getParentFile().mkdirs();
            props.store(new FileWriter(f), null);
        } catch (IOException e) {
            // Just ignore any error heres
        }
    }

    @Override
    public Iterator<Setting> iterator() {
        return mSettings.iterator();
    }

}
