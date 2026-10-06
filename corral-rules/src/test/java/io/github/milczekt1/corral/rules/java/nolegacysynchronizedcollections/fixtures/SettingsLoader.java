package io.github.milczekt1.corral.rules.java.nolegacysynchronizedcollections.fixtures;

import java.util.Properties;

/** MUST IGNORE: {@code Properties} extends {@code Hashtable} but is a distinct class. */
public class SettingsLoader {

    public String load(Properties settings) {
        settings.put("mode", "strict");
        return settings.getProperty("mode");
    }
}
