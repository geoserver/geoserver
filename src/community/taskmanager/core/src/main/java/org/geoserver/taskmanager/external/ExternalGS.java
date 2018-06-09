/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external;

import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import java.net.MalformedURLException;
import java.net.URL;
import org.geoserver.taskmanager.util.Named;

/**
 * External GeoServer.
 *
 * @author Niels Charlier
 */
public interface ExternalGS extends Named {

    String getUrl();

    String getUsername();

    String getPassword();

    default GeoServerRESTManager getRESTManager() throws MalformedURLException {
        return new GeoServerRESTManager(new URL(getUrl()), getUsername(), getPassword());
    }
}
