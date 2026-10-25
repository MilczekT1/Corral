package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

/** MUST IGNORE as a class: bound only through {@link CsvConfiguration#exportSettings()}. */
public class CsvSettings extends ExportSettings {
    private char delimiter = ',';
}
