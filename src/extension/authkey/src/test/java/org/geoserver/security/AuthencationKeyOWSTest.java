/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2012 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import javax.servlet.Filter;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wms.WMSInfo;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class AuthencationKeyOWSTest extends GeoServerSystemTestSupport {

    private static String adminKey;

    private static String citeKey;

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        // setup their data access rights
        // namespace.layer.permission=role[,role2,...]
        File security = new File(testData.getDataDirectoryRoot(), "security");
        Properties props = new Properties();
        File layers = new File(security, "layers.properties");
        props = new Properties();
        props.put("mode", "hidden");
        props.put("*.*.r", "NO_ONE");
        props.put("*.*.w", "NO_ONE");
        props.put("sf.*.r", "*");
        props.put("cite.*.r", "cite");
        props.put("cite.*.w", "cite");
        FileOutputStream outputFile = new FileOutputStream(layers);
        try {
            props.store(outputFile, "");
        } finally {
            outputFile.close();
        }
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("wms", "http://www.opengis.net/wms");
        namespaces.put("ows", "http://www.opengis.net/ows");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        namespaces.put("", "http://www.opengis.net/ogc");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        CiteTestData.registerNamespaces(namespaces);
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));

        // setup limited srs
        GeoServer gs = getGeoServer();
        WMSInfo wms = gs.getService(WMSInfo.class);
        wms.getSRS().add("EPSG:4326");
        gs.save(wms);

        GeoServerUserGroupService service = getSecurityManager().loadUserGroupService("default");
        GeoServerUserGroupStore store = service.createStore();
        store.load();
        store.addUser(store.createUserObject("cite", "cite", true));
        store.store();

        GeoServerRoleService rservice = getSecurityManager().loadRoleService("default");
        GeoServerRoleStore rstore = rservice.createStore();
        rstore.load();
        GeoServerRole no_one = rstore.createRoleObject("NO_ONE");
        rstore.addRole(no_one);
        GeoServerRole rcite = rstore.createRoleObject("cite");
        rstore.addRole(rcite);
        rstore.associateRoleToUser(rstore.createRoleObject("cite"), "cite");
        rstore.store();

        String authKeyUrlParam = "authkey";
        String filterName = "testAuthKeyFilter1";

        AuthenticationKeyFilterConfig config = new AuthenticationKeyFilterConfig();
        config.setClassName(GeoServerAuthenticationKeyFilter.class.getName());
        config.setName(filterName);
        config.setUserGroupServiceName("default");
        config.setAuthKeyParamName(authKeyUrlParam);
        config.setAuthKeyMapperName("propertyMapper");
        getSecurityManager().saveFilter(config);

        SecurityManagerConfig mconfig = getSecurityManager().getSecurityConfig();
        GeoServerSecurityFilterChain filterChain = mconfig.getFilterChain();
        VariableFilterChain chain =
                (VariableFilterChain) filterChain.getRequestChainByName("default");
        chain.getFilterNames().add(0, filterName);
        getSecurityManager().saveSecurityConfig(mconfig);

        GeoServerAuthenticationKeyFilter authKeyFilter =
                (GeoServerAuthenticationKeyFilter) getSecurityManager().loadFilter(filterName);
        PropertyAuthenticationKeyMapper mapper =
                (PropertyAuthenticationKeyMapper) authKeyFilter.getMapper();
        mapper.synchronize();

        for (Entry<Object, Object> entry : mapper.authKeyProps.entrySet()) {
            if ("admin".equals(entry.getValue())) adminKey = (String) entry.getKey();
            if ("cite".equals(entry.getValue())) citeKey = (String) entry.getKey();
        }
        if (adminKey == null) throw new RuntimeException("Missing admin key");
        if (citeKey == null) throw new RuntimeException("Missing cite key");
    }

    /**
     * Enable the Spring Security authentication filters, we want the test to be complete and
     * realistic
     */
    @Override
    protected List<javax.servlet.Filter> getFilters() {

        SecurityManagerConfig mconfig = getSecurityManager().getSecurityConfig();
        GeoServerSecurityFilterChain filterChain = mconfig.getFilterChain();
        VariableFilterChain chain =
                (VariableFilterChain) filterChain.getRequestChainByName("default");
        List<Filter> result = new ArrayList<Filter>();
        for (String filterName : chain.getCompiledFilterNames()) {
            try {
                result.add(getSecurityManager().loadFilter(filterName));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    @Test
    public void testAnonymousCapabilities() throws Exception {

        Document doc = getAsDOM("wms?request=GetCapabilities&version=1.1.0");
        // print(doc);

        // check we have the sf layers, but not the cite ones not the cdf ones
        XpathEngine engine = XMLUnit.newXpathEngine();
        assertTrue(
                engine.getMatchingNodes("//Layer/Name[starts-with(text(), 'sf:')]", doc).getLength()
                        > 1);
        assertEquals(
                0,
                engine.getMatchingNodes("//Layer/Name[starts-with(text(), 'cite:')]", doc)
                        .getLength());
        assertEquals(
                0,
                engine.getMatchingNodes("//Layer/Name[starts-with(text(), 'cdf:')]", doc)
                        .getLength());
    }

    @Test
    public void testAdminCapabilities() throws Exception {
        Document doc = getAsDOM("wms?request=GetCapabilities&version=1.1.0&authkey=" + adminKey);
        // print(doc);

        // check we have all the layers
        XpathEngine engine = XMLUnit.newXpathEngine();
        assertTrue(
                engine.getMatchingNodes("//Layer/Name[starts-with(text(), 'sf:')]", doc).getLength()
                        > 1);
        assertTrue(
                engine.getMatchingNodes("//Layer/Name[starts-with(text(), 'cdf:')]", doc)
                                .getLength()
                        > 1);
        assertTrue(
                engine.getMatchingNodes("//Layer/Name[starts-with(text(), 'cite:')]", doc)
                                .getLength()
                        > 1);

        // check the authentication key has been propagated
        String url = engine.evaluate("//GetMap/DCPType/HTTP/Get/OnlineResource/@xlink:href", doc);
        assertTrue(url.contains("&authkey=" + adminKey));
    }

    @Test
    public void testCiteCapabilities() throws Exception {
        Document doc = getAsDOM("wms?request=GetCapabilities&version=1.1.0&authkey=" + citeKey);
        // print(doc);

        // check we have the sf and cite layers, but not cdf
        XpathEngine engine = XMLUnit.newXpathEngine();
        assertTrue(
                engine.getMatchingNodes("//Layer/Name[starts-with(text(), 'sf:')]", doc).getLength()
                        > 1);
        assertTrue(
                engine.getMatchingNodes("//Layer/Name[starts-with(text(), 'cite:')]", doc)
                                .getLength()
                        > 1);
        assertEquals(
                0,
                engine.getMatchingNodes("//Layer/Name[starts-with(text(), 'cdf:')]", doc)
                        .getLength());

        // check the authentication key has been propagated
        String url = engine.evaluate("//GetMap/DCPType/HTTP/Get/OnlineResource/@xlink:href", doc);
        assertTrue(url.contains("&authkey=" + citeKey));
    }

    @Test
    public void testAnonymousGetFeature() throws Exception {
        Document doc =
                getAsDOM(
                        "wfs?service=WFS&version=1.0.0&request=GetFeature&typeName="
                                + getLayerId(MockData.PONDS));
        assertEquals("ServiceExceptionReport", doc.getDocumentElement().getLocalName());
    }

    @Test
    public void testAdminGetFeature() throws Exception {
        Document doc =
                getAsDOM(
                        "wfs?service=WFS&version=1.0.0&request=GetFeature&typeName="
                                + getLayerId(MockData.PONDS)
                                + "&authkey="
                                + adminKey);
        // print(doc);
        assertXpathEvaluatesTo("1", "count(//wfs:FeatureCollection)", doc);

        XpathEngine engine = XMLUnit.newXpathEngine();
        String url = engine.evaluate("//wfs:FeatureCollection/@xsi:schemaLocation", doc);
        assertTrue(url.contains("&authkey=" + adminKey));
    }

    @Test
    public void testCiteGetFeature() throws Exception {
        Document doc =
                getAsDOM(
                        "wfs?service=WFS&version=1.0.0&request=GetFeature&typeName="
                                + getLayerId(MockData.PONDS)
                                + "&authkey="
                                + citeKey);
        // print(doc);
        assertXpathEvaluatesTo("1", "count(//wfs:FeatureCollection)", doc);

        XpathEngine engine = XMLUnit.newXpathEngine();
        String url = engine.evaluate("//wfs:FeatureCollection/@xsi:schemaLocation", doc);
        assertTrue(url.contains("&authkey=" + citeKey));
    }

    @Test
    public void testCiteGetFeatureCaseInsensitive() throws Exception {
        Document doc =
                getAsDOM(
                        "wfs?service=WFS&version=1.0.0&request=GetFeature&typeName="
                                + getLayerId(MockData.PONDS)
                                + "&AUTHKEY="
                                + citeKey);
        // print(doc);
        assertXpathEvaluatesTo("1", "count(//wfs:FeatureCollection)", doc);

        XpathEngine engine = XMLUnit.newXpathEngine();
        String url = engine.evaluate("//wfs:FeatureCollection/@xsi:schemaLocation", doc);
        assertTrue(url.contains("&authkey=" + citeKey));
    }

    /*
     * Tests that URLs in the OpenLayers Map are correctly generated (see: GEOS-7295)
     */
    @Test
    public void testOpenLayersMapOutput() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "cite/wms?service=WMS&"
                                + "version=1.1.0&"
                                + "request=GetMap&"
                                + "bbox=-2.0,2.0,-1.0,6.0&"
                                + "layers="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart()
                                + "&"
                                + "width=300&"
                                + "height=300&"
                                + "srs=EPSG:4326&"
                                + "format=application/openlayers"
                                + "&authkey="
                                + citeKey);
        byte[] responseContent = getBinary(response);
        String htmlDoc = new String(responseContent, "UTF-8");
        assertTrue(
                htmlDoc.indexOf("http://localhost:8080/geoserver/cite/wms?authkey=" + citeKey) > 0);
    }
}
