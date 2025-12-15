/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import org.geotools.xml.transform.TransformerBase;

/** Factory class for GetCapabilities transformers. */
public interface GetCapabilitiesTransformerFactory {

    /**
     * Create a GetCapabilities transformer for the given request, or returns null if this factory cannot handle the
     * version requested.
     */
    TransformerBase createTransformer(GetCapabilitiesRequest request);
}
