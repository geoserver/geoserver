/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.resource;

import org.geoserver.platform.resource.Resource;

/**
 * Tracks and cleans up a GeoServer resource
 *
 * @author Andrea Aime - GeoSolutions
 */
public class WPSResourceResource implements WPSResource {
    Resource resource;

    public WPSResourceResource(Resource resource) {
        this.resource = resource;
    }

    public void delete() throws Exception {
        resource.delete();
    }

    public String getName() {
        return resource.path();
    }
}
