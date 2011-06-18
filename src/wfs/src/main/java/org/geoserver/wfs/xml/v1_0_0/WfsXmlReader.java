/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_0_0;

import java.io.Reader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.ows.XmlRequestReader;
import org.geoserver.wfs.WFSException;
import org.geotools.util.Version;
import org.geotools.xml.Configuration;
import org.geotools.xml.Parser;

/**
 * Xml reader for wfs 1.0.0 xml requests.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 *
 * TODO: there is too much duplication with the 1.1.0 reader, factor it out.
 */
public class WfsXmlReader extends XmlRequestReader {
    /**
     * Xml Configuration
     */
    Configuration configuration;
    /**
     * Catalog, to access namespaces
     */
    Catalog catalog;

    public WfsXmlReader(String element, Configuration configuration, Catalog catalog) {
        this(element, configuration, catalog, "wfs");
    }
    
    protected WfsXmlReader(String element, Configuration configuration, Catalog catalog, String serviceId) {
        super(new QName(WFS.NAMESPACE, element), new Version("1.0.0"), serviceId);
        this.configuration = configuration;
        this.catalog = catalog;
    }

    public Object read(Object request, Reader reader, Map kvp) throws Exception {
        //check the strict flag to determine if we should validate or not
        Boolean strict = (Boolean) kvp.get("strict");
        if ( strict == null ) {
            strict = Boolean.FALSE;
        }
        
        //create the parser instance
        Parser parser = new Parser(configuration);
        
        //"inject" namespace mappings
        List<NamespaceInfo> namespaces = catalog.getNamespaces();
        for ( NamespaceInfo ns : namespaces ) {
            //if ( namespaces[i].isDefault() ) 
            //    continue;
            
            parser.getNamespaces().declarePrefix( 
                ns.getPrefix(), ns.getURI());
        }
        //set validation based on strict or not
        parser.setValidating(strict.booleanValue());
        
        //parse
        Object parsed = parser.parse(reader); 
        
        //if strict was set, check for validation errors and throw an exception 
        if (strict.booleanValue() && !parser.getValidationErrors().isEmpty()) {
            WFSException exception = new WFSException("Invalid request", "InvalidParameterValue");

            for (Iterator e = parser.getValidationErrors().iterator(); e.hasNext();) {
                Exception error = (Exception) e.next();
                exception.getExceptionText().add(error.getLocalizedMessage());
            }

            throw exception;
        }
        
        return parsed;
    }
}
