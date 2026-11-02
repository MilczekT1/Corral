package io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures.production;

/** MUST IGNORE: nothing here is bound; its override in {@link ClientConfiguration} gets a bridge method. */
public abstract class ConfigurationTemplate {
    public SettingsTemplate clientSettings() {
        return new SettingsTemplate();
    }
}
