/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wms.featureinfo;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.WfsFactory;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Keyword;
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
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.template.GeoServerMemberAccessPolicy;
import org.geoserver.template.GeoServerTemplateLoader;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.DataUtilities;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

public class HTMLFeatureInfoOutputFormatTest extends WMSTestSupport {
    private HTMLFeatureInfoOutputFormat outputFormat;

    private FeatureCollectionType fcType;

    Map<String, Object> parameters;

    GetFeatureInfoRequest getFeatureInfoRequest;

    static final String templateFolder = "/org/geoserver/wms/featureinfo/";

    private String currentTemplate;

    @Before
    public void setUp() throws URISyntaxException, IOException {
        outputFormat =
                new HTMLFeatureInfoOutputFormat(getWMS(), GeoServerExtensions.bean(GeoServerResourceLoader.class));

        currentTemplate = "test_content.ftl";
        // configure template loader
        GeoServerTemplateLoader templateLoader = new GeoServerTemplateLoader(this.getClass(), getDataDirectory()) {

            @Override
            public Object findTemplateSource(String path) throws IOException {
                String templatePath;
                if (path.toLowerCase().contains("content")) {
                    templatePath = currentTemplate;

                } else {
                    templatePath = "empty.ftl";
                }
                try {
                    return new File(this.getClass()
                            .getResource(templateFolder + templatePath)
                            .toURI());
                } catch (URISyntaxException e) {
                    return null;
                }
            }
        };
        outputFormat.getTemplateManager().setTemplateLoader(templateLoader);

        // test request with some parameters to use in templates
        Request request = new Request();
        parameters = new HashMap<>();
        parameters.put("LAYER", "testLayer");
        parameters.put("NUMBER1", 10);
        parameters.put("NUMBER2", 100);
        Map<String, String> env = new HashMap<>();
        env.put("TEST1", "VALUE1");
        env.put("TEST2", "VALUE2");
        parameters.put("ENV", env);
        request.setKvp(parameters);

        Dispatcher.REQUEST.set(request);

        final FeatureTypeInfo featureType = getFeatureTypeInfo(MockData.PRIMITIVEGEOFEATURE);

        initFeatureType(featureType);

        // fake layer list
        List<MapLayerInfo> queryLayers = new ArrayList<>();
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

    @After
    public void resetSettings() {
        System.clearProperty(FreeMarkerTemplateManager.FORCE_FREEMARKER_ESCAPING);
        System.clearProperty(GeoServerMemberAccessPolicy.FREEMARKER_API_EXPOSED);
        System.clearProperty(GeoServerMemberAccessPolicy.FREEMARKER_BLOCK_LIST);
        FreeMarkerTemplateManager.clearClassIntrospectionCache();
    }

    @SuppressWarnings("unchecked") // EMF model without generics
    private void initFeatureType(FeatureTypeInfo featureType) throws IOException {
        fcType = WfsFactory.eINSTANCE.createFeatureCollectionType();
        fcType.getFeature().add(featureType.getFeatureSource(null, null).getFeatures());
    }

    /** Test request values are inserted in processed template */
    @Test
    public void testRequestParametersAreEvaluatedInTemplate() throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        outputFormat.write(fcType, getFeatureInfoRequest, outStream);
        String result = new String(outStream.toByteArray());

        assertEquals("VALUE1,VALUE2,testLayer", result);
    }

