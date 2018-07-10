/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.wms;

import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.qos.xml.QosMainConfiguration;
import org.geoserver.qos.xml.WmsQosConfigurationTest;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wms.WMSInfo;
import org.junit.Test;

public class QosWMSCapabilitiesProviderTest extends GeoServerSystemTestSupport {

    private static final String WMS_1_1_1_GETCAPREQUEST =
            "wms?request=GetCapabilities&service=WMS&version=1.1.1";
    private static final String WMS_1_3_0_GETCAPREQUEST =
            "wms?request=GetCapabilities&service=WMS&version=1.3.0";

    public QosWMSCapabilitiesProviderTest() {}

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {}

    @Test
    public void testWorkspaceQosMetadata() throws Exception {
        // getGeoServer().getCatalog().setDefaultWorkspace(getWorkspaceInfo());
        //        setupWSQosData();

        // final Document dom = getAsDOM(getGetCapabilitiesRequestPath());
        //        final NodeList nodeList =
        //                dom.getElementsByTagNameNS(QosSchema.QOS_WMS_NAMESPACE,
        // "ExtendedCapabilities");
        //        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 0,
        // nodeList.getLength());
    }

    private void setupWSQosData() throws Exception {
        //        setupSystemData();
        getLoader().setConfiguration(getServiceInfo(), buildQosMetadataConfig());
        getGeoServer().save(getServiceInfo());
    }

    protected QosMainConfiguration buildQosMetadataConfig() {
        return WmsQosConfigurationTest.buildConfigExample();
    }

    protected WmsQosConfigurationLoader getLoader() {
        WmsQosConfigurationLoader loader =
                (WmsQosConfigurationLoader) applicationContext.getBean("wmsQosConfigurationLoader");
        return loader;
    }

    protected WMSInfo getServiceInfo() {
        return getGeoServer().getService(WMSInfo.class);
    }

    protected WorkspaceInfo getWorkspaceInfo() {
        WorkspaceInfo wsInfo =
                getGeoServer().getCatalog().getWorkspaceByName(CiteTestData.SF_PREFIX);
        return wsInfo;
    }

    protected String getGetCapabilitiesRequestPath() {
        return WMS_1_3_0_GETCAPREQUEST;
    }
}
