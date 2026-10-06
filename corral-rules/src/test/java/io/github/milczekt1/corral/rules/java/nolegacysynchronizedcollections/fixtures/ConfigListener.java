package io.github.milczekt1.corral.rules.java.nolegacysynchronizedcollections.fixtures;

import java.util.Dictionary;

/** MUST IGNORE: {@code Dictionary}, as OSGi's {@code ManagedService.updated} takes it, is not matched. */
public class ConfigListener {

    private Object port;

    public void updated(Dictionary<String, ?> properties) {
        port = properties.get("port");
    }

    public Object port() {
        return port;
    }
}
