/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.models.media.Schema;

/*
 * A Queryables document is a schema, with a couple of additional properties
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Queryables extends Schema<Object> {

    public static final String REL = "http://www.opengis.net/def/rel/ogc/1.0/queryables";

    private final String schema = "https://json-schema.org/draft/2019-09/schema";

    private String id;

    private String collectionId;

    public Queryables(String id) {
        this.id = id;
    }

    @JsonProperty("$schema")
    public String getSchema() {
        return schema;
    }

    @JsonProperty("$id")
    public String getId() {
        return id;
    }

    @JsonIgnore
    public String getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(String collectionId) {
        this.collectionId = collectionId;
    }
}
