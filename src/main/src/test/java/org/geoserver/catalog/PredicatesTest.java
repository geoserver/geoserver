/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import static org.geoserver.catalog.Predicates.contains;
import static org.geoserver.catalog.Predicates.equal;
import static org.junit.Assert.*;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.geoserver.catalog.impl.AuthorityURL;
import org.geoserver.catalog.impl.CoverageInfoImpl;
import org.geoserver.catalog.impl.CoverageStoreInfoImpl;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.MultiValuedFilter.MatchAction;

public class PredicatesTest {

    private WorkspaceInfoImpl ws;

    private NamespaceInfoImpl ns;

    private DataStoreInfoImpl dataStore;

    private FeatureTypeInfoImpl featureType;

    private CoverageStoreInfoImpl coverageStore;

    private CoverageInfoImpl coverage;

    private LayerInfoImpl vectorLayer, rasterLayer;

    private StyleInfoImpl defaultStyle;

    private StyleInfoImpl style1;

    private StyleInfoImpl style2;

    @Before
    public void setUp() throws Exception {
        ns = new NamespaceInfoImpl();
        ns.setId("nsid");
        ns.setPrefix("test");
        ns.setURI("http://example.com");

        ws = new WorkspaceInfoImpl();
        ws.setId("wsid");
        ws.setName("ws1");

        dataStore = new DataStoreInfoImpl(null);
        dataStore.getConnectionParameters().put("boolParam", Boolean.TRUE);
        dataStore.getConnectionParameters().put("intParam", Integer.valueOf(1001));
        dataStore.getConnectionParameters().put("uriParam", new URI("http://some.place.net"));
        dataStore.setDescription("dataStore description");
        dataStore.setWorkspace(ws);
        dataStore.setName("dataStore");
        dataStore.setEnabled(true);
        dataStore.setId("dataStoreId");
        dataStore.setType("TestType");

        featureType = new FeatureTypeInfoImpl(null);
        featureType.setId("featureTypeId");
        featureType.setName("featureType");
        featureType.setNamespace(ns);
        featureType.setAbstract("featureType abstract");
        featureType.setEnabled(true);
        featureType.setMaxFeatures(5000);
        featureType.setStore(dataStore);
        featureType.setTitle("featureType title");

        vectorLayer = new LayerInfoImpl();
        vectorLayer.setResource(featureType);
        featureType.setAbstract("vectorLayer abstract");
        vectorLayer.setAdvertised(true);
        vectorLayer.setEnabled(true);
        vectorLayer.setName("vectorLayer");
        vectorLayer.setId("vectorLayerId");
        vectorLayer.setType(PublishedType.VECTOR);

        defaultStyle = new StyleInfoImpl(null);
        defaultStyle.setName("default");
        defaultStyle.setId("defaultStyle_id");
        vectorLayer.setDefaultStyle(defaultStyle);

        style1 = new StyleInfoImpl(null);
        style1.setName("style1");
        style1.setId("style1_id");

        style2 = new StyleInfoImpl(null);
        style2.setName("style2");
        style2.setId("style2_id");

        vectorLayer.getStyles().add(style1);
        vectorLayer.getStyles().add(style2);
    }

    @Test
    public void testPropertyEqualsSimple() {
        assertTrue(equal("prefix", ns.getPrefix()).evaluate(ns));
        assertTrue(equal("id", ws.getId()).evaluate(ws));
        assertFalse(equal("id", "somethingElse").evaluate(ws));

        Set<StyleInfo> styles = new HashSet<StyleInfo>();
        styles.add(style1);

        assertFalse(equal("styles", styles, MatchAction.ALL).evaluate(vectorLayer));
        assertTrue(equal("styles", styles, MatchAction.ANY).evaluate(vectorLayer));

        styles.add(style2);
        assertTrue(equal("styles", styles).evaluate(vectorLayer));
    }

    @Test
    public void testPropertyNotEqualsSimple() {
        assertTrue(Predicates.notEqual("id", "somethingElse").evaluate(ws));
    }

    @Test
    public void testPropertyEqualsCompound() {
        assertTrue(equal("resource.id", featureType.getId()).evaluate(vectorLayer));
        assertTrue(
                equal("resource.maxFeatures", featureType.getMaxFeatures()).evaluate(vectorLayer));
        assertTrue(equal("resource.store.type", dataStore.getType()).evaluate(vectorLayer));

        assertTrue(
                equal("resource.store.connectionParameters.boolParam", true).evaluate(vectorLayer));
        assertFalse(
                equal("resource.store.connectionParameters.boolParam", false)
                        .evaluate(vectorLayer));

        ws.getMetadata().put("checkMe", new java.util.Date(1000));

        assertTrue(equal("metadata.checkMe", new java.util.Date(1000)).evaluate(ws));

        assertFalse(
                equal("resource.store.someNonExistentProperty", "someValue").evaluate(vectorLayer));
    }

