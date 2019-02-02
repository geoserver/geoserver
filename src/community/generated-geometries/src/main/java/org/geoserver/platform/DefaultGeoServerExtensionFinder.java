/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.platform;

import java.util.List;

public class DefaultGeoServerExtensionFinder implements GeoServerExtensionFinder {

    private static final long serialVersionUID = 1L;

    @Override
    public <T> List<T> find(Class<T> clazz) {
        return GeoServerExtensions.extensions(clazz);
    }
}
