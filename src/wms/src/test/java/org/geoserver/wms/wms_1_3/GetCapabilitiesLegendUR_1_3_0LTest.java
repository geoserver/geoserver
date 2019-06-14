/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_3;

import java.util.HashSet;
import org.geoserver.wms.ExtendedCapabilitiesProvider;
import org.geoserver.wms.capabilities.Capabilities_1_3_0_Transformer;
import org.geoserver.wms.capabilities.GetCapabilitiesLegendURLTest;
import org.geotools.xml.transform.TransformerBase;

public class GetCapabilitiesLegendUR_1_3_0LTest extends GetCapabilitiesLegendURLTest {

    @Override
    protected TransformerBase createTransformer() {
        return new Capabilities_1_3_0_Transformer(
                wmsConfig,
                baseUrl,
                wmsConfig.getAllowedMapFormats(),
                new HashSet<ExtendedCapabilitiesProvider>());
    }

    @Override
    protected String getRootElement() {
        return "WMS_Capabilities";
    }

    @Override
    protected String getElementPrefix() {
        return "wms:";
    }
}
