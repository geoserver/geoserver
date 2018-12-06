/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ServiceResourceVoter;

/** Hides OpenSearch for EO from the list of services that */
public class OpenSearchServiceVoter implements ServiceResourceVoter {

    @Override
    public boolean hideService(String service, ResourceInfo resource) {
        // this service is not publishing layers, so it should not show up
        return "OSEO".equalsIgnoreCase(service);
    }
}
