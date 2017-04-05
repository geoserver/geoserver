package org.geoserver.rest.catalog;

import java.util.List;

/**
 * Helper class that can be used to serialize a list of string to XML or JSON.
 * The alias will be used as the tag name in XML.
 */
public class StringsList {

    private final List<String> values;
    private final String alias;

    public StringsList(List<String> values, String alias) {
        this.values = values;
        this.alias = alias;
    }

    public List<String> getValues() {
        return values;
    }

    public String getAlias() {
        return alias;
    }
}
