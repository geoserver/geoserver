/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.io.File;


public class FreemarkerTemplateInfo {

    private String name;

    public FreemarkerTemplateInfo(File file) {
        name = file.getName();
    }

    public String getName() {
        return name;
    }
}