    @Test
    public void testPropertyEqualsConverters() {

        Object expected;

        expected = featureType.getMaxFeatures();
        assertTrue(equal("resource.maxFeatures", expected).evaluate(vectorLayer));

        expected = String.valueOf(featureType.getMaxFeatures());
        assertTrue(equal("resource.maxFeatures", expected).evaluate(vectorLayer));

        expected = Double.valueOf(featureType.getMaxFeatures());
        assertTrue(equal("resource.maxFeatures", expected).evaluate(vectorLayer));

        expected = "true";
        assertTrue(
                equal("resource.store.connectionParameters.boolParam", expected)
                        .evaluate(vectorLayer));

        expected = "false";
        assertFalse(
                equal("resource.store.connectionParameters.boolParam", false)
                        .evaluate(vectorLayer));

        ws.getMetadata().put("checkMe", new java.util.Date(1000));

        expected = new java.sql.Timestamp(1000);
        assertTrue(
                equal("resource.store.workspace.metadata.checkMe", expected).evaluate(vectorLayer));

        assertFalse(
                equal("resource.store.someNonExistentProperty", "someValue").evaluate(vectorLayer));
    }

    @Test
    public void testPropertyEqualsIndexed() {

        AuthorityURLInfo aurl1 = new AuthorityURL();
        aurl1.setName("url1");
        AuthorityURLInfo aurl2 = new AuthorityURL();
        aurl2.setName("url2");
        AuthorityURLInfo aurl3 = new AuthorityURL();
        aurl3.setName("url3");
        vectorLayer.setAuthorityURLs(Arrays.asList(aurl1, aurl2, aurl3));

        assertTrue(equal("authorityURLs[1]", aurl1).evaluate(vectorLayer));
        assertTrue(equal("authorityURLs[1].name", aurl1.getName()).evaluate(vectorLayer));

        assertTrue(equal("authorityURLs[2]", aurl2).evaluate(vectorLayer));
        assertTrue(equal("authorityURLs[2].name", aurl2.getName()).evaluate(vectorLayer));

        assertTrue(equal("authorityURLs[3]", aurl3).evaluate(vectorLayer));
        assertTrue(equal("authorityURLs[3].name", aurl3.getName()).evaluate(vectorLayer));
    }

    @Test
    public void testPropertyEqualsAny() {
        assertTrue(equal("styles.id", style1.getId()).evaluate(vectorLayer));
        assertTrue(equal("styles.name", style2.getName()).evaluate(vectorLayer));
        assertFalse(equal("styles.id", "nonExistent").evaluate(vectorLayer));
    }

    @Test
    public void testContains() {
        assertTrue(contains("URI", "example").evaluate(ns));
        assertFalse(contains("resource.ns.URI", "example").evaluate(vectorLayer));
        assertTrue(contains("resource.namespace.URI", "example").evaluate(vectorLayer));

        assertTrue(contains("id", "vectorLayerId").evaluate(vectorLayer));
        assertTrue(contains("id", "vectorLayerID").evaluate(vectorLayer));
        assertTrue(contains("id", "torLAY").evaluate(vectorLayer));

        assertTrue(contains("styles.name", "style2").evaluate(vectorLayer));
        assertTrue(contains("styles.name", "Style2").evaluate(vectorLayer));
        assertTrue(contains("styles.name", "YL").evaluate(vectorLayer));
        assertFalse(contains("styles.name", "style3").evaluate(vectorLayer));

        String name = featureType.getName();
        assertTrue(contains("resource.name", name).evaluate(vectorLayer));
        assertFalse(contains("resource.name", "?").evaluate(vectorLayer));

        featureType.setName("name?.*$[]&()");
        assertTrue(contains("resource.name", "?").evaluate(vectorLayer));
        assertTrue(contains("resource.name", ".").evaluate(vectorLayer));
        assertTrue(contains("resource.name", "*").evaluate(vectorLayer));

        featureType.setName(null);

        assertFalse(contains("resource.name", name).evaluate(vectorLayer));
    }
}
