/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import static org.geoserver.gwc.wmts.MultiDimensionalExtension.SIDECAR_STORE;
import static org.geoserver.gwc.wmts.MultiDimensionalExtension.SIDECAR_TYPE;
import static org.geoserver.gwc.wmts.SidecarStoreTypeTest.MAINSTORE_URI;
import static org.geoserver.gwc.wmts.SidecarStoreTypeTest.SIDECAR_VECTOR_ET_SS;
import static org.geoserver.gwc.wmts.SidecarStoreTypeTest.VECTOR_ELEVATION_TIME_SS;
import static org.geoserver.gwc.wmts.TestsSupport.SIDECAR_VECTOR_ET;
import static org.geoserver.gwc.wmts.TestsSupport.VECTOR_ELEVATION;
import static org.geoserver.gwc.wmts.TestsSupport.minimumValue;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import javax.xml.namespace.QName;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.wmts.MultiDimensionalExtension;
import org.geoserver.gwc.wmts.TestsSupport;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class MultiDimLayerPanelTest extends GeoServerWicketTestSupport {

    protected static final QName WATERTEMP =
            new QName(MockData.SF_URI, "watertemp", MockData.SF_PREFIX);

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // do no setup common layers
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // raster with elevation dimension
        testData.addRasterLayer(
                WATERTEMP,
                "/org/geoserver/wms/dimension/watertemp.zip",
                null,
                Collections.emptyMap(),
                getClass(),
                getCatalog());
        setupRasterDimension(WATERTEMP, "time", DimensionPresentation.LIST, null, null, null);
        // vector with elevation dimension and its sidecar, in the same store
        testData.addVectorLayer(
                VECTOR_ELEVATION,
                Collections.emptyMap(),
                "/TimeElevationWithStartEnd.properties",
                this.getClass(),
                getCatalog());
        testData.addVectorLayer(
                SIDECAR_VECTOR_ET,
                Collections.emptyMap(),
                "/SidecarTimeElevationWithStartEnd.properties",
                this.getClass(),
                getCatalog());
        // now again, but in different stores
        testData.addVectorLayer(
                VECTOR_ELEVATION_TIME_SS,
                Collections.emptyMap(),
                "/TimeElevationWithStartEnd.properties",
                this.getClass(),
                getCatalog());
        testData.addVectorLayer(
                SIDECAR_VECTOR_ET_SS,
                Collections.emptyMap(),
                "/SidecarTimeElevationWithStartEnd.properties",
                this.getClass(),
                getCatalog());
        // one extra store to play interference and check validation
        testData.addVectorLayer(MockData.BUILDINGS, getCatalog());

        // setup dimensions
        FeatureTypeInfo timeElevationSidecar =
                getCatalog()
                        .getFeatureTypeByName(
                                MAINSTORE_URI, VECTOR_ELEVATION_TIME_SS.getLocalPart());
        TestsSupport.registerLayerDimension(
                timeElevationSidecar,
                ResourceInfo.ELEVATION,
                "startElevation",
                null,
                DimensionPresentation.CONTINUOUS_INTERVAL,
                minimumValue());
        TestsSupport.registerLayerDimension(
                timeElevationSidecar,
                ResourceInfo.TIME,
                "startTime",
                null,
                DimensionPresentation.CONTINUOUS_INTERVAL,
                minimumValue());
    }

    @Test
    public void testExtensionPanel() {
        LayerInfo waterTemp = getCatalog().getLayerByName(getLayerId(WATERTEMP));
        MetadataMap metadata = waterTemp.getResource().getMetadata();
        metadata.put(MultiDimensionalExtension.EXPAND_LIMIT_KEY, "50");
        metadata.put(MultiDimensionalExtension.EXPAND_LIMIT_MAX_KEY, "100");
        tester.startComponentInPage(new MultiDimLayerPanel("foo", new Model<>(waterTemp)));
        // print(tester.getLastRenderedPage(), true, true, true);
        tester.assertNoErrorMessage();
        tester.assertModelValue("foo:defaultExpandLimit", "50");
        tester.assertModelValue("foo:maxExpandLimit", "100");
    }

    /**
     * Simple sidecar setup, one store for both main and sidecar types (only one choice available)
     */
    @Test
    public void testSimpleSidecar() {
        LayerInfo vector = getCatalog().getLayerByName(getLayerId(VECTOR_ELEVATION));
        tester.startPage(new FormTestPage(id -> new MultiDimLayerPanel(id, new Model<>(vector))));

        // print(tester.getLastRenderedPage(), true, true, true);
        FormTester form = tester.newFormTester("form");
        form.select("panel:sidecarTypeContainer:sidecarType", 0);
        form.submit();

        tester.assertNoErrorMessage();

        MetadataMap metadata = vector.getResource().getMetadata();
        assertEquals(metadata.get(SIDECAR_TYPE), SIDECAR_VECTOR_ET.getLocalPart());
    }

    /**
     * Simple sidecar setup, one store for both main and sidecar types (only one choice available)
     */
    @Test
    public void testSeparateSidecar() {
        LayerInfo vector = getCatalog().getLayerByName(getLayerId(VECTOR_ELEVATION_TIME_SS));
        tester.startPage(new FormTestPage(id -> new MultiDimLayerPanel(id, new Model<>(vector))));

        // print(tester.getLastRenderedPage(), true, true, true);
        FormTester form = tester.newFormTester("form");
        // sidecar stores is the fourth entry, they are alphabetically sorted (predictable)
        form.select("panel:sidecarTypeContainer:sidecarStore", 3);
        tester.executeAjaxEvent("form:panel:sidecarTypeContainer:sidecarStore", "change");
        // have to set it again, the form tester lost the original panel due to re-rendering
        form.select("panel:sidecarTypeContainer:sidecarStore", 3);
        form.select("panel:sidecarTypeContainer:sidecarType", 0);
        form.submit();

        tester.assertNoErrorMessage();

        MetadataMap metadata = vector.getResource().getMetadata();
        assertEquals("sidestore:sidestore", metadata.get(SIDECAR_STORE));
        assertEquals(SIDECAR_VECTOR_ET_SS.getLocalPart(), metadata.get(SIDECAR_TYPE));
    }

    /**
     * Simple sidecar setup, one store for both main and sidecar types (only one choice available)
     */
    @Test
    public void testSidecarValidation() {
        LayerInfo vector = getCatalog().getLayerByName(getLayerId(VECTOR_ELEVATION_TIME_SS));
        tester.startPage(new FormTestPage(id -> new MultiDimLayerPanel(id, new Model<>(vector))));

        // print(tester.getLastRenderedPage(), true, true, true);
        FormTester form = tester.newFormTester("form");
        // unrelated store is the first entry, they are alphabetically sorted (predictable)
        form.select("panel:sidecarTypeContainer:sidecarStore", 0);
        tester.executeAjaxEvent("form:panel:sidecarTypeContainer:sidecarStore", "change");
        // have to set it again, the form tester lost the original panel due to re-rendering
        form.select("panel:sidecarTypeContainer:sidecarStore", 0);
        form.select("panel:sidecarTypeContainer:sidecarType", 0);
        form.submit();

        // should complain that the sidecar type does match the main type dimension attributes
        tester.assertErrorMessages(
                "Could not find attribute 'startElevation' in 'Buildings'",
                "Could not find attribute 'startTime' in 'Buildings'");
    }
}
