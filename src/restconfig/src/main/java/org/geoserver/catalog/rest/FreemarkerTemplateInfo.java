/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import org.geoserver.platform.resource.Resource;


public class FreemarkerTemplateInfo {

    private String name;

    public FreemarkerTemplateInfo(Resource file) {
        name = file.name();
    }

    public String getName() {
        return name;
    }
}
