/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;

import net.opengis.wfs.BaseRequestType;
import net.opengis.wfs.FeatureCollectionType;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.xml.v1_1_0.WFSConfiguration;
import org.geotools.gml3.v3_2.GML;
import org.geotools.wfs.v2_0.WFS;
import org.geotools.xml.Configuration;
import org.geotools.xml.Encoder;
import org.w3c.dom.Document;

public class GML32OutputFormat extends GML3OutputFormat {

    GeoServer geoServer;

    protected static DOMSource xslt;

    static {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware(true);
        Document xsdDocument = null;
        try {
            xsdDocument = docFactory.newDocumentBuilder().parse(
                    GML3OutputFormat.class.getResourceAsStream("/ChangeNumberOfFeature32.xslt"));
            xslt = new DOMSource(xsdDocument);
        } catch (Exception e) {
            xslt = null;
            LOGGER.log(Level.INFO, e.getMessage(), e);
        }
    }

    public GML32OutputFormat(GeoServer geoServer, WFSConfiguration configuration) {
        super(new HashSet(Arrays.asList("gml32", "text/xml; subtype=gml/3.2")), 
            geoServer, configuration);
        this.geoServer = geoServer;
    }

    @Override
    public String getMimeType(Object value, Operation operation) {
        return "text/xml; subtype=gml/3.2";
    }

    @Override
    protected Encoder createEncoder(Configuration configuration, 
        Map<String, Set<FeatureTypeInfo>> featureTypes, BaseRequestType request) {
        
        FeatureTypeSchemaBuilder schemaBuilder = new FeatureTypeSchemaBuilder.GML32(geoServer);
        
        ApplicationSchemaXSD2 xsd = new ApplicationSchemaXSD2(schemaBuilder, featureTypes);
        xsd.setBaseURL(request.getBaseUrl());
        
        ApplicationSchemaConfiguration2 config = new ApplicationSchemaConfiguration2(xsd, 
            new org.geotools.wfs.v2_0.WFSConfiguration());
        
        return new Encoder(config);
    }

    @Override
    protected void setAdditionalSchemaLocations(Encoder encoder, BaseRequestType request, WFSInfo wfs) {
        //since wfs 2.0 schema does not depend on gml 3.2 schema we register it manually
        String loc = wfs.isCanonicalSchemaLocation() ? GML.CANONICAL_SCHEMA_LOCATION : 
            ResponseUtils.buildSchemaURL(request.getBaseUrl(), "gml/3.2.1/gml.xsd");
        encoder.setSchemaLocation(GML.NAMESPACE, loc);
    }

    @Override
    protected void encode(FeatureCollectionType results, OutputStream output, Encoder encoder)
            throws IOException {
        // encoder.getNamespaces().declarePrefix("gml", GML.NAMESPACE);
        encoder.encode(results, WFS.FeatureCollection, output);
    }
    
    @Override
    protected String getWfsNamespace() {
        return WFS.NAMESPACE;
    }
    
    @Override
    protected String getCanonicalWfsSchemaLocation() {
        return WFS.CANONICAL_SCHEMA_LOCATION;
    }
    
    @Override
    protected String getRelativeWfsSchemaLocation() {
        return "wfs/2.0/wfs.xsd";
    }

    @Override
    protected DOMSource getXSLT() {
        return GML32OutputFormat.xslt;
    }

}
