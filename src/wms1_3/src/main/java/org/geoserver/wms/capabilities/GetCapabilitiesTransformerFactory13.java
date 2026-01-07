/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import java.util.Collection;
import org.geoserver.wms.ExtendedCapabilitiesProvider;
import org.geoserver.wms.GetCapabilitiesRequest;
import org.geoserver.wms.GetCapabilitiesTransformerFactory;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.WMS;
import org.geotools.util.Version;
import org.geotools.xml.transform.TransformerBase;

public class GetCapabilitiesTransformerFactory13 implements GetCapabilitiesTransformerFactory {

    private final WMS wms;

    public GetCapabilitiesTransformerFactory13(WMS wms) {
        this.wms = wms;
    }

    @Override
    public TransformerBase createTransformer(GetCapabilitiesRequest request) {
        final Version version = WMS.version(request.getVersion(), WMS.VERSION_1_3_0);

        if (WMS.VERSION_1_3_0.equals(version)) {
            String baseUrl = request.getBaseUrl();
            Collection<GetMapOutputFormat> mapFormats = wms.getAllowedMapFormats();
            Collection<ExtendedCapabilitiesProvider> extCapsProviders = wms.getAvailableExtendedCapabilitiesProviders();
            Capabilities_1_3_0_Transformer transformer =
                    new Capabilities_1_3_0_Transformer(wms, baseUrl, mapFormats, extCapsProviders);
            transformer.setIncludeRootLayer(request.isRootLayerEnabled());
            return transformer;
        }

        return null;
    }
}
