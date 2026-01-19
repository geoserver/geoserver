/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.util.AllowListEntityResolver;
import org.geoserver.util.EntityResolverProvider;
import org.geoserver.wfs.kvp.Filter_1_1_0_KvpParser;
import org.geotools.api.filter.Id;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.spatial.Intersects;
import org.junit.After;
import org.junit.Test;

public class ExternalEntitiesTest extends WFSTestSupport {

    private static final String FILTER =
            """
            <Filter xmlns="http://www.opengis.net/ogc">
              <FeatureId fid="states.1"/>
            </Filter>""";

    private static final String FILTER_OGC_NAMESPACE = "<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\" "
            + "xmlns:gml=\"http://www.opengis.net/gml\">"
            + "<ogc:Intersects><ogc:PropertyName>the_geom</ogc:PropertyName>"
            + "<gml:Polygon><gml:exterior><gml:LinearRing>"
            + "<gml:posList>-112 46 -109 46 -109 47 -112 47 -112 46</gml:posList>"
            + "</gml:LinearRing></gml:exterior></gml:Polygon></ogc:Intersects></ogc:Filter>";

    private static final String FILTER_OGC_SCHEMA_LOCATION =
            """
            <Filter xmlns="http://www.opengis.net/ogc"
                  xsi:schemaLocation="http://www.opengis.net/ogc http://schemas.opengis.net/filter/1.1.0/filter.xsd">
              <FeatureId fid="states.1"/>
            </Filter>""";

    private static final String FILTER_RESTRICTED_SCHEMA_SCHEMA_LOCATION =
            """
            <Filter xmlns="http://invalid/schema"
                  xsi:schemaLocation="http://invalid/schema http://schemas.opengis.net/filter/1.1.0/filter.xsd">
              <FeatureId fid="states.1"/>
            </Filter>""";

    private static final String FILTER_RESTRICTED_NAMESPACE =
            """
            <Filter xmlns="http://invalid/schema"
                  xsi:schemaLocation="http://invalid/schema http://schemas.opengis.net/filter/1.1.0/filter.xsd">
              <FeatureId fid="states.1"/>
            </Filter>""";

    @After
    public void clearEntityResolutionUnrestrictedProperty() {
        System.clearProperty(EntityResolverProvider.ENTITY_RESOLUTION_UNRESTRICTED);
    }

    @Test
    public void testAllowListFilter() throws Exception {
        try {
            EntityResolverProvider.setEntityResolver(
                    new AllowListEntityResolver(getGeoServer(), "http://localhost:8080/"));

            Filter_1_1_0_KvpParser kvpParser = new Filter_1_1_0_KvpParser(getGeoServer());

            List filters = (List) kvpParser.parse(FILTER);
            assertEquals("parsed id filter", 1, filters.size());
            Id id = (Id) filters.get(0);
            assertTrue("parsed id filter", id.getIDs().contains("states.1"));

            filters = (List) kvpParser.parse(FILTER_OGC_NAMESPACE);
            assertEquals("parsed intsersect filter", 1, filters.size());
            Intersects intersect = (Intersects) filters.get(0);
            assertEquals(
                    "parsed intsersect filter",
                    "the_geom",
                    ((PropertyName) intersect.getExpression1()).getPropertyName());

            filters = (List) kvpParser.parse(FILTER_OGC_SCHEMA_LOCATION);
            assertEquals("parsed ogc filter", 1, filters.size());
            id = (Id) filters.get(0);
            assertTrue("parsed ogc filter", id.getIDs().contains("states.1"));

            filters = (List) kvpParser.parse(FILTER_RESTRICTED_SCHEMA_SCHEMA_LOCATION);
            assertEquals("parsed restricted filter", 1, filters.size());
            id = (Id) filters.get(0);
            assertTrue("parsed restricted filter", id.getIDs().contains("states.1"));

            filters = (List) kvpParser.parse(FILTER_RESTRICTED_NAMESPACE);
            assertEquals("parsed restricted namespace filter", 1, filters.size());
            id = (Id) filters.get(0);
            assertTrue("parsed restricted namespace filter", id.getIDs().contains("states.1"));

            final String LOCALHOST =
                    "<dmt xmlns=\"http://a.b/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://a.b/ http://localhost:8080/dmt.xsd\">dmt</dmt>";
            filters = (List) kvpParser.parse(LOCALHOST);
            assertTrue(filters.isEmpty());

            EntityResolverProvider.setEntityResolver(new AllowListEntityResolver(getGeoServer()));
            filters = (List) kvpParser.parse(LOCALHOST);
            assertTrue(filters.isEmpty());
        } finally {
            EntityResolverProvider.setEntityResolver(GeoServerSystemTestSupport.RESOLVE_DISABLED_PROVIDER_DEVMODE);
        }
    }
}
