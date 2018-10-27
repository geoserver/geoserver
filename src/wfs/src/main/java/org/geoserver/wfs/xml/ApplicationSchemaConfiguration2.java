/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import org.geotools.xsd.Configuration;

public class ApplicationSchemaConfiguration2 extends Configuration {

    public ApplicationSchemaConfiguration2(ApplicationSchemaXSD2 xsd, Configuration config) {
        super(xsd);

        addDependency(config);
    }
}
