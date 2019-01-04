/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.nsg;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;

import java.util.Arrays;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.v2_0.WFS20TestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

public class WFSCapabilitiesTest extends WFS20TestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        GeoServer geoServer = getGeoServer();
        WFSInfo service = geoServer.getService(WFSInfo.class);
        service.getSRS().add("4326");
        service.getSRS().add("3395");
        geoServer.save(service);
    }

    @Test
    public void testCapabilitiesExtras() throws Exception {
        Document dom = getAsDOM("wfs?service=WFS&version=2.0.0&request=GetCapabilities");
        print(dom);

        // profiles
        assertXpathExists(
                "//ows:ServiceIdentification[ows:Profile='"
                        + NSGWFSExtendedCapabilitiesProvider.NSG_BASIC
                        + "']",
                dom);

        // constraints
        assertXpathExists(
                "//ows:OperationsMetadata/ows:Constraint[@name='"
                        + NSGWFSExtendedCapabilitiesProvider.IMPLEMENTS_FEATURE_VERSIONING
                        + "' and ows:DefaultValue='TRUE']",
                dom);
        assertXpathExists(
                "//ows:OperationsMetadata/ows:Constraint[@name='"
                        + NSGWFSExtendedCapabilitiesProvider.IMPLEMENTS_ENHANCED_PAGING
                        + "' and ows:DefaultValue='TRUE']",
                dom);

        // ensure "version" is there for all operations besides capabilities (10 basic plus paged
        // results)
        assertXpathEvaluatesTo(
                "11",
                "count(//ows:OperationsMetadata/ows:Operation[@name!='GetCapabilities']/ows"
                        + ":Parameter[@name='version']/ows:AllowedValues[ows:Value[1]='2.0.0'])",
                dom);

        // ensure the srsName parameter is available on GetFeature, GetFeatureWithLock, Transaction
        for (String operation : NSGWFSExtendedCapabilitiesProvider.SRS_OPERATIONS) {
            // the two configured SRSs plus one coming from the data
            for (Integer srsCode : Arrays.asList(4326, 3395, 32615)) {
                String xpath =
                        String.format(
                                "//ows:OperationsMetadata/ows:Operation[@name = "
                                        + "'%s']/ows:Parameter[@name='srsName' "
                                        + "and ows:AllowedValues/ows:Value='urn:ogc:def:crs:EPSG::%d']",
                                operation, srsCode);
                assertXpathExists(xpath, dom);
            }
        }

        // ensure the timeout parameter is configured on expected operations
        for (String operation : NSGWFSExtendedCapabilitiesProvider.TIMEOUT_OPERATIONS) {
            String xpath =
                    String.format(
                            "//ows:OperationsMetadata/ows:Operation[@name = "
                                    + "'%s']/ows:Parameter[@name='Timeout' and ows:DefaultValue='300']",
                            operation);
            assertXpathExists(xpath, dom);
        }

        // check the PageResults operation is there
        assertXpathExists("//ows:OperationsMetadata/ows:Operation[@name = 'PageResults']", dom);
        assertXpathExists(
                "//ows:OperationsMetadata/ows:Operation[@name = "
                        + "'PageResults']/ows:Parameter[@name='outputFormat' and ows:DefaultValue='"
                        + NSGWFSExtendedCapabilitiesProvider.GML32_FORMAT
                        + "']",
                dom);
    }
}