    @Test
    public void testEnvironmentVariablesAreEvaluatedInTemplate() throws IOException {
        currentTemplate = "test_env_content.ftl";
        System.setProperty("TEST_PROPERTY", "MYVALUE");
        MockServletContext servletContext = (MockServletContext) applicationContext.getServletContext();
        servletContext.setInitParameter("TEST_INIT_PARAM", "MYPARAM");
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            outputFormat.write(fcType, getFeatureInfoRequest, outStream);
            String result = new String(outStream.toByteArray());

            assertEquals("MYVALUE,MYPARAM", result);
        } finally {
            System.clearProperty("TEST_PROPERTY");
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCoverageInfoIsEvaluatedInTemplate() throws IOException {
        currentTemplate = "test_resource_content.ftl";
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(toName(MockData.WORLD));
        SimpleFeatureType type = builder.buildFeatureType();
        Double[] values = new Double[0];
        fcType.getFeature().set(0, DataUtilities.collection(SimpleFeatureBuilder.build(type, values, "")));
        ResourceInfo resource = getCatalog().getCoverageByName(toName(MockData.WORLD));
        resource.setTitle("Raster Title");
        resource.setAbstract("Raster Abstract");
        resource.getKeywords().set(0, new Keyword("Raster Keyword"));
        getCatalog().save(resource);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        outputFormat.write(fcType, getFeatureInfoRequest, outStream);
        String result = new String(outStream.toByteArray());

        // Verify that a raster layer's title, abstract, etc. is retrieved properly.
        assertEquals("Raster Title,Raster Abstract,Raster Keyword,EPSG:4326", result);
    }

    @Test
    public void testFeatureTypeInfoIsEvaluatedInTemplate() throws IOException {
        currentTemplate = "test_resource_content.ftl";
        ResourceInfo resource = getFeatureTypeInfo(MockData.PRIMITIVEGEOFEATURE);
        resource.setTitle("Vector Title");
        resource.setAbstract("Vector Abstract");
        resource.getKeywords().set(0, new Keyword("Vector Keyword"));
        getCatalog().save(resource);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        outputFormat.write(fcType, getFeatureInfoRequest, outStream);
        String result = new String(outStream.toByteArray());

        // Verify that a vector layer's title, abstract, etc. is retrieved properly.
        assertEquals("Vector Title,Vector Abstract,Vector Keyword,EPSG:4326", result);
    }

    @Test
    public void testExecuteIsBlocked() throws IOException {
        currentTemplate = "test_execute.ftl";

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        IOException e = Assert.assertThrows(
                IOException.class, () -> outputFormat.write(fcType, getFeatureInfoRequest, outStream));
        assertThat(
                e.getMessage(), CoreMatchers.containsString("Error occurred processing content template content.ftl"));
    }

    /** Test that if template asks a request parameter that is not present in request an exception is thrown. */
    @Test
    public void testErrorWhenRequestParametersAreNotDefined() {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        boolean error = false;

        // remove one parameter required in template
        parameters.remove("LAYER");
        try {
            outputFormat.write(fcType, getFeatureInfoRequest, outStream);
        } catch (IOException e) {
            error = true;
        }
        assertTrue(error);
    }

    @Test
    public void testHTMLGetFeatureInfoCharset() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request = "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg"
                + "&request=GetFeatureInfo&layers="
                + layer
                + "&query_layers="
                + layer
                + "&width=20&height=20&x=10&y=10"
                + "&info_format=text/html";

        MockHttpServletResponse response = getAsServletResponse(request, "");

        // MimeType
        assertEquals("text/html", getBaseMimeType(response.getContentType()));

        // Check if the character encoding is the one expected
        assertEquals(UTF_8.name(), response.getCharacterEncoding());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testConcurrentRequests() throws Exception {
        FeatureTypeInfo featureType1 = getFeatureTypeInfo(MockData.PRIMITIVEGEOFEATURE);
        List<MapLayerInfo> layers1 =
                Collections.singletonList(new MapLayerInfo(getCatalog().getLayerByName(featureType1.prefixedName())));
        FeatureCollectionType type1 = WfsFactory.eINSTANCE.createFeatureCollectionType();
        type1.getFeature().add(featureType1.getFeatureSource(null, null).getFeatures());
        final FeatureTypeInfo featureType2 = getFeatureTypeInfo(MockData.BASIC_POLYGONS);
        List<MapLayerInfo> layers2 =
                Collections.singletonList(new MapLayerInfo(getCatalog().getLayerByName(featureType2.prefixedName())));
        FeatureCollectionType type2 = WfsFactory.eINSTANCE.createFeatureCollectionType();
        type2.getFeature().add(featureType2.getFeatureSource(null, null).getFeatures());
        final HTMLFeatureInfoOutputFormat format =
                new HTMLFeatureInfoOutputFormat(getWMS(), GeoServerExtensions.bean(GeoServerResourceLoader.class));
        format.getTemplateManager().setTemplateLoader(new GeoServerTemplateLoader(getClass(), getDataDirectory()) {
            @Override
            public Object findTemplateSource(String path) throws IOException {
                String templatePath = "empty.ftl";
                if (path.toLowerCase().contains("content")
                        && (this.resource != null)
                        && this.resource.prefixedName().equals(featureType2.prefixedName())) {
                    templatePath = "test_content.ftl";
                }
                try {
                    return new File(this.getClass()
                            .getResource(templateFolder + templatePath)
                            .toURI());
                } catch (URISyntaxException e) {
                    return null;
                }
            }
        });
        int numRequests = 50;
        List<Callable<String>> tasks = new ArrayList<>(numRequests);
        for (int i = 0; i < numRequests; i++) {
            final GetFeatureInfoRequest request = new GetFeatureInfoRequest();
            request.setQueryLayers(((i % 2) == 0) ? layers1 : layers2);
            final FeatureCollectionType type = (((i % 2) == 0) ? type1 : type2);
            tasks.add(() -> {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                format.write(type, request, output);
                return new String(output.toByteArray());
            });
        }
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Future<String>> futures = executor.invokeAll(tasks);
            for (int i = 0; i < numRequests; i++) {
                String info = futures.get(i).get();
                if ((i % 2) == 0) {
                    assertEquals("", info);
                } else {
                    assertNotEquals("", info);
                }
            }
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testStaticMathMethodsAreEvaluatedInTemplate() throws IOException {
        currentTemplate = "test_static_content.ftl";
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        outputFormat.write(fcType, getFeatureInfoRequest, outStream);
        String result = new String(outStream.toByteArray());
        assertEquals(String.valueOf(Math.max(10, 100)), result);
    }

    /** Verifies calls to static methods are possible in unrestricted case. */
    @Test
    public void testStaticMethodsUnrestrictedInTemplate() throws IOException {
        activateStaticsAccessRules("*");
        currentTemplate = "test_custom_static_content.ftl";
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        outputFormat.write(fcType, getFeatureInfoRequest, outStream);
        String result = new String(outStream.toByteArray());
        assertEquals(String.format("Amount: %.2f €", 47.11), result);
    }

    /** Verifies calls to static methods are disabled by default. */
    @Test(expected = IOException.class)
    public void testStaticMethodsDisabledInTemplate() throws IOException {
        activateStaticsAccessRules(null);
        currentTemplate = "test_custom_static_content.ftl";
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        outputFormat.write(fcType, getFeatureInfoRequest, outStream);
    }

    // for test below: Name has to be duplicate of existing class
    public static final class Locale {
        public static String m() {
            return "Hello world";
        }
    }

    /** Verifies calls to static methods for are enabled for specified classes. */
    @Test
    public void testSpecifiedStaticMethodsInTemplateAvailable() throws IOException {
        activateStaticsAccessRules(java.util.Locale.class.getName() + "," + Locale.class.getName());
        currentTemplate = "test_custom_static_content_specified.ftl";
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        outputFormat.write(fcType, getFeatureInfoRequest, outStream);
        String result = new String(outStream.toByteArray());
        assertEquals("Hello world from de", result);
    }

    /**
     * Ensures the template processing works when one query layer is resolved to multiple featureCollections
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testMultipleFeatureCollectionsPerQueryLayer() throws Exception {
        currentTemplate = "test_content_multi_ft.ftl";
        // given: further featureType and feature instance
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(toName(MockData.WORLD));
        SimpleFeature feature = SimpleFeatureBuilder.build(builder.buildFeatureType(), new Double[0], "");

        // given: further featureCollection in result data
        fcType.getFeature().add(DataUtilities.collection(feature));

        // when: processing template
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        outputFormat.write(fcType, getFeatureInfoRequest, outStream);
        String result = new String(outStream.toByteArray());

        // then: assert template was processed as expected
        assertTrue(result.contains("Type: PrimitiveGeoFeature"));
        assertTrue(result.contains("Type: World"));
    }

    @Test
    public void testAutoEscaping() throws Exception {
        currentTemplate = "test_resource_content.ftl";
        String decoded = "<foo>bar</foo>";
        String encoded = "&lt;foo&gt;bar&lt;/foo&gt;";
        ResourceInfo resource = getFeatureTypeInfo(MockData.PRIMITIVEGEOFEATURE);
        resource.getKeywords().set(0, new Keyword(decoded));
        getCatalog().save(resource);

        // test with no system property defined (default is true) and WMS setting disabled
        // result will be escaped
        doTestAutoEscaping(null, encoded, decoded);

        // test with system property set to true and WMS setting disabled
        // result will be escaped
        doTestAutoEscaping("true", encoded, decoded);

        // test with system property set to false and WMS setting disabled
        // result will not be escaped
        doTestAutoEscaping("false", decoded, encoded);

        WMSInfo info = getGeoServer().getService(WMSInfo.class);
        info.setAutoEscapeTemplateValues(true);
        getGeoServer().save(info);
        try {
            // test with no system property defined (default is true) and WMS setting enabled
            // result will be escaped
            doTestAutoEscaping(null, encoded, decoded);

            // test with system property set to true and WMS setting enabled
            // result will be escaped
            doTestAutoEscaping("true", encoded, decoded);

            // test with system property set to false and WMS setting enabled
            // result will be escaped
            doTestAutoEscaping("false", encoded, decoded);
        } finally {
            info.setAutoEscapeTemplateValues(false);
            getGeoServer().save(info);
        }
    }

    private void doTestAutoEscaping(String property, String contains, String notContains) throws Exception {
        if (property != null) {
            System.setProperty(FreeMarkerTemplateManager.FORCE_FREEMARKER_ESCAPING, property);
        } else {
            System.clearProperty(FreeMarkerTemplateManager.FORCE_FREEMARKER_ESCAPING);
        }
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        outputFormat.write(fcType, getFeatureInfoRequest, outStream);
        String result = new String(outStream.toByteArray());
        assertThat(result, not(containsString(notContains)));
        assertThat(result, containsString(contains));
    }

    /** Restore FreeMarkerTemplateManager default state */
    @After
    @Before
    public void tearDownStaticAccessKey() {
        activateStaticsAccessRules(null);
    }

    /**
     * Activates the rule for the given pattern by re-initializing the {@link FreeMarkerTemplateManager}.
     *
     * @param aPattern
     */
    private void activateStaticsAccessRules(String aPattern) {
        if (aPattern == null) {
            System.clearProperty(FreeMarkerTemplateManager.KEY_STATIC_MEMBER_ACCESS);
        } else {
            System.setProperty(FreeMarkerTemplateManager.KEY_STATIC_MEMBER_ACCESS, aPattern);
        }
        FreeMarkerTemplateManager.initStaticsAccessRule();
    }

    @Test
    public void testBlockSensitive1() throws IOException {
        doTestSensitive("test_block_sensitive_1.ftl", true);
    }

    @Test
    public void testBlockSensitive2() throws IOException {
        doTestSensitive("test_block_sensitive_2.ftl", true);
    }

    @Test
    public void testBlockSensitive3() throws IOException {
        doTestSensitive("test_block_sensitive_3.ftl", true);
    }

    @Test
    public void testCustomBlockList() throws IOException {
        // access to the namespace is allowed by default
        doTestSensitive("test_block_custom.ftl", false);
        // set the property to block access to the namespace
        System.setProperty(GeoServerMemberAccessPolicy.FREEMARKER_BLOCK_LIST, NamespaceInfo.class.getName());
        doTestSensitive("test_block_custom.ftl", true);
    }

    @Test
    public void testBlockApi() throws IOException {
        // access to non-getter methods is blocked by default
        doTestSensitive("test_block_api.ftl", true);
        // set the property to allow access to non-getter methods
        System.setProperty(GeoServerMemberAccessPolicy.FREEMARKER_API_EXPOSED, "true");
        doTestSensitive("test_block_api.ftl", false);
    }

    private void doTestSensitive(String template, boolean exception) throws IOException {
        FreeMarkerTemplateManager.clearClassIntrospectionCache();
        currentTemplate = template;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (exception) {
            IOException e =
                    assertThrows(IOException.class, () -> outputFormat.write(fcType, getFeatureInfoRequest, out));
            assertThat(e.getMessage(), containsString("Error occurred processing content template content.ftl"));
        } else {
            outputFormat.write(fcType, getFeatureInfoRequest, out);
        }
    }
}
