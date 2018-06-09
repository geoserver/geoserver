/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers.configuration;

import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerResourceLoader;

/** Loader for the test service. */
public class JmsTestServiceLoader extends XStreamServiceLoader<JmsTestServiceInfo> {

    public JmsTestServiceLoader(GeoServerResourceLoader resourceLoader) {
        super(resourceLoader, null);
    }

    @Override
    public Class<JmsTestServiceInfo> getServiceClass() {
        return JmsTestServiceInfo.class;
    }

    @Override
    protected JmsTestServiceInfo createServiceFromScratch(GeoServer gs) {
        return new JmsTestServiceInfoImpl();
    }
}
