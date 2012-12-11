/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import org.geotools.xml.Configuration;

public class ApplicationSchemaConfiguration2 extends Configuration {

    public ApplicationSchemaConfiguration2(ApplicationSchemaXSD2 xsd, Configuration config) {
        super(xsd);
        
        addDependency(config);
    }

}
