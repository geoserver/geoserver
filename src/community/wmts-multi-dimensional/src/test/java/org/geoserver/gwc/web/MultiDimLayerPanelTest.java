/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import java.util.Collections;
import javax.xml.namespace.QName;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.wmts.MultiDimensionalExtension;
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
    }

    @Test
    public void testExtensionPanel() {
        LayerInfo waterTemp = getCatalog().getLayerByName(getLayerId(WATERTEMP));
        MetadataMap metadata = waterTemp.getResource().getMetadata();
        metadata.put(MultiDimensionalExtension.EXPAND_LIMIT_KEY, "50");
        metadata.put(MultiDimensionalExtension.EXPAND_LIMIT_MAX_KEY, "100");
        MultiDimLayerPanel panel =
                tester.startComponentInPage(new MultiDimLayerPanel("foo", new Model<>(waterTemp)));
        // print(tester.getLastRenderedPage(), true, true, true);
        tester.assertNoErrorMessage();
        tester.assertModelValue("foo:defaultExpandLimit", "50");
        tester.assertModelValue("foo:maxExpandLimit", "100");
    }
}
