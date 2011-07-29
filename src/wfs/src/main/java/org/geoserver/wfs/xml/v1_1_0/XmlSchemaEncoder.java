/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_1_0;

import java.io.IOException;
import java.io.OutputStream;

import net.opengis.wfs.DescribeFeatureTypeType;

import org.eclipse.xsd.XSDSchema;
import org.eclipse.xsd.util.XSDResourceImpl;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSDescribeFeatureTypeOutputFormat;
import org.geoserver.wfs.xml.FeatureTypeSchemaBuilder;
import org.geotools.xml.Schemas;


public class XmlSchemaEncoder extends WFSDescribeFeatureTypeOutputFormat {
    
    /** the catalog */
    Catalog catalog;

    /** the geoserver resource loader */
    GeoServerResourceLoader resourceLoader;
    
    /** schema builder */
    FeatureTypeSchemaBuilder schemaBuilder;

    public XmlSchemaEncoder(String mimeType, GeoServer gs, FeatureTypeSchemaBuilder schemaBuilder) {
        super(gs, mimeType);
        
       
        this.catalog = gs.getCatalog();
        this.resourceLoader = catalog.getResourceLoader();
        this.schemaBuilder = schemaBuilder;
    }

    public String getMimeType(Object value, Operation operation)
        throws ServiceException {
        return getOutputFormat();
        //return "text/xml; subtype=gml/3.1.1";
    }

    protected void write(FeatureTypeInfo[] featureTypeInfos, OutputStream output,
        Operation describeFeatureType) throws IOException {
        
        GeoServerInfo global = gs.getGlobal();
        //create the schema
        DescribeFeatureTypeType req = (DescribeFeatureTypeType)describeFeatureType.getParameters()[0];
        XSDSchema schema = schemaBuilder.build(featureTypeInfos, req.getBaseUrl());
    
        //serialize
        schema.updateElement();
        final String encoding = global.getCharset();
        XSDResourceImpl.serialize(output, schema.getElement(), encoding);
    }
    
    public static class V20 extends XmlSchemaEncoder {

        public V20(GeoServer gs) {
            super("text/xml; subtype=gml/3.2", gs, new FeatureTypeSchemaBuilder.GML32(gs));
        }
        
    }
    
    public static class V11 extends XmlSchemaEncoder {

        public V11(GeoServer gs) {
            super("text/xml; subtype=gml/3.1.1",gs,new FeatureTypeSchemaBuilder.GML3(gs));
        }
        
    }
    
    public static class V10 extends XmlSchemaEncoder {

        public V10(GeoServer gs) {
            super("XMLSCHEMA", gs, new FeatureTypeSchemaBuilder.GML2(gs));
        }
        
        @Override
        public String getMimeType(Object arg0, Operation arg1) throws ServiceException {
            return "text/xml";
        }
        
    }
}
