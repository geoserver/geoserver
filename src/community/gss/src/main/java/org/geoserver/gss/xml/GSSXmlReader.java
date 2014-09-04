/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.xml;

import java.io.Reader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.gss.GetDiffResponseType;
import org.geoserver.gss.PostDiffType;
import org.geoserver.ows.XmlRequestReader;
import org.geoserver.wfs.WFSException;
import org.geotools.util.Version;
import org.geotools.xml.Configuration;
import org.geotools.xml.Parser;
import org.xml.sax.InputSource;

/**
 * Xml reader for GSS 1.1.0 xml requests.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 * @author Andrea Aime - OpenGeo
 *
 * TODO: there is too much duplication with the WFS readers, factor it out.
 */
public class GSSXmlReader extends XmlRequestReader {
    /**
     * Xml Configuration
     */
    Configuration configuration;
    
    /**
     * Access to the global configuration
     */
    GeoServer geoServer;

    public GSSXmlReader(String element, GeoServer gs, Configuration configuration) {
        super(new QName(GSS.NAMESPACE, element), new Version("1.0.0"), "gss");
            this.configuration = configuration;
            this.geoServer = gs;
    }
    
    public Object read(Object request, Reader reader, Map kvp) throws Exception {
        Parser parser = new Parser(configuration);
        parser.setValidating(true);
        
        //"inject" namespace mappings
        Catalog catalog = geoServer.getCatalog();
        List<NamespaceInfo> namespaces = catalog.getNamespaces();
        for ( NamespaceInfo ns : namespaces ) {
            if ( ns.equals( catalog.getDefaultNamespace() ) )  
                continue;
            
            parser.getNamespaces().declarePrefix( 
                ns.getPrefix(), ns.getURI());
        }
       
        //set the input source with the correct encoding
        InputSource source = new InputSource(reader);
        source.setEncoding(geoServer.getGlobal().getCharset());

        Object parsed = parser.parse(source);

        // unfortunately insert elements in transactions cannot be validated...
        if(!(parsed instanceof PostDiffType) && !(parsed instanceof GetDiffResponseType)) {
            if (!parser.getValidationErrors().isEmpty()) {
                WFSException exception = new WFSException("Invalid request", "InvalidParameterValue");
    
                for (Iterator e = parser.getValidationErrors().iterator(); e.hasNext();) {
                    Exception error = (Exception) e.next();
                    exception.getExceptionText().add(error.getLocalizedMessage());
                }
    
                throw exception;
            }
        }

        return parsed;
    }
}
