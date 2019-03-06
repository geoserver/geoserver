/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.generatedgeometries.core.longitudelatitude;

import static java.util.Collections.emptyMap;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;
import static org.geoserver.generatedgeometries.core.longitudelatitude.LongLatTestData.LONG_LAT_LAYER;
import static org.geoserver.generatedgeometries.core.longitudelatitude.LongLatTestData.LONG_LAT_NO_GEOM_ON_THE_FLY_LAYER;
import static org.geoserver.generatedgeometries.core.longitudelatitude.LongLatTestData.LONG_LAT_NO_GEOM_ON_THE_FLY_QNAME;
import static org.geoserver.generatedgeometries.core.longitudelatitude.LongLatTestData.LONG_LAT_QNAME;
import static org.geoserver.generatedgeometries.core.longitudelatitude.LongLatTestData.enableGeometryGenerationStrategy;
import static org.geoserver.generatedgeometries.core.longitudelatitude.LongLatTestData.filenameOf;
import static org.geoserver.generatedgeometries.core.longitudelatitude.LongLatTestData.setupXMLNamespaces;
import static org.geoserver.generatedgeometries.core.longitudelatitude.LongLatTestData.wfsUrl;

import java.io.IOException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class LongLatWFSTest extends GeoServerSystemTestSupport {

    @Before
    public void before() {
        setupXMLNamespaces();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        setupBasicLayer(testData);
        setupComplexLayer(testData);
    }

    private void setupBasicLayer(SystemTestData testData) throws IOException {
        testData.addVectorLayer(
                LONG_LAT_NO_GEOM_ON_THE_FLY_QNAME,
                emptyMap(),
                filenameOf(LONG_LAT_NO_GEOM_ON_THE_FLY_LAYER),
                getClass(),
                getCatalog());
    }

    private void setupComplexLayer(SystemTestData testData) throws IOException {
        Catalog catalog = getCatalog();
        testData.addVectorLayer(
                LONG_LAT_QNAME, emptyMap(), filenameOf(LONG_LAT_LAYER), getClass(), catalog);
        FeatureTypeInfo featureTypeInfo = getFeatureTypeInfo(LONG_LAT_QNAME);
        enableGeometryGenerationStrategy(catalog, featureTypeInfo);
    }

    /**
     *
     *
     * <pre>{@code
     *   <?xml version="1.0" encoding="UTF-8"?>
     * <wfs:FeatureCollection xmlns:wfs="http://www.opengis.net/wfs" xmlns="http://www.opengis.net/wfs"
     * 	xmlns:gml="http://www.opengis.net/gml" xmlns:gs="http://geoserver.org"
     * 	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://geoserver.org http://localhost:8080/geoserver/wfs?service=WFS&amp;version=1.0.0&amp;request=DescribeFeatureType&amp;typeName=gs%3ALongLatBasicLayer http://www.opengis.net/wfs http://localhost:8080/geoserver/schemas/wfs/1.0.0/WFS-basic.xsd">
     *   <gml:boundedBy>
     *     <gml:Box srsName="http://www.opengis.net/gml/srs/epsg.xml#4326">
     *       <gml:coordinates cs="," decimal="." ts=" " xmlns:gml="http://www.opengis.net/gml">-1,-1 0,0</gml:coordinates>
     *     </gml:Box>
     *   </gml:boundedBy>
     *   <gml:featureMember>
     *     <gs:LongLatBasicLayer fid="feature.0">
     *       <gs:id>0</gs:id>
     *       <gs:lon>0.0</gs:lon>
     *       <gs:lat>0.0</gs:lat>
     *       <gs:data>data</gs:data>
     *     </gs:LongLatBasicLayer>
     *   </gml:featureMember>
     * </wfs:FeatureCollection>
     * }</pre>
     *
     * @throws Exception
     */
    @Test
    public void testThatWFSReturnsFeaturesWithoutGeometryOnTheFly() throws Exception {
        // when
        Document dom = getAsDOM(wfsUrl(LONG_LAT_NO_GEOM_ON_THE_FLY_LAYER), 200);

        // then
        assertXpathExists("//wfs:FeatureCollection", dom);
        assertXpathEvaluatesTo(
                "feature.0",
                "//wfs:FeatureCollection/gml:featureMember/gs:LongLatBasicLayer/@fid",
                dom);
        assertXpathNotExists(
                "//wfs:FeatureCollection/gml:featureMember/gs:LongLatLayer/gs:geom/gml:Point/gml:coordinates",
                dom);
    }

    @Test
    public void testThatWFSReturnsFeaturesWithGeometryOnTheFly() throws Exception {
        // when
        Document dom = getAsDOM(wfsUrl(LONG_LAT_LAYER), 200);
        print(dom);

        // then
        assertXpathExists("//wfs:FeatureCollection", dom);
        assertXpathEvaluatesTo(
                "feature.0", "//wfs:FeatureCollection/gml:featureMember/gs:LongLatLayer/@fid", dom);
        assertXpathEvaluatesTo(
                "0,0",
                "//wfs:FeatureCollection/gml:featureMember/gs:LongLatLayer/gs:geom/gml:Point/gml:coordinates",
                dom);
        assertXpathEvaluatesTo(
                "0", "//wfs:FeatureCollection/gml:featureMember/gs:LongLatLayer/gs:id", dom);
    }
}
