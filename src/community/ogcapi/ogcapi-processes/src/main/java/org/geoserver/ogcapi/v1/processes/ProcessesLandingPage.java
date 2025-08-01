/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import java.util.Optional;
import org.geoserver.ogcapi.AbstractLandingPageDocument;
import org.geoserver.ogcapi.LinksBuilder;
import org.geoserver.wps.WPSInfo;

/** Landing page of the OGC API Processes service */
public class ProcessesLandingPage extends AbstractLandingPageDocument {
    public static final String REL_PROCESSES = "http://www.opengis.net/def/rel/ogc/1.0/processes";

    public ProcessesLandingPage(WPSInfo wps, String basePath) {
        super(
                Optional.ofNullable(wps.getTitle()).orElse("Processes 1.0 server"),
                Optional.ofNullable(wps.getAbstract()).orElse(""),
                basePath);

        // processes
        new LinksBuilder(ProcessListDocument.class, basePath)
                .segment("/processes")
                .title("Processes Metadata as ")
                .rel(REL_PROCESSES)
                .add(this);
    }
}
