/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.web;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.config.GeoServer;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.security.EOCollectionAccessLimitInfo;
import org.geoserver.opensearch.eo.security.EOCollectionAccessLimitInfoImpl;
import org.geoserver.opensearch.eo.security.EOProductAccessLimitInfo;
import org.geoserver.opensearch.eo.security.EOProductAccessLimitInfoImpl;
import org.junit.Before;
import org.junit.Test;

public class OSEOSecurityPageTest extends OSEOWebTestSupport {

    private static final String COLLECTION_PREFIX = "form:collectionLimitsContainer:collectionLimits:listContainer";
    private static final String PRODUCT_PREFIX = "form:productLimitsContainer:productLimits:listContainer";
    private static final String ROLE_L8 = "ROLE_L8";
    private static final String ROLE_RECENT = "ROLE_RECENT";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_SNOWLESS = "ROLE_SNOWLESS";
    private static final String PLATFORM_L8 = "eo:platform='LANDSAT8'";
    private static final String PLATFORM_S2 = "eo:platform='SENTINEL2'";
    private static final String CLOUD_LT_10 = "opt:cloudCover < 10";
    private static final String SNOW_LT_10 = "opt:snowCover < 10";
    private static final String TIME_GT_20240101 = "timeStart > 2024-01-01";
    private static final String LANDSAT8 = "LANDSAT8";
    public static final String SENTINEL2 = "SENTINEL2";
    private GeoServer gs;

    @Before
    public void beforeTest() throws Exception {
        login();

        this.gs = getGeoServer();
        OSEOInfo oseo = gs.getService(OSEOInfo.class);
        List<EOCollectionAccessLimitInfo> collectionLimits = oseo.getCollectionLimits();
        collectionLimits.clear();
        collectionLimits.add(new EOCollectionAccessLimitInfoImpl(PLATFORM_L8, List.of(ROLE_L8)));
        collectionLimits.add(new EOCollectionAccessLimitInfoImpl(TIME_GT_20240101, List.of(ROLE_RECENT, ROLE_ADMIN)));
        List<EOProductAccessLimitInfo> productLimits = oseo.getProductLimits();
        productLimits.clear();
        productLimits.add(new EOProductAccessLimitInfoImpl(LANDSAT8, CLOUD_LT_10, List.of(ROLE_L8)));
        gs.save(oseo);

        ensureRolesAvailable(List.of(ROLE_L8, ROLE_RECENT, ROLE_SNOWLESS, ROLE_ADMIN));
    }

    @Test
    public void testRenderOSEOSecurityPage() {
        // Start and render the OSEOSecurityPage
        tester.startPage(OSEOSecurityPage.class);
        tester.assertRenderedPage(OSEOSecurityPage.class);

        // check that the collection limits are displayed
        tester.assertModelValue(collectionFilterPath(1), PLATFORM_L8);
        tester.assertModelValue(collectionRolesPath(1), ROLE_L8);
        tester.assertModelValue(collectionFilterPath(2), TIME_GT_20240101);
        tester.assertModelValue(collectionRolesPath(2), ROLE_RECENT + ", " + ROLE_ADMIN);

        // check that the product limits are displayed
        tester.assertModelValue(productCollectionPath(1), LANDSAT8);
        tester.assertModelValue(productFilterPath(1), CLOUD_LT_10);
        tester.assertModelValue(productRolesPath(1), ROLE_L8);
    }

