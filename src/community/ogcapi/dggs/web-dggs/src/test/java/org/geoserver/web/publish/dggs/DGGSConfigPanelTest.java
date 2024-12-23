/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.publish.dggs;

import static org.geoserver.data.test.MockData.DEFAULT_PREFIX;
import static org.geoserver.data.test.MockData.PONDS;
import static org.geotools.dggs.gstore.DGGSResolutionCalculator.CONFIGURED_MAXRES_KEY;
import static org.geotools.dggs.gstore.DGGSResolutionCalculator.CONFIGURED_MINRES_KEY;
import static org.geotools.dggs.gstore.DGGSResolutionCalculator.CONFIGURED_OFFSET_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geotools.dggs.gstore.DGGSGeometryStoreFactory;
import org.geotools.feature.NameImpl;
import org.junit.Before;
import org.junit.Test;

public class DGGSConfigPanelTest extends GeoServerWicketTestSupport {

    public static final String PUBLISHED_INFO = "publishedinfo";

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // don't set up the default layers
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // create a geometry DGGS store
        Catalog catalog = getCatalog();
        CatalogBuilder builder = new CatalogBuilder(catalog);
        DataStoreInfo dggs = builder.buildDataStore("dggs");
        dggs.setType("DGGS Geometry Store");
        dggs.getConnectionParameters().put(DGGSGeometryStoreFactory.DGGS_FACTORY_ID.key, "H3");
        catalog.add(dggs);

        // create a layer with a geometry DGGS store
        builder.setStore(dggs);
        FeatureTypeInfo fti = builder.buildFeatureType(new NameImpl("H3"));
        builder.setupBounds(fti);
        LayerInfo li = builder.buildLayer(fti);
        catalog.add(fti);
        catalog.add(li);

        // add one of the default layers as a non DGGS test case
        testData.addVectorLayer(PONDS, getCatalog());
    }

    @Before
    public void resetConfiguration() throws Exception {
        FeatureTypeInfo fti = getCatalog().getFeatureTypeByName("H3");
        fti.getMetadata().remove(CONFIGURED_OFFSET_KEY);
        fti.getMetadata().remove(CONFIGURED_MINRES_KEY);
        fti.getMetadata().remove(CONFIGURED_MAXRES_KEY);
        getCatalog().save(fti);
    }

    private String openDGGSPanel() {
        login();
        tester.startPage(new ResourceConfigurationPage(DEFAULT_PREFIX, "H3"));
        tester.assertNoErrorMessage();

        // switch to the publishing tab
        tester.clickLink("publishedinfo:tabs:tabs-container:tabs:1:link");
        tester.assertNoErrorMessage();

        Form form = (Form) tester.getComponentFromLastRenderedPage(PUBLISHED_INFO);
        String configPanelPath = getComponentPath(form, DGGSConfigPanel.class);
        assertNotNull(configPanelPath);
        // compute path relative to the form
        String baseFormPath = configPanelPath.substring(PUBLISHED_INFO.length() + 1);
        return baseFormPath;
    }

    @Test
    public void testPanelSave() {
        String baseFormPath = openDGGSPanel();

        // set some values and save
        FormTester formTester = tester.newFormTester(PUBLISHED_INFO);
        formTester.setValue(baseFormPath + ":resolutionOffset", "1");
        formTester.setValue(baseFormPath + ":minResolution", "2");
        formTester.setValue(baseFormPath + ":maxResolution", "5");
        formTester.submit();
        tester.clickLink("publishedinfo:save");
        tester.assertNoErrorMessage();

        FeatureTypeInfo fti = getCatalog().getFeatureTypeByName("H3");
        assertIntegerMetadataKey(fti, CONFIGURED_OFFSET_KEY, 1);
        assertIntegerMetadataKey(fti, CONFIGURED_MINRES_KEY, 2);
        assertIntegerMetadataKey(fti, CONFIGURED_MAXRES_KEY, 5);
    }

    private static void assertIntegerMetadataKey(FeatureTypeInfo fti, String key, int expectedValue) {
        assertEquals(Integer.valueOf(expectedValue), fti.getMetadata().get(key, Integer.class));
    }

    @Test
    public void testPanelNonDGGS() {
        login();
        tester.startPage(new ResourceConfigurationPage(PONDS.getPrefix(), PONDS.getLocalPart()));
        tester.assertNoErrorMessage();

        // switch to the publishing tab
        tester.clickLink("publishedinfo:tabs:tabs-container:tabs:1:link");
        tester.assertNoErrorMessage();

        // panel should not be found, not a DGGS layer
        Form form = (Form) tester.getComponentFromLastRenderedPage(PUBLISHED_INFO);
        String configPanelPath = getComponentPath(form, DGGSConfigPanel.class);
        assertNull(configPanelPath);
    }

    @Test
    public void testMinMaxIndividualValidation() {
        String baseFormPath = openDGGSPanel();

        // set values outside of the DGGS supported range (0-16 for H3)
        FormTester formTester = tester.newFormTester(PUBLISHED_INFO);
        formTester.setValue(baseFormPath + ":minResolution", "-1");
        formTester.setValue(baseFormPath + ":maxResolution", "25");
        formTester.submit();
        tester.assertErrorMessages(
                "The value of 'Minimum resolution in layer' must be at least 0.",
                "The value of 'Maximum resolution in layer' must be at most 15.");
    }

    @Test
    public void testMinMaxConsistencyValidation() {
        String baseFormPath = openDGGSPanel();

        // set values inside of the DGGS supported range, but min > max
        FormTester formTester = tester.newFormTester(PUBLISHED_INFO);
        formTester.setValue(baseFormPath + ":minResolution", "6");
        formTester.setValue(baseFormPath + ":maxResolution", "2");
        formTester.submit();
        tester.assertErrorMessages("DGGS minimum layer resolution but be less or equal than the maximum resolution.");
    }
}
