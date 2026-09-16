/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2015 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.config;

import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;
import org.springframework.stereotype.Component;

/** @author ETj (etj at geo-solutions.it) */
@Component
public class GeoFenceXStreamPersisterInitializer implements XStreamPersisterInitializer {

    @Override
    public void init(XStreamPersister persister) {}
}
