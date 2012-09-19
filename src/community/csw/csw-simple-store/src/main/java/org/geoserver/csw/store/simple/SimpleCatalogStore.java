/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.simple;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.csw.feature.MemoryFeatureCollection;
import org.geoserver.csw.feature.sort.ComplexComparatorFactory;
import org.geoserver.csw.records.CSWRecordDescriptor;
import org.geoserver.csw.store.CatalogStore;
import org.geoserver.csw.store.CatalogStoreCapabilities;
import org.geoserver.csw.store.RepositoryItem;
import org.geoserver.csw.util.QNameResolver;
import org.geotools.csw.DC;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.store.FilteringFeatureCollection;
import org.geotools.data.store.MaxFeaturesFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.opengis.feature.ComplexAttribute;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;

/**
 * A simple implementation of {@link CatalogStore} geared towards test support. 
 * The store reads CSW records from xml files located in the root folder, it is not meant to 
 * be fast or scalable, on the contrary, to keep its implementation as simple as possible it
 * is actually slow and occasionally memory bound. 
 * <p>Do not use it for production purposes. 
 * 
 * @author Andrea Aime - GeoSolutions
 *
 */
public class SimpleCatalogStore implements CatalogStore {

    private File root;
    
    static final List<Name> QUERIABLES;
    
    static {
        List<String> queriables = Arrays.asList("dc:contributor", "dc:source", "dc:language", 
                "dc:title", "dc:subject", "dc:creator", "dc:type", "ows:BoundingBox", "dct:modified", 
                "dct:abstract", "dc:relation", "dc:date", "dc:identifier", "dc:publisher", 
                "dc:format", "csw:AnyText", "dc:rights");
        QUERIABLES = new ArrayList<Name>();
        QNameResolver resolver = new QNameResolver();
        for (String q : queriables) {
            QName qname = resolver.parseQName(q, CSWRecordDescriptor.NAMESPACES);
            QUERIABLES.add(new NameImpl(qname));
        }
    }

    public SimpleCatalogStore(File root) {
        this.root = root;

        if (!root.exists()) {
            throw new IllegalArgumentException("Record directory does not exists: "
                    + root.getPath());
        } else if (!root.isDirectory()) {
            throw new IllegalArgumentException(
                    "Got an existing reference on the file system, but it's not a directory: "
                            + root.getPath());
        }
    }

    @Override
    public FeatureType[] getRecordSchemas() throws IOException {
        // right now we only support CSW record, hopefully this will be extended later
        return new FeatureType[] { CSWRecordDescriptor.RECORD };
    }

    @Override
    public FeatureCollection getRecords(Query q, Transaction t) throws IOException {
        if (q.getTypeName() != null
                && !q.getTypeName().equals(CSWRecordDescriptor.RECORD.getName().getLocalPart())) {
            throw new IOException(q.getTypeName() + " is not a supported type");
        }
        if (q.getNamespace() != null
                && !q.getNamespace().toString()
                        .equals(CSWRecordDescriptor.RECORD.getName().getNamespaceURI())) {
            throw new IOException(q.getNamespace() + ":" + q.getTypeName()
                    + " is not a supported type");
        }

        int startIndex = 0;
        if (q.getStartIndex() != null) {
            startIndex = q.getStartIndex();
        }
        FeatureCollection records = new RecordsFeatureCollection(root, startIndex);

        // filtering
        if (q.getFilter() != null && q.getFilter() != Filter.INCLUDE) {
            Filter filter = q.getFilter();
            CSWAnyExpander expander = new CSWAnyExpander();
            Filter expanded = (Filter) filter.accept(expander, null);
            
            records = new FilteringFeatureCollection<FeatureType, Feature>(records, expanded);
        }

        // sorting
        if (q.getSortBy() != null && q.getSortBy().length > 0) {
            Feature[] features = (Feature[]) records.toArray(new Feature[records.size()]);
            Comparator<Feature> comparator = ComplexComparatorFactory.buildComparator(q.getSortBy());
            Arrays.sort(features, comparator);
            
            records = new MemoryFeatureCollection(records.getSchema(), Arrays.asList(features));
        }

        // max features
        if (q.getMaxFeatures() < Query.DEFAULT_MAX) {
            records = new MaxFeaturesFeatureCollection<FeatureType, Feature>(records,
                    q.getMaxFeatures());
        }
        
        // reducing attributes
        if(q.getProperties() != null && q.getProperties().size() > 0) {
            records = new RetypingFeatureCollection(records, q.getProperties());
        }

        return records;
    }

    @Override
    public CloseableIterator<String> getDomain(Name typeName, final Name attributeName) throws IOException {
        // do we have such attribute?
        AttributeDescriptor ad = CSWRecordDescriptor.getDescriptor(attributeName.getLocalPart());
        if(ad == null) {
            return new CloseableIteratorAdapter<String>(new ArrayList<String>().iterator());
        }

        // build the query against csw:record
        Query q = new Query(typeName.getLocalPart());
        FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();
        String xpath;
        if(ad.getName().getNamespaceURI().equals(DC.NAMESPACE)) {
            xpath = "dc:" + ad.getLocalName();
        } else {
            xpath = "dct:" + ad.getLocalName();
        }
        q.setProperties(Arrays.asList(FF.property(xpath, CSWRecordDescriptor.NAMESPACES)));
        
        // collect the values without duplicates
        FeatureIterator<Feature> fi = null;
        final Set<String> values = new HashSet<String>();
        getRecords(q, Transaction.AUTO_COMMIT).accepts(new FeatureVisitor() {
            
            @Override
            public void visit(Feature feature) {
                ComplexAttribute att = (ComplexAttribute) feature.getProperty(attributeName.getLocalPart());
                if (att != null && att.getProperty("value") != null)
                    try {
                        values.add( new String( ((String) att.getProperty("value").getValue()).getBytes("ISO-8859-1"), "UTF-8" ) );
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
//                values.add((String) att.getProperty("value").getValue());
            }
        }, null);
        
        // sort and return
        List<String> result = new ArrayList(values);
        Collections.sort(result);
        return new CloseableIteratorAdapter<String>(result.iterator());
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
        return new CatalogStoreCapabilities() {
          
            @Override
            public boolean supportsGetRepositoryItem(Name typeName) {
                return CSWRecordDescriptor.RECORD.getName().equals(typeName);
            }
            
            @Override
            public List<Name> getQueriables(Name typeName) {
                return QUERIABLES;
            }
            
            @Override
            public List<Name> getDomainQueriables(Name typeName) {
                return QUERIABLES;
            }
            
        };
    }

    @Override
    public int getRecordsCount(Query q, Transaction t) throws IOException {
        // simply delegate to the feature collection, we have no optimizations 
        // available for the time being (even counting the files in case of no filtering
        // would be wrong as we have to 
        return getRecords(q, t).size();
    }

    /**
     * This dummy implementation returns the file backing the record, verbatim
     */
    @Override
    public RepositoryItem getRepositoryItem(String recordId) {
        SimpleRecordIterator it = new SimpleRecordIterator(root, 0);
        while(it.hasNext()) {
            Feature f = it.next();
            if(recordId.equals(f.getIdentifier().getID())) {
                final File file = it.getLastFile();
                return new RepositoryItem() {
                    
                    @Override
                    public String getMime() {
                        return "application/xml";
                    }
                    
                    @Override
                    public InputStream getContents() throws IOException {
                        return new FileInputStream(file);
                    }
                };
            }
        }
        
        // not found
        return null;
    }

}
