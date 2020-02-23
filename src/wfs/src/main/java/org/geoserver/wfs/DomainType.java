/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents a OWS 1.1 DomainType (used for constraints, operation parametes and the like). The
 * current implementation is incomplete, more options are possible but these are the ones currently
 * used in WFS capabilities documents
 */
public class DomainType {
    String name;
    boolean noValues;
    List<String> allowedValues = Collections.emptyList();
    String defaultValue;

    /** Builds a constraint with a name, and a default value */
    public DomainType(String name, String defaultValue) {
        this.name = name;
        this.noValues = true;
        this.defaultValue = defaultValue;
    }

    /** Builds a constraint with a name and a non null list of allowed values (eventually empty) */
    public DomainType(String name, List<String> allowedValues) {
        this.name = name;
        this.allowedValues = allowedValues;
    }

    public DomainType(String name, String[] allowedValues) {
        this.name = name;
        this.allowedValues = new ArrayList<>(Arrays.asList(allowedValues));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isNoValues() {
        return noValues;
    }

    public void setNoValues(boolean noValues) {
        this.noValues = noValues;
    }

    public List<String> getAllowedValues() {
        return allowedValues;
    }

    public void setAllowedValues(List<String> allowedValues) {
        this.allowedValues = allowedValues;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
}
