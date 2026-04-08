/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import org.geotools.wfs.WFSConfiguration;
import org.geotools.xsd.Configuration;

/**
 * XML configuration for a geoserver application schema.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class ApplicationSchemaConfiguration extends Configuration {

    public ApplicationSchemaConfiguration(ApplicationSchemaXSD xsd, WFSConfiguration config) {
        super(xsd);

        addDependency(config);
    }
}
