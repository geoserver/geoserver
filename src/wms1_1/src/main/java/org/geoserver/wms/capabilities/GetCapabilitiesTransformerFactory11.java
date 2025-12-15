/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import java.util.List;
import java.util.Set;
import org.geoserver.wms.ExtendedCapabilitiesProvider;
import org.geoserver.wms.GetCapabilitiesRequest;
import org.geoserver.wms.GetCapabilitiesTransformerFactory;
import org.geoserver.wms.WMS;
import org.geotools.util.Version;
import org.geotools.xml.transform.TransformerBase;

public class GetCapabilitiesTransformerFactory11 implements GetCapabilitiesTransformerFactory {

    private final WMS wms;

    public GetCapabilitiesTransformerFactory11(WMS wms) {
        this.wms = wms;
    }

    @Override
    public TransformerBase createTransformer(GetCapabilitiesRequest request) {
        final Version version = WMS.version(request.getVersion(), WMS.VERSION_1_3_0);

        if (WMS.VERSION_1_1_1.equals(version) || WMS.VERSION_1_0_0.equals(version)) {
            Set<String> legendFormats = wms.getAvailableLegendGraphicsFormats();
            String baseUrl = request.getBaseUrl();
            Set<String> mapFormats = wms.getAllowedMapFormatNames();
            List<ExtendedCapabilitiesProvider> extCapsProviders = wms.getAvailableExtendedCapabilitiesProviders();
            GetCapabilitiesTransformer transformer =
                    new GetCapabilitiesTransformer(wms, baseUrl, mapFormats, legendFormats, extCapsProviders);
            transformer.setIncludeRootLayer(request.isRootLayerEnabled());
            return transformer;
        }

        return null;
    }
}
