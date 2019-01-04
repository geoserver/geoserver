/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import org.geoserver.platform.resource.Resource;

/** Data transfer info object, used to transport template name. */
public class TemplateInfo {

    private String name;

    public TemplateInfo(Resource resource) {
        name = resource.name();
    }

    /** Resource name of Freemarker Template. */
    public String getName() {
        return name;
    }
}
