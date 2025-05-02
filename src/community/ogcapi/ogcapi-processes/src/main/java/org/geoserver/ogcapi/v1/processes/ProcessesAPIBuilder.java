/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.IOException;
import org.geoserver.wps.WPSInfo;

/** Builds the OGC Processes OpenAPI document */
public class ProcessesAPIBuilder extends org.geoserver.ogcapi.OpenAPIBuilder<WPSInfo> {

    public ProcessesAPIBuilder() {
        super(ProcessesAPIBuilder.class, "openapi.yaml", "Processes 1.0 server", ProcessesService.class);
    }

    @Override
    public OpenAPI build(WPSInfo service) throws IOException {
        OpenAPI api = super.build(service);

        // the external documentation
        api.externalDocs(new ExternalDocumentation()
                .description("Processes specification")
                .url("https://docs.ogc.org/is/18-062r2/18-062r2.html"));

        return api;
    }
}
