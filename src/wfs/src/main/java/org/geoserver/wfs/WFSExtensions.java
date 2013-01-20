/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.util.List;

import org.geoserver.ExtendedCapabilitiesProvider;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.context.ApplicationContext;

/**
 * Utility class uses to process GeoServer WFS extension points.
 * 
 * @author Jesse Eichar
 * @version $Id$
 */
public class WFSExtensions {
    /**
     * Looks up {@link ExtendedCapabilitiesProvider} extensions.
     */
    public static List<WFSExtendedCapabilitiesProvider> findExtendedCapabilitiesProviders(
            final ApplicationContext applicationContext) {
        return GeoServerExtensions.extensions(WFSExtendedCapabilitiesProvider.class,
                applicationContext);
    }

}
