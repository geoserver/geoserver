package org.geoserver.wfs.xml;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.opengis.wfs.BaseRequestType;
import net.opengis.wfs.FeatureCollectionType;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.xml.v1_1_0.WFSConfiguration;
import org.geotools.gml3.v3_2.GML;
import org.geotools.wfs.v2_0.WFS;
import org.geotools.xml.Configuration;
import org.geotools.xml.Encoder;

public class GML32OutputFormat extends GML3OutputFormat {

    GeoServer geoServer;
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
    protected void encode(FeatureCollectionType results, OutputStream output, Encoder encoder)
            throws IOException {
        //encoder.getNamespaces().declarePrefix("gml", GML.NAMESPACE);
        encoder.encode(results, WFS.FeatureCollection, output);
    }
}
