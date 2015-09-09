/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
/**
 * FeatureCollection that remaps attribute names using a given map.
 * 
 * @author Mauro Bartolomeoli, mbarto@infosia.it
 * 
 */
public class RemappingFeatureCollection extends DecoratingSimpleFeatureCollection {

    Map<String,String> attributesMapping;
    
    public RemappingFeatureCollection(SimpleFeatureCollection delegate,Map<String,String> attributesMapping) {
        super(delegate);
        this.attributesMapping=attributesMapping;       
    }
    
    public SimpleFeatureType getSchema() {
        return remapSchema(delegate.getSchema());
    }
    
    /**
     * Builds an inverted version of the given map.
     * Inversion means that key->value becomes value->key
     * @param map
     * @return
     */
    static Map<String,String> invertMappings(Map<String,String> map) {
        Map<String,String> result=new HashMap<String,String>();
        for(String key:map.keySet())
            result.put(map.get(key),key);
        return result;
    }
    
    /**
     * Gets a new schema, built remapping attribute names via
     * the attributeMappings map. 
     * @param schema
     * @return
     */
    private SimpleFeatureType remapSchema(SimpleFeatureType schema) {
        SimpleFeatureTypeBuilder builder=new SimpleFeatureTypeBuilder();
        builder.setName(schema.getName());
        for(AttributeDescriptor attDesc : schema.getAttributeDescriptors()) {
            if(attDesc instanceof GeometryDescriptor) {
                GeometryDescriptor geoDesc=(GeometryDescriptor)attDesc;
                builder.add(attributesMapping.get(attDesc.getLocalName()),attDesc.getType().getBinding(),geoDesc.getCoordinateReferenceSystem());            
            } else {
                List<Filter> filters = attDesc.getType().getRestrictions();
                if (filters != null && !filters.isEmpty()) {
                    builder.restrictions(filters);
                }
                builder.add(attributesMapping.get(attDesc.getLocalName()),attDesc.getType().getBinding());
            }
        }
        return builder.buildFeatureType();
    }

    public SimpleFeatureIterator features() {
        return new RemappingIterator(delegate.features(), attributesMapping, getSchema());
    }

    /**
     * Remaps a SimpleFeature, using the given mappings (oldname -> mappedname).
     * The builder uses the mapped schema.
     * 
     * @param source
     * @param attributeMappings
     * @param builder
     * @return
     */
    static SimpleFeature remap(SimpleFeature source, Map<String,String> attributeMappings,SimpleFeatureBuilder builder) {
        SimpleFeatureType target = builder.getFeatureType();
        for (int i = 0; i < target.getAttributeCount(); i++) {
            AttributeDescriptor attributeType = target.getDescriptor(i);
            Object value = null;
            String mappedName=attributeMappings.get(attributeType.getLocalName());
            if (source.getFeatureType().getDescriptor(mappedName) != null) {
                value = source.getAttribute(mappedName);
            }

            builder.add(value);
        }
        
        return builder.buildFeature(source.getIdentifier().getID());
    }

    
    public static class RemappingIterator implements SimpleFeatureIterator {
        Map<String,String> attributesMapping;
        SimpleFeatureIterator delegate;
        SimpleFeatureBuilder builder;

        public RemappingIterator(SimpleFeatureIterator delegate, Map attributesMapping,SimpleFeatureType schema) {
            this.delegate = delegate;
            this.attributesMapping = RemappingFeatureCollection.invertMappings(attributesMapping);
            this.builder = new SimpleFeatureBuilder(schema);
        }

        public boolean hasNext() {
            return delegate.hasNext();
        }

        public SimpleFeature next() {
            return RemappingFeatureCollection.remap(delegate.next(), attributesMapping,builder);
        }

        @Override
        public void close() {
            delegate.close();
        }
    }

    


}