    @Test
    public void testAddNewLimits() {
        // Start and render the OSEOSecurityPage
        tester.startPage(OSEOSecurityPage.class);
        tester.assertRenderedPage(OSEOSecurityPage.class);

        // open the editor for a collection limit
        tester.clickLink("form:addCollectionRule", true);

        String dialogFormPath = "dialog:dialog:modal:overlay:dialog:content:content:form";
        String panelFormPath = dialogFormPath + ":userPanel:form";
        FormTester collectionForm = tester.newFormTester(panelFormPath);
        collectionForm.setValue("cqlFilter", PLATFORM_S2);
        collectionForm.setValue("roles:palette:recorder", ROLE_ADMIN);
        tester.clickLink(dialogFormPath + ":submit", true);
        tester.assertNoErrorMessage();

        // check that the new collection limit is present (item ids are not reused)
        tester.assertModelValue(collectionFilterPath(5), PLATFORM_S2);
        tester.assertModelValue(collectionRolesPath(5), ROLE_ADMIN);

        // open the editor for a product limit
        tester.clickLink("form:addProductRule", true);
        FormTester productForm = tester.newFormTester(panelFormPath);
        int l8Index = collectionNames.indexOf(LANDSAT8);
        productForm.select("collectionContainer:collection", l8Index);
        productForm.setValue("cqlFilter", SNOW_LT_10);
        productForm.setValue("roles:palette:recorder", ROLE_SNOWLESS);
        tester.clickLink(dialogFormPath + ":submit", true);
        tester.assertNoErrorMessage();

        print(tester.getLastRenderedPage(), true, true);

        // check that the new product limit is present
        tester.assertModelValue(productCollectionPath(3), LANDSAT8);
        tester.assertModelValue(productFilterPath(3), SNOW_LT_10);
        tester.assertModelValue(productRolesPath(3), ROLE_SNOWLESS);

        // save the page
        tester.clickLink("form:submit", true);

        // check that the limits have been persisted
        OSEOInfo oseo = gs.getService(OSEOInfo.class);
        List<EOCollectionAccessLimitInfo> collectionLimits = oseo.getCollectionLimits();
        assertEquals(3, collectionLimits.size());
        EOCollectionAccessLimitInfo newCollectionLimit = collectionLimits.get(2);
        assertEquals(PLATFORM_S2, newCollectionLimit.getCQLFilter());
        assertEquals(List.of(ROLE_ADMIN), newCollectionLimit.getRoles());

        List<EOProductAccessLimitInfo> productLimits = oseo.getProductLimits();
        assertEquals(2, productLimits.size());
        EOProductAccessLimitInfo newProductLimit = productLimits.get(1);
        assertEquals(LANDSAT8, newProductLimit.getCollection());
        assertEquals(SNOW_LT_10, newProductLimit.getCQLFilter());
        assertEquals(List.of(ROLE_SNOWLESS), newProductLimit.getRoles());
    }

    @Test
    public void testEditCollectionLimit() throws Exception {
        // Start and render the OSEOSecurityPage
        tester.startPage(OSEOSecurityPage.class);
        tester.assertRenderedPage(OSEOSecurityPage.class);

        // edit the first collection limit
        tester.clickLink(
                "form:collectionLimitsContainer:collectionLimits:listContainer:items:1:itemProperties:3:component:link",
                true);
        tester.assertNoErrorMessage();

        String dialogFormPath = "dialog:dialog:modal:overlay:dialog:content:content:form";
        String panelFormPath = dialogFormPath + ":userPanel:form";
        FormTester collectionForm = tester.newFormTester(panelFormPath);
        collectionForm.setValue("cqlFilter", PLATFORM_S2); // was platform_l8
        collectionForm.setValue("roles:palette:recorder", ROLE_ADMIN);
        tester.clickLink(dialogFormPath + ":submit", true);
        tester.assertNoErrorMessage();

        // save the page
        tester.clickLink("form:submit", true);

        // check that the limits have been updated
        OSEOInfo oseo = gs.getService(OSEOInfo.class);
        List<EOCollectionAccessLimitInfo> collectionLimits = oseo.getCollectionLimits();
        assertEquals(2, collectionLimits.size());
        EOCollectionAccessLimitInfo editedCollectionLimit = collectionLimits.get(0);
        assertEquals(PLATFORM_S2, editedCollectionLimit.getCQLFilter());
        assertEquals(List.of(ROLE_ADMIN), editedCollectionLimit.getRoles());
    }

