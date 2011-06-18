package org.geoserver.wfs.xml;

import org.geotools.xml.Configuration;

public class ApplicationSchemaConfiguration2 extends Configuration {

    public ApplicationSchemaConfiguration2(ApplicationSchemaXSD2 xsd, Configuration config) {
        super(xsd);
        
        addDependency(config);
    }

}
