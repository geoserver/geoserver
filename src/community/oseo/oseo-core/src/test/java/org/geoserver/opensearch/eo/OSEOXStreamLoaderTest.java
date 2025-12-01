/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.opensearch.eo.security.EOCollectionAccessLimitInfoImpl;
import org.geoserver.opensearch.eo.security.EOProductAccessLimitInfoImpl;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

public class OSEOXStreamLoaderTest extends GeoServerSystemTestSupport {

    public static final String ROLE1 = "R1";
    public static final String ROLE2 = "R2";
    public static final String LANDSAT8 = "landast8";
    public static final String PLATFORM_IS_S2 = "\"eo:platform\" = 's2a'";

    @Test
    public void testInit() throws Exception {
        OSEOXStreamLoader loader = GeoServerExtensions.bean(OSEOXStreamLoader.class);
        OSEOInfo oseo = new OSEOInfoImpl();
        loader.initializeService(oseo);
        assertNotNull(oseo.getGlobalQueryables());
    }

    @Test
    public void testSerialize() throws Exception {
        OSEOXStreamLoader loader = GeoServerExtensions.bean(OSEOXStreamLoader.class);
        OSEOInfo oseo = new OSEOInfoImpl();
        loader.initializeService(oseo);
        oseo.getCollectionLimits().add(new EOCollectionAccessLimitInfoImpl(PLATFORM_IS_S2, List.of(ROLE1)));
        oseo.getProductLimits().add(new EOProductAccessLimitInfoImpl(LANDSAT8, PLATFORM_IS_S2, List.of(ROLE1, ROLE2)));
        XStreamPersister persister = new XStreamPersisterFactory().createXMLPersister();
        loader.initXStreamPersister(persister, getGeoServer());
        String xml = persister.getXStream().toXML(oseo);

        Document document = XMLUnit.buildTestDocument(xml);
        assertXpathEvaluatesTo("true", "oseo/enabled", document);
        assertXpathEvaluatesTo("100", "oseo/maximumRecords", document);
        assertXpathEvaluatesTo("10", "oseo/recordsPerPage", document);
        assertXpathEvaluatesTo("1", "oseo/aggregatesCacheTTL", document);
        assertXpathEvaluatesTo("HOURS", "oseo/aggregatesCacheTTLUnit", document);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/eop/2.1",
                "oseo/productClasses/productClass[name='eop_generic']/namespace",
                document);
        assertXpathEvaluatesTo("eop", "oseo/productClasses/productClass[name='eop_generic']/prefix", document);
        assertXpathEvaluatesTo(PLATFORM_IS_S2, "oseo/collectionLimits/eoCollectionLimit/cqlFilter", document);
        assertXpathEvaluatesTo(ROLE1, "oseo/collectionLimits/eoCollectionLimit/roles/string", document);
        assertXpathEvaluatesTo(PLATFORM_IS_S2, "oseo/productLimits/eoProductLimit/cqlFilter", document);
        assertXpathEvaluatesTo(ROLE1, "oseo/productLimits/eoProductLimit/roles/string[1]", document);
        assertXpathEvaluatesTo(ROLE2, "oseo/productLimits/eoProductLimit/roles/string[2]", document);
    }
}
