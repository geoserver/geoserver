/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.internal;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.csw.records.GenericRecordBuilder;
import org.geoserver.csw.records.RecordBuilder;
import org.geoserver.csw.records.RecordDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.geotools.data.complex.filter.XPathUtil;
import org.geotools.filter.SortByImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;

/**
 * Internal Catalog Store Feature Iterator
 * 
 * @author Niels Charlier
 */
class CatalogStoreFeatureIterator implements Iterator<Feature> {

    static final Logger LOGGER = Logging.getLogger(CatalogStoreFeatureIterator.class);
    
    protected RecordBuilder builder;

    protected Iterator<ResourceInfo> layerIt;
    
    protected CatalogStoreMapping mapping;
    
    public CatalogStoreFeatureIterator(int offset, int count, SortBy[] sortOrder, Filter filter, Catalog catalog, CatalogStoreMapping mapping, RecordDescriptor recordDescriptor) {
        CatalogFacade catalogFacade = catalog.getFacade();
        layerIt = catalogFacade.list(ResourceInfo.class, filter, offset, count, sortOrder);
        this.mapping = mapping;
        builder = new GenericRecordBuilder(recordDescriptor);
    }

    @Override
    public boolean hasNext() {
        return layerIt.hasNext();
    }

    @Override
    public Feature next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more records to retrieve");
        }

        return convertToFeature(layerIt.next());
    }
    
    private Feature convertToFeature(ResourceInfo resource) {
        String id = null;
        
        for (CatalogStoreMapping.CatalogStoreMappingElement mappingElement: mapping.elements()) {
        	Object value = mappingElement.getContent().evaluate(resource);

            if (value != null) {
                if (value instanceof Collection) {
                    if (((Collection)value).size() > 0) {
                        String[] elements = new String[((Collection) value).size()];
                        int i = 0;
                        for (Object element : (Collection) value) {
                            elements[i++] = element.toString();
                        }
                        builder.addElement(mappingElement.getKey(), elements);
                    }
                } else {
                    builder.addElement(mappingElement.getKey(), value.toString());
                }

                if (mappingElement == mapping.getIdentifierElement()) {
                    id = value.toString();
                }
            }
        }
        // move on to the bounding boxes
      
        if (mapping.isIncludeEnvelope()) {
            ReferencedEnvelope bbox = null;
            try {
            	bbox = resource.boundingBox();
            } catch (Exception e) {
            	LOGGER.log(Level.INFO, "Failed to parse original record bbox");
            }
            if (bbox != null) {
                builder.addBoundingBox(bbox);
            }          
        }

        return builder.build(id);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("This iterator is read only");
    }

}
