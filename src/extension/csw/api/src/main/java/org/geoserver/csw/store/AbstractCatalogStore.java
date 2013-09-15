/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.opengis.cat.csw20.ElementSetType;

import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.csw.records.AbstractRecordDescriptor;
import org.geoserver.csw.records.CSWRecordDescriptor;
import org.geoserver.csw.records.RecordDescriptor;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.Property;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.identity.FeatureId;

/**
 * 
 * @author Andrea Aime - GeoSolutions
 * @author Niels Charlier
 *
 */
public abstract class AbstractCatalogStore implements CatalogStore {

    protected Map<String, RecordDescriptor> descriptorByType = new HashMap<String, RecordDescriptor>();
    protected Map<String, RecordDescriptor> descriptorByOutputSchema = new HashMap<String, RecordDescriptor>();

    protected void support(RecordDescriptor descriptor) {
        descriptorByType.put(descriptor.getFeatureDescriptor().getName().getLocalPart(), descriptor);
        descriptorByOutputSchema.put(descriptor.getOutputSchema(), descriptor);
    }

    @Override
    public RecordDescriptor[] getRecordDescriptors() throws IOException {
        ArrayList<RecordDescriptor> ft = new ArrayList<RecordDescriptor>(descriptorByType.values());
        return ft.toArray(new RecordDescriptor[ft.size()]);
    }

    @Override
    public CloseableIterator<String> getDomain(Name typeName, final Name attributeName) throws IOException {
		
	final RecordDescriptor rd = descriptorByType.get(typeName.getLocalPart());
	        
        if (rd==null) {
            throw new IOException(typeName.getLocalPart() + " is not a supported type");
        }
		
        // do we have such attribute?
        final PropertyName property = rd.translateProperty(attributeName);
        AttributeDescriptor ad = (AttributeDescriptor) property.evaluate(rd.getFeatureType());
        if(ad == null) {
            return new CloseableIteratorAdapter<String>(new ArrayList<String>().iterator());
        }

        // build the query against csw:record
        Query q = new Query(typeName.getLocalPart());
        
        q.setProperties(Arrays.asList(translateProperty(rd, attributeName)));
        
        // collect the values without duplicates
        final Set<String> values = new HashSet<String>();
        getRecords(q, Transaction.AUTO_COMMIT, rd.getOutputSchema()).accepts(new FeatureVisitor() {
            
            @Override
            public void visit(Feature feature) {
                Property prop = (Property) property.evaluate(feature);
                if (prop != null)
                    try {
                        values.add( new String(((String) prop.getValue()).getBytes("ISO-8859-1"), "UTF-8" ) );
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
            }
        }, null);
        
        // sort and return
        List<String> result = new ArrayList(values);
        Collections.sort(result);
        return new CloseableIteratorAdapter<String>(result.iterator());
    }
    
    @Override
    public FeatureCollection getRecords(Query q, Transaction t, String outputSchema) throws IOException {
        RecordDescriptor rd;
        if (q.getTypeName() == null) {
            rd = descriptorByType.get("Record");
        } else {
            rd = descriptorByType.get(q.getTypeName());
        }
        
        RecordDescriptor rdOutput;
        if (outputSchema == null || "".equals(outputSchema)) {
        	rdOutput = descriptorByOutputSchema.get(CSWRecordDescriptor.getInstance().getOutputSchema());
        } else {
        	rdOutput = descriptorByOutputSchema.get(outputSchema);
        }
                
        if (rd==null) {
            throw new IOException(q.getTypeName() + " is not a supported type");
        }
        
        if (rdOutput==null) {
            throw new IOException(outputSchema + " is not a supported output schema");
        }
        
        return getRecordsInternal(rd, rdOutput, q, t);
    }
    
    public abstract FeatureCollection getRecordsInternal(RecordDescriptor rd, RecordDescriptor rdOutput, Query q, Transaction t) throws IOException;
    
    @Override
    public RepositoryItem getRepositoryItem(String recordId) throws IOException {
        // not supported
        return null;
    }

    @Override
    public int getRecordsCount(Query q, Transaction t) throws IOException {
        // simply delegate to the feature collection, we have no optimizations 
        // available for the time being (even counting the files in case of no filtering
        // would be wrong as we have to 
        return getRecords(q, t, null).size();
    }

    @Override
    public List<FeatureId> addRecord(Feature f, Transaction t) throws IOException {
        throw new UnsupportedOperationException("This store does not support transactions yet");
    }

    @Override
    public void deleteRecord(Filter f, Transaction t) throws IOException {
        throw new UnsupportedOperationException("This store does not support transactions yet");
    }

    @Override
    public void updateRecord(Name typeName, Name[] attributeNames, Object[] attributeValues,
            Filter filter, Transaction t) throws IOException {
        throw new UnsupportedOperationException("This store does not support transactions yet");
    }
    
    @Override
    public CatalogStoreCapabilities getCapabilities() {
        return new CatalogStoreCapabilities(descriptorByType) ;
    }
    
    @Override
    public PropertyName translateProperty(RecordDescriptor rd, Name name) {
        return AbstractRecordDescriptor.buildPropertyName(rd.getNamespaceSupport(), name);
    }


}
