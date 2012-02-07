package com.sonyericsson.chkbugreport.settings;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

public class Settings implements Iterable<Setting> {

    private static final String PROPERTIES_FILE_NAME = ".chkbugreport";

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
            props.store(new FileWriter(homeDir + "/" + PROPERTIES_FILE_NAME), null);
        } catch (IOException e) {
            // Just ignore any error heres
        }
    }

    @Override
    public Iterator<Setting> iterator() {
        return mSettings.iterator();
    }

}