    @Test
    public void testEditProductLimit() throws Exception {
        // Start and render the OSEOSecurityPage
        tester.startPage(OSEOSecurityPage.class);
        tester.assertRenderedPage(OSEOSecurityPage.class);

        // edit the first product limit
        tester.clickLink(
                "form:productLimitsContainer:productLimits:listContainer:items:1:itemProperties:4:component:link",
                true);
        tester.assertNoErrorMessage();

        String dialogFormPath = "dialog:dialog:modal:overlay:dialog:content:content:form";
        String panelFormPath = dialogFormPath + ":userPanel:form";
        FormTester productForm = tester.newFormTester(panelFormPath);
        int s2Index = collectionNames.indexOf(SENTINEL2); // was LANDSAT8
        productForm.select("collectionContainer:collection", s2Index); // was LANDSAT8
        productForm.setValue("cqlFilter", SNOW_LT_10); // was CLOUD_LT_10
        productForm.setValue("roles:palette:recorder", ROLE_SNOWLESS);
        tester.clickLink(dialogFormPath + ":submit", true);
        tester.assertNoErrorMessage();

        // save the page
        tester.clickLink("form:submit", true);
        // check that the limits have been updated
        OSEOInfo oseo = gs.getService(OSEOInfo.class);
        List<EOProductAccessLimitInfo> productLimits = oseo.getProductLimits();
        assertEquals(1, productLimits.size());
        EOProductAccessLimitInfo editedProductLimit = productLimits.get(0);
        assertEquals(SENTINEL2, editedProductLimit.getCollection());
        assertEquals(SNOW_LT_10, editedProductLimit.getCQLFilter());
        assertEquals(List.of(ROLE_SNOWLESS), editedProductLimit.getRoles());
    }

    @Test
    public void testRemoveLimits() throws Exception {
        // Start and render the OSEOSecurityPage
        tester.startPage(OSEOSecurityPage.class);
        tester.assertRenderedPage(OSEOSecurityPage.class);

        // remove the first collection limit
        tester.clickLink(
                "form:collectionLimitsContainer:collectionLimits:listContainer:items:1:itemProperties:4:component:link",
                true);
        tester.assertNoErrorMessage();

        // remove the first product limit
        tester.clickLink(
                "form:productLimitsContainer:productLimits:listContainer:items:1:itemProperties:5:component:link",
                true);
        tester.assertNoErrorMessage();

        // save the page
        tester.clickLink("form:submit", true);

        // check that the limits have been removed
        OSEOInfo oseo = gs.getService(OSEOInfo.class);
        List<EOCollectionAccessLimitInfo> collectionLimits = oseo.getCollectionLimits();
        assertEquals(1, collectionLimits.size());
        EOCollectionAccessLimitInfo remainingCollectionLimit = collectionLimits.get(0);
        assertEquals(TIME_GT_20240101, remainingCollectionLimit.getCQLFilter());

        List<EOProductAccessLimitInfo> productLimits = oseo.getProductLimits();
        assertEquals(0, productLimits.size());
    }

    private static String collectionFilterPath(int itemIndex) {
        return "%s:items:%d:itemProperties:1:component".formatted(COLLECTION_PREFIX, itemIndex);
    }

    private static String collectionRolesPath(int itemIndex) {
        return "%s:items:%d:itemProperties:2:component".formatted(COLLECTION_PREFIX, itemIndex);
    }

    private static String productCollectionPath(int itemIndex) {
        return "%s:items:%d:itemProperties:1:component".formatted(PRODUCT_PREFIX, itemIndex);
    }

    private static String productFilterPath(int itemIndex) {
        return "%s:items:%d:itemProperties:2:component".formatted(PRODUCT_PREFIX, itemIndex);
    }

    private static String productRolesPath(int itemIndex) {
        return "%s:items:%d:itemProperties:3:component".formatted(PRODUCT_PREFIX, itemIndex);
    }
}
