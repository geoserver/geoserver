/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.util.Arrays;
import java.util.TimeZone;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.dimension.DefaultValueConfiguration;
import org.geoserver.wms.dimension.DefaultValueConfigurations;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

public class WMSDynamicDimensionTestSupport extends WMSDimensionsTestSupport {

    protected static QName TIME_ELEVATION_CUSTOM =
            new QName(MockData.SF_URI, "time_elevation_custom", MockData.SF_PREFIX);

    protected XpathEngine xpath;

    @BeforeClass
    public static void setupTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }

    @AfterClass
    public static void resetTimeZone() {
        TimeZone.setDefault(null);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        Catalog catalog = getCatalog();
        testData.addRasterLayer(
                TIME_ELEVATION_CUSTOM,
                "time_elevation_custom.zip",
                null,
                null,
                WMSDynamicDimensionTestSupport.class,
                catalog);
    }

    @Before
    public void prepareXPathEngine() {
        xpath = XMLUnit.newXpathEngine();
    }

    @Before
    public void removeDynamicDimensions() throws Exception {
        removeDynamicDimensions("TimeElevation");
        removeDynamicDimensions(getLayerId(TIME_ELEVATION_CUSTOM));
    }

    public void removeDynamicDimensions(String resourceName) throws Exception {
        ResourceInfo ri = getCatalog().getResourceByName(resourceName, ResourceInfo.class);
        ri.getMetadata().remove(DefaultValueConfigurations.KEY);
        getCatalog().save(ri);
    }

    protected void setupDynamicDimensions(
            String resourceName, DefaultValueConfiguration... configurations) {
        ResourceInfo info = getCatalog().getResourceByName(resourceName, ResourceInfo.class);
        DefaultValueConfigurations configs =
                new DefaultValueConfigurations(Arrays.asList(configurations));
        info.getMetadata().put(DefaultValueConfigurations.KEY, configs);
        getCatalog().save(info);
    }

    protected void setupDynamicDimensions(
            QName resourceName, DefaultValueConfiguration... configurations) {
        setupDynamicDimensions(getLayerId(resourceName), configurations);
    }
}
