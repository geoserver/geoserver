/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import org.geoserver.config.ServiceFactoryExtension;

/** Factory extension for OSEOInfoImpl */
public class OSEOFactoryExtension extends ServiceFactoryExtension<OSEOInfo> {
    /** Creates a new instance of OSEOFactoryExtension */
    public OSEOFactoryExtension() {
        super(OSEOInfo.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> clazz) {
        return (T) new OSEOInfoImpl();
    }
}
