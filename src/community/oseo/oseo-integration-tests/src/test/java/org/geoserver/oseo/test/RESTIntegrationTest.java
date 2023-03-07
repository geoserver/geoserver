/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.oseo.test;

import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import org.geoserver.ogcapi.v1.stac.STACQueryablesBuilder;
import org.geoserver.opensearch.rest.OSEORestTestSupport;
import org.junit.Test;

/** Checks integration between STAC and the OpenSearch REST management API */
public class RESTIntegrationTest extends OSEORestTestSupport {

    @Test
    public void testEmptyCollectionQueriables() throws Exception {
        // the sample feature code used fail here, the collection is empty, no products yet
        createTest123Collection();

        DocumentContext json = getAsJSONPath("ogc/stac/v1/collections/TEST123/queryables", 200);
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/v1/collections/TEST123/queryables",
                json.read("$.$id"));

        // only has the basic built-ins
        assertEquals(
                STACQueryablesBuilder.GEOMETRY_SCHEMA_REF, json.read("properties.geometry.$ref"));
        assertEquals(
                STACQueryablesBuilder.DATETIME_SCHEMA_REF, json.read("properties.datetime.$ref"));
        assertEquals(
                STACQueryablesBuilder.COLLECTION_SCHEMA_REF,
                json.read("properties.collection.$ref"));
        assertEquals(STACQueryablesBuilder.ID_SCHEMA_REF, json.read("properties.id.$ref"));
    }
}
