/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import org.geoserver.config.GeoServer;
import org.geoserver.web.ogcapi.OgcApiServiceDescriptionProvider;
import org.geoserver.wps.WPSInfo;

public class ProcessesServiceDescriptionProvider extends OgcApiServiceDescriptionProvider<WPSInfo, ProcessesService> {

    public ProcessesServiceDescriptionProvider(GeoServer gs) {
        super(gs, "WPS", "Processes", "Processes");
    }
}
