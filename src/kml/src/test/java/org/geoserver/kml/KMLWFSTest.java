/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.WfsFactory;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSTestSupport;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class KMLWFSTest extends WFSTestSupport {

    @Override
    protected void setUpNamespaces(Map<String, String> namespaces) {
        namespaces.put("kml", "http://www.opengis.net/kml/2.2");
    }

    @Test
    public void testGetCapabilities() throws Exception {
        Document doc = getAsDOM("wfs?request=GetCapabilities&version=1.1.0");
        // print(doc);

        // check the new output format is part of the caps document
        XMLAssert.assertXpathEvaluatesTo(
                "1",
                "count(//ows:Operation[@name='GetFeature']/"
                        + "ows:Parameter[@name='outputFormat']/ows:Value[text() = '"
                        + KMLMapOutputFormat.MIME_TYPE
                        + "'])",
                doc);
    }

    @Test
    public void testGetFeature() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wfs?service=WFS&version=1.1.0&request=GetFeature&typeName="
                                + getLayerId(MockData.AGGREGATEGEOFEATURE)
                                + "&outputFormat="
                                + KMLMapOutputFormat.MIME_TYPE.replace("+", "%2B"));
        assertEquals(200, response.getStatus());
        assertEquals(
                "inline; filename=" + MockData.AGGREGATEGEOFEATURE.getLocalPart() + ".kml",
                response.getHeader("Content-Disposition"));
        Document doc = dom(new ByteArrayInputStream(response.getContentAsString().getBytes()));
        checkAggregateGeoFeatureKmlContents(doc);
    }

    @Test
    public void testGetFeatureKMLAlias() throws Exception {
        Document doc =
                getAsDOM(
                        "wfs?service=WFS&version=1.1.0&request=GetFeature&typeName="
                                + getLayerId(MockData.AGGREGATEGEOFEATURE)
                                + "&outputFormat=KML");
        checkAggregateGeoFeatureKmlContents(doc);
    }

    private void checkAggregateGeoFeatureKmlContents(Document doc) throws Exception {
        // print(doc);

        // there is one schema
        XMLAssert.assertXpathEvaluatesTo("1", "count(//kml:Document/kml:Schema)", doc);
        // check we only have the non geom properties
        XMLAssert.assertXpathEvaluatesTo(
                "6", "count(//kml:Document/kml:Schema/kml:SimpleField)", doc);
        XMLAssert.assertXpathEvaluatesTo(
                "0",
                "count(//kml:Document/kml:Schema/kml:SimpleField[@name='multiPointProperty'])",
                doc);
        XMLAssert.assertXpathEvaluatesTo(
                "0",
                "count(//kml:Document/kml:Schema/kml:SimpleField[@name='multiCurveProperty'])",
                doc);
        XMLAssert.assertXpathEvaluatesTo(
                "0",
                "count(//kml:Document/kml:Schema/kml:SimpleField[@name='multiSurfaceProperty'])",
                doc);
        // check the type mapping
        XMLAssert.assertXpathEvaluatesTo(
                "string",
                "//kml:Document/kml:Schema/kml:SimpleField[@name='description']/@type",
                doc);
        XMLAssert.assertXpathEvaluatesTo(
                "double",
                "//kml:Document/kml:Schema/kml:SimpleField[@name='doubleProperty']/@type",
                doc);
        XMLAssert.assertXpathEvaluatesTo(
                "int",
                "//kml:Document/kml:Schema/kml:SimpleField[@name='intRangeProperty']/@type",
                doc);
        XMLAssert.assertXpathEvaluatesTo(
                "string",
                "//kml:Document/kml:Schema/kml:SimpleField[@name='strProperty']/@type",
                doc);
        XMLAssert.assertXpathEvaluatesTo(
                "string",
                "//kml:Document/kml:Schema/kml:SimpleField[@name='featureCode']/@type",
                doc);

        // check the extended data of one feature
        String sd =
                "//kml:Placemark[@id='AggregateGeoFeature.f005']/kml:ExtendedData/kml:SchemaData/kml:SimpleData";
        XMLAssert.assertXpathEvaluatesTo("description-f005", sd + "[@name='description']", doc);
        XMLAssert.assertXpathEvaluatesTo("name-f005", sd + "[@name='name']", doc);
        XMLAssert.assertXpathEvaluatesTo("2012.78", sd + "[@name='doubleProperty']", doc);
        XMLAssert.assertXpathEvaluatesTo(
                "Ma quande lingues coalesce, li grammatica del resultant "
                        + "lingue es plu simplic e regulari quam ti del coalescent lingues. Li nov lingua "
                        + "franca va esser plu simplic e regulari quam li existent Europan lingues.",
                sd + "[@name='strProperty']",
                doc);
        XMLAssert.assertXpathEvaluatesTo("BK030", sd + "[@name='featureCode']", doc);
    }

    @Test
    public void testForceWGS84() throws Exception {
        Document doc =
                getAsDOM(
                        "wfs?service=WFS&version=1.1.0&request=GetFeature&typeName="
                                + getLayerId(MockData.MPOINTS)
                                + "&outputFormat=KML");

        // print(doc);

        XMLAssert.assertXpathEvaluatesTo("1", "count(//kml:Folder)", doc);
        KMLTest.assertPointCoordinate(
                doc,
                "//kml:Placemark/kml:MultiGeometry/kml:Point[1]/kml:coordinates",
                -92.99707024070754,
                4.523788746085423);
        KMLTest.assertPointCoordinate(
                doc,
                "//kml:Placemark/kml:MultiGeometry/kml:Point[2]/kml:coordinates",
                -92.99661950641159,
                4.524241081543828);
    }

    @Test
    public void testCloseIterators() throws ServiceException, IOException {
        // build a wfs response with an iterator that will mark if close has been called, or not
        FeatureTypeInfo fti = getCatalog().getFeatureTypeByName(getLayerId(MockData.POLYGONS));
        FeatureSource fs = fti.getFeatureSource(null, null);
        SimpleFeatureCollection fc = (SimpleFeatureCollection) fs.getFeatures();
        final AtomicInteger openIterators = new AtomicInteger(0);
        FeatureCollection decorated =
                new org.geotools.feature.collection.DecoratingSimpleFeatureCollection(fc) {
                    @Override
                    public SimpleFeatureIterator features() {
                        openIterators.incrementAndGet();
                        final SimpleFeature f = DataUtilities.first(delegate);
                        return new SimpleFeatureIterator() {

                            @Override
                            public SimpleFeature next() throws NoSuchElementException {
                                return f;
                            }

                            @Override
                            public boolean hasNext() {
                                return true;
                            }

                            @Override
                            public void close() {
                                openIterators.decrementAndGet();
                            }
                        };
                    }
                };
        FeatureCollectionType response = WfsFactory.eINSTANCE.createFeatureCollectionType();
        response.getFeature().add(decorated);
        FeatureCollectionResponse fcResponse = FeatureCollectionResponse.adapt(response);

        WFSKMLOutputFormat outputFormat = GeoServerExtensions.bean(WFSKMLOutputFormat.class);
        FilterOutputStream fos =
                new FilterOutputStream(new ByteArrayOutputStream()) {

                    int count = 0;

                    @Override
                    public void write(byte[] b) throws IOException {
                        write(b, 0, b.length);
                    }

                    @Override
                    public void write(byte[] b, int off, int len) throws IOException {
                        for (int i = off; i < len; i++) {
                            write(b[i]);
                        }
                    }

                    @Override
                    public void write(int b) throws IOException {
                        count++;
                        if (count > 100) {
                            throw new IOException("Simularing client shutting down connection");
                        }
                        super.write(b);
                    }
                };
        try {
            outputFormat.write(fcResponse, fos, null);
        } catch (Exception e) {
            // fine, it's expected
        }

        assertEquals(0, openIterators.get());
    }
}
