/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import org.geoserver.wms.capabilities.GetCapabilitiesLegendURLTest;
import org.geoserver.wms.capabilities.GetCapabilitiesTransformer;
import org.geotools.xml.transform.TransformerBase;

public class GetCapabilitiesLegendURL_1_1_1Test extends GetCapabilitiesLegendURLTest {

    @Override
    protected TransformerBase createTransformer() {
        return new GetCapabilitiesTransformer(wmsConfig, baseUrl, mapFormats, legendFormats, null);
    }

    @Override
    protected String getRootElement() {
        return "WMT_MS_Capabilities";
    }

    @Override
    protected String getElementPrefix() {
        return "";
    }
}
