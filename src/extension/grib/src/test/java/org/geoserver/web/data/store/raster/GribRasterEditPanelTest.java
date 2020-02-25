/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.raster;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import javax.xml.namespace.QName;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.grib.GribDataTest;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.store.CoverageStoreEditPage;
import org.geoserver.web.data.store.CoverageStoreNewPage;
import org.geotools.coverage.io.grib.GRIBFormat;
import org.junit.Before;
import org.junit.Test;

public class GribRasterEditPanelTest extends GeoServerWicketTestSupport {

    protected static QName SAMPLE_GRIB =
            new QName(MockData.SF_URI, "sampleGrib", MockData.SF_PREFIX);

    @Before
    public void loginBefore() {
        super.login();
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpRasterLayer(
                SAMPLE_GRIB, "test-data/sampleGrib.grb2", null, null, GribDataTest.class);
    }

    @Test
    public void testGribCreate() throws Exception {
        Page page = tester.startPage(new CoverageStoreNewPage(new GRIBFormat().getName()));
        tester.assertNoErrorMessage();
        print(page, true, true);
        Component editor =
                tester.getComponentFromLastRenderedPage("rasterStoreForm:parametersPanel");
        assertThat(editor, instanceOf(GribRasterEditPanel.class));
    }

    @Test
    public void testGribEdit() throws Exception {
        CoverageStoreInfo store =
                getCatalog()
                        .getCoverageStoreByName(
                                SAMPLE_GRIB.getPrefix(), SAMPLE_GRIB.getLocalPart());
        assertNotNull(store);
        Page page = tester.startPage(new CoverageStoreEditPage(store));
        tester.assertNoErrorMessage();
        print(page, true, true);
        Component editor =
                tester.getComponentFromLastRenderedPage("rasterStoreForm:parametersPanel");
        assertThat(editor, instanceOf(GribRasterEditPanel.class));
    }
}
