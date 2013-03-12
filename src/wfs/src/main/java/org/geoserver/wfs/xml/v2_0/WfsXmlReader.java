/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v2_0;

import java.io.Reader;
import java.util.Map;

import javax.xml.namespace.QName;

import org.geoserver.catalog.NoExternalEntityResolver;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.XmlRequestReader;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.xml.FeatureTypeSchemaBuilder;
import org.geoserver.wfs.xml.WFSURIHandler;
import org.geoserver.wfs.xml.WFSXmlUtils;
import org.geotools.util.Version;
import org.geotools.wfs.v2_0.WFS;
import org.geotools.wfs.v2_0.WFSConfiguration;
import org.geotools.xml.Parser;
import org.xml.sax.EntityResolver;

/**
 * Xml reader for wfs 2.0 xml requests.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class WfsXmlReader extends XmlRequestReader {

    GeoServer gs;
    
    public WfsXmlReader(String element, GeoServer gs) {
        super(new QName(WFS.NAMESPACE, element), new Version("2.0.0"), "wfs");
        this.gs = gs;
    }
    
    /**
     * Creates an XML Entity Resolver.
     */
    protected EntityResolver getEntityResolver() {
        Boolean externalEntitiesEnabled = gs.getGlobal().getXmlExternalEntitiesEnabled();
        if (externalEntitiesEnabled != null && externalEntitiesEnabled.booleanValue()) {
            // XML parser will try to resolve entities
            return null;
        } else {
            // default behaviour: entities disabled
            return new NoExternalEntityResolver();
        }
    }    
    
    @Override
    public Object read(Object request, Reader reader, Map kvp) throws Exception {
        WFSConfiguration config = new WFSConfiguration();
        WFSXmlUtils.initWfsConfiguration(config, gs, new FeatureTypeSchemaBuilder.GML32(gs));
        
        Parser parser = new Parser(config);
        parser.setEntityResolver(getEntityResolver());
        parser.getURIHandlers().add(0, new WFSURIHandler(gs));
        
        WFSInfo wfs = wfs();
        
        WFSXmlUtils.initRequestParser(parser, wfs, gs, kvp);
        Object parsed = null;
        try {
            parsed = WFSXmlUtils.parseRequest(parser, reader, wfs);    
        }
        catch(Exception e) {
            //check the exception, and set code to OperationParsingFailed if code not set
            if (!(e instanceof ServiceException) || ((ServiceException)e).getCode() == null) {
                e = new WFSException("Request parsing failed", e, "OperationParsingFailed");
            }
            throw e;
        }

        WFSXmlUtils.checkValidationErrors(parser, this);
        
        return parsed;
    }

    WFSInfo wfs() {
        return gs.getService(WFSInfo.class);
    }
}
