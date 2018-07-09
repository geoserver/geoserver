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
import org.geoserver.netcdf.NetCDFDataTest;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.store.CoverageStoreEditPage;
import org.geoserver.web.data.store.CoverageStoreNewPage;
import org.geotools.coverage.io.netcdf.NetCDFFormat;
import org.junit.Before;
import org.junit.Test;

public class NetCDFRasterEditPanelTest extends GeoServerWicketTestSupport {

    protected static QName SAMPLE_NETCDF =
            new QName(MockData.SF_URI, "sampleNetCDF", MockData.SF_PREFIX);

    @Before
    public void loginBefore() {
        super.login();
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpRasterLayer(
                SAMPLE_NETCDF, "test-data/2DLatLonCoverage.nc", null, null, NetCDFDataTest.class);
    }

    @Test
    public void testNetCDFCreate() throws Exception {
        Page page = tester.startPage(new CoverageStoreNewPage(new NetCDFFormat().getName()));
        tester.assertNoErrorMessage();
        print(page, true, true);
        Component editor =
                tester.getComponentFromLastRenderedPage("rasterStoreForm:parametersPanel");
        assertThat(editor, instanceOf(NetCDFRasterEditPanel.class));
    }

    @Test
    public void testNetCDFEdit() throws Exception {
        CoverageStoreInfo store =
                getCatalog()
                        .getCoverageStoreByName(
                                SAMPLE_NETCDF.getPrefix(), SAMPLE_NETCDF.getLocalPart());
        assertNotNull(store);
        Page page = tester.startPage(new CoverageStoreEditPage(store));
        tester.assertNoErrorMessage();
        print(page, true, true);
        Component editor =
                tester.getComponentFromLastRenderedPage("rasterStoreForm:parametersPanel");
        assertThat(editor, instanceOf(NetCDFRasterEditPanel.class));
    }
}
