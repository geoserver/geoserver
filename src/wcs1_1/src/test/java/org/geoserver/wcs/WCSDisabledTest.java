/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.wcs.test.WCSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

public class WCSDisabledTest extends WCSTestSupport {

    @Test
    public void testDisabledServiceResponse() throws Exception {
        WCSInfo wcs = getGeoServer().getService(WCSInfo.class);
        wcs.setEnabled(false);
        getGeoServer().save(wcs);

        Document doc = getAsDOM("wcs?service=WCS&request=getCapabilities");
        assertEquals("ows:ExceptionReport", doc.getDocumentElement().getNodeName());
    }

    @Test
    public void testEnabledServiceResponse() throws Exception {
        enableWCS();

        Document doc = getAsDOM("wcs?service=WCS&request=getCapabilities");
        assertEquals("wcs:Capabilities", doc.getDocumentElement().getNodeName());
    }

    /** Tests WCS service disabled on layer-resource */
    @Test
    public void testDisableLayerServiceResponse() throws Exception {
        enableWCS();
        CoverageInfo cinfo = getCatalog().getCoverageByName("wcs:BlueMarble");
        cinfo.setServiceConfiguration(true);
        cinfo.setDisabledServices(new ArrayList<>(Arrays.asList("WCS")));
        getCatalog().save(cinfo);

        Document doc = getAsDOM("wcs?service=WCS&request=getCapabilities");
        assertXpathEvaluatesTo(
                "0", "count(//wcs:Contents/wcs:CoverageSummary/ows:Title[.='BlueMarble'])", doc);
    }

    /** Tests WCS service enabled on layer-resource */
    @Test
    public void testEnableLayerServiceResponse() throws Exception {
        enableWCS();
        CoverageInfo cinfo = getCatalog().getCoverageByName("wcs:BlueMarble");
        cinfo.setServiceConfiguration(false);
        cinfo.setDisabledServices(new ArrayList<>());
        getCatalog().save(cinfo);

        Document doc = getAsDOM("wcs?service=WCS&request=getCapabilities");
        assertXpathEvaluatesTo(
                "1", "count(//wcs:Contents/wcs:CoverageSummary/ows:Title[.='BlueMarble'])", doc);
    }

    private void enableWCS() {
        WCSInfo wcs = getGeoServer().getService(WCSInfo.class);
        wcs.setEnabled(true);
        getGeoServer().save(wcs);
    }
}
