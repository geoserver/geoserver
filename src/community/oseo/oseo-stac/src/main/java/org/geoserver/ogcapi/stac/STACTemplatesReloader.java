/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import org.geoserver.opensearch.eo.TemplatesReloader;
import org.springframework.stereotype.Component;

/** Forces reload of the templates on big configuration changes. */
@Component
public class STACTemplatesReloader extends TemplatesReloader {
    STACTemplates templates;

    public STACTemplatesReloader(STACTemplates templates) {
        super(templates);
    }
}
