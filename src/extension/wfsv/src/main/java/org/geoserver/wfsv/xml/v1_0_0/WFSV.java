/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfsv.xml.v1_0_0;

import java.io.IOException;
import java.util.Set;

import org.eclipse.xsd.XSDSchema;
import org.geoserver.wfs.xml.v1_0_0.WFS;
import org.geotools.xml.XSD;

/**
 * This interface contains the qualified names of all the types,elements, and 
 * attributes in the http://www.opengis.net/wfsv schema.
 *
 * @generated
 */
public class WFSV extends XSD {
    
    // if you're looking for the elements containts they are declared in the v1.1.0 brother
    // of this class
    
    /** wfs dependency */
    WFS wfs;
    
    public WFSV(WFS wfs) {
        this.wfs = wfs;
    }
    
    protected void addDependencies(Set dependencies) {
        super.addDependencies(dependencies);
        
        dependencies.add( wfs );
    }
    
    public String getNamespaceURI() {
        return org.geoserver.wfsv.xml.v1_1_0.WFSV.NAMESPACE;
    }
    
    public String getSchemaLocation() {
        return getClass().getResource("WFS-versioning.xsd").toString();
    }
    
    protected XSDSchema buildSchema() throws IOException {
        XSDSchema wfsvSchema = super.buildSchema();
        wfsvSchema = wfs.getSchemaBuilder().addApplicationTypes(wfsvSchema);
        return wfsvSchema;
    }

}
        