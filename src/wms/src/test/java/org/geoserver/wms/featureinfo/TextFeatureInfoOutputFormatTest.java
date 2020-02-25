/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.WfsFactory;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.util.factory.Hints;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TextFeatureInfoOutputFormatTest extends WMSTestSupport {

    public static TimeZone defaultTimeZone;

    private TextFeatureInfoOutputFormat outputFormat;

    private FeatureCollectionType fcType;

    Map<String, Object> parameters;

    GetFeatureInfoRequest getFeatureInfoRequest;

    @BeforeClass
    public static void beforeClass() {
        System.setProperty("org.geotools.localDateTimeHandling", "true");
        System.getProperties().remove("org.geotools.dateTimeFormatHandling");
        Hints.scanSystemProperties();
        defaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("GMT-05:00"));
    }

    @AfterClass
    public static void afterClass() {
        System.getProperties().remove("org.geotools.dateTimeFormatHandling");
        Hints.scanSystemProperties();
        TimeZone.setDefault(defaultTimeZone);
    }

    @Before
    public void setUp() throws URISyntaxException, IOException {
        outputFormat = new TextFeatureInfoOutputFormat(getWMS());

        Request request = new Request();
        parameters = new HashMap<String, Object>();
        parameters.put("LAYER", "testLayer");
        Map<String, String> env = new HashMap<String, String>();
        env.put("TEST1", "VALUE1");
        env.put("TEST2", "VALUE2");
        parameters.put("ENV", env);
        request.setKvp(parameters);

        Dispatcher.REQUEST.set(request);

        final FeatureTypeInfo featureType = getFeatureTypeInfo(MockData.NULLS);

        fcType = WfsFactory.eINSTANCE.createFeatureCollectionType();
        fcType.getFeature().add(featureType.getFeatureSource(null, null).getFeatures());

        // fake layer list
        List<MapLayerInfo> queryLayers = new ArrayList<MapLayerInfo>();
        LayerInfo layerInfo = new LayerInfoImpl();
        layerInfo.setType(PublishedType.VECTOR);
        ResourceInfo resourceInfo = new FeatureTypeInfoImpl(null);
        NamespaceInfo nameSpace = new NamespaceInfoImpl();
        nameSpace.setPrefix("topp");
        nameSpace.setURI("http://www.topp.org");
        resourceInfo.setNamespace(nameSpace);
        layerInfo.setResource(resourceInfo);
        MapLayerInfo mapLayerInfo = new MapLayerInfo(layerInfo);
        queryLayers.add(mapLayerInfo);
        getFeatureInfoRequest = new GetFeatureInfoRequest();
        getFeatureInfoRequest.setQueryLayers(queryLayers);
    }

    /** Test null geometry is correctly handled (GEOS-6829). */
    @Test
    public void testNullGeometry() throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        outputFormat.write(fcType, getFeatureInfoRequest, outStream);
        String result = new String(outStream.toByteArray());

        assertFalse(result.contains("java.lang.NullPointerException"));
        assertTrue(result.contains("pointProperty = null"));
    }

    @Test
    public void testDateTimeFormattingEnabled() throws Exception {
        TimeZone defaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("GMT-05:00"));
        try {
            System.setProperty("org.geotools.dateTimeFormatHandling", "true");
            Hints.scanSystemProperties();
            final FeatureTypeInfo featureType = getFeatureTypeInfo(MockData.PRIMITIVEGEOFEATURE);
            fcType = WfsFactory.eINSTANCE.createFeatureCollectionType();
            fcType.getFeature().add(featureType.getFeatureSource(null, null).getFeatures());

            getFeatureInfoRequest.setFeatureCount(10);
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            outputFormat.write(fcType, getFeatureInfoRequest, outStream);
            String result = new String(outStream.toByteArray());
            assertTrue(result.contains("dateTimeProperty = 2006-06-26T19:00:00-05:00"));

        } finally {
            getFeatureInfoRequest.setFeatureCount(1);
            TimeZone.setDefault(defaultTimeZone);
        }
    }

    @Test
    public void testDateTimeFormattingDisabled() throws Exception {
        System.setProperty("org.geotools.dateTimeFormatHandling", "false");
        Hints.scanSystemProperties();
        try {
            final FeatureTypeInfo featureType = getFeatureTypeInfo(MockData.PRIMITIVEGEOFEATURE);
            fcType = WfsFactory.eINSTANCE.createFeatureCollectionType();
            fcType.getFeature().add(featureType.getFeatureSource(null, null).getFeatures());

            getFeatureInfoRequest.setFeatureCount(10);
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            outputFormat.write(fcType, getFeatureInfoRequest, outStream);
            String result = new String(outStream.toByteArray());
            assertTrue(result.contains("dateTimeProperty = 2006-06-26 19:00:00.0"));
        } finally {
            getFeatureInfoRequest.setFeatureCount(1);
            System.getProperties().remove("org.geotools.dateTimeFormatHandling");
            Hints.scanSystemProperties();
        }
    }
}
