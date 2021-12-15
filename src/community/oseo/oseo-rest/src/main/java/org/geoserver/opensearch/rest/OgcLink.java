/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.HashMap;
import java.util.Map;

/**
 * Bean representation of a OWS context OGC Link
 *
 * @author Andrea Aime - GeoSolutions
 */
class OgcLink {
    String offering;

    String method;

    String code;

    String type;

    String href;

    Map<String, Object> unknownFields = new HashMap<>();

    public OgcLink() {
        // default constructor
    }

    public OgcLink(
            String offering,
            String method,
            String code,
            String type,
            String href,
            Map<String, Object> unknownFields) {
        this.offering = offering;
        this.method = method;
        this.code = code;
        this.type = type;
        this.href = href;
        this.unknownFields = unknownFields;
    }

    public String getOffering() {
        return offering;
    }

    public void setOffering(String offering) {
        this.offering = offering;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    // Capture all other fields that Jackson do not match other members
    @JsonAnyGetter
    @JsonSerialize(using = UnknownFieldsSerializer.class)
    public Map<String, Object> otherFields() {
        return unknownFields;
    }

    @JsonAnySetter
    public void setOtherField(String name, Object value) {
        unknownFields.put(name, value);
    }
}
