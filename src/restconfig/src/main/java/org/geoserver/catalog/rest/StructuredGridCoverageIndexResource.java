/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.rest;

import java.util.List;

import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.impl.AttributeTypeInfoImpl;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.opengis.feature.simple.SimpleFeatureType;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * The index of a structured grid coverage reader, contains a feature type and a link to the
 * granules
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class StructuredGridCoverageIndexResource extends CatalogResourceBase {

    private CoverageInfo coverage;

    public StructuredGridCoverageIndexResource(Context context, Request request, Response response,
            Catalog catalog, CoverageInfo coverage) {
        super(context, request, response, IndexSchema.class, catalog);
        this.coverage = coverage;
    }

    @Override
    protected Object handleObjectGet() throws Exception {
        String nativeCoverageName = coverage.getNativeCoverageName();
        StructuredGridCoverage2DReader reader = (StructuredGridCoverage2DReader) coverage.getGridCoverageReader(null, null);
        if(nativeCoverageName == null) {
            if(reader.getGridCoverageNames().length > 1) {
                throw new IllegalStateException("The grid coverage configuration for " + coverage.getName() 
                        + " does not specify a native coverage name, yet the reader provides more than one coverage. " +
                        "Please assign a native coverage name (the GUI does so automatically)");
            } else {
                nativeCoverageName = reader.getGridCoverageNames()[0];
            }
        }
        
        GranuleSource source = reader.getGranules(nativeCoverageName, true);
        SimpleFeatureType schema = source.getSchema();
        List<AttributeTypeInfo> attributes = new CatalogBuilder(catalog).getAttributes(schema, null);
        
        return new IndexSchema(attributes);
    }
    
    @Override
    protected void configureXStream(XStream xstream) {
        super.configureXStream(xstream);
        xstream.alias("Schema", IndexSchema.class);
        xstream.alias("Attribute", AttributeTypeInfoImpl.class);
        xstream.omitField( AttributeTypeInfoImpl.class, "featureType");
        xstream.omitField( AttributeTypeInfoImpl.class, "metadata");
        xstream.registerConverter(new IndexSchemaConverter(xstream));
    }
    
    class IndexSchemaConverter extends ReflectionConverter {

        public IndexSchemaConverter(XStream xstream) {
            super(xstream.getMapper(), xstream.getReflectionProvider());
        }
        
        @Override
        public boolean canConvert(Class type) {
            return type.equals(IndexSchema.class);
        }
        
        @Override
        public void marshal(Object original, HierarchicalStreamWriter writer,
                MarshallingContext context) {
            super.marshal(original, writer, context);
            encodeLink("granules", writer);
        }
    }

    /**
     * Just holds a list of attributes
     * 
     * @author Andrea Aime - GeoSolutions
     */
    static class IndexSchema {
        List<AttributeTypeInfo> attributes;
        
        public IndexSchema(List<AttributeTypeInfo> attributes) {
            this.attributes = attributes;
        }
    }

}
