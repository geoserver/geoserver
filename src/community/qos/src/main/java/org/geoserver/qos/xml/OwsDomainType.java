/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import java.io.Serializable;
import java.util.List;

/** @author Fernando Miño, Geosolutions */
public class OwsDomainType implements Serializable {
    private static final long serialVersionUID = 1L;

    // ows:Value list
    private List<String> value;
    // ows:Metadata [0..*]
    private List<OwsMetadata> metadata;
    // ows:AllowedValues
    private OwsAllowedValues allowedValues;

    public OwsDomainType() {}

    public List<String> getValue() {
        return value;
    }

    public void setValue(List<String> value) {
        this.value = value;
    }

    public List<OwsMetadata> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<OwsMetadata> metadata) {
        this.metadata = metadata;
    }

    public OwsAllowedValues getAllowedValues() {
        return allowedValues;
    }

    public void setAllowedValues(OwsAllowedValues allowedValues) {
        this.allowedValues = allowedValues;
    }
}
