/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.Repository;
import org.geotools.data.ServiceInfo;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.NameImpl;
import org.geotools.feature.TypeBuilder;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;

/**
 * A data store building OpenSearch for EO records based on a wrapped data store providing all expected tables in form of simple features (and
 * leveraging joins to put them together into complex features as needed).
 * 
 * The delegate store is fetched on demand to avoid being caught in a ResourcePool dispose
 *
 * @author Andrea Aime - GeoSolutions
 */
public class JDBCOpenSearchAccess implements OpenSearchAccess {

    static final String COLLECTION = "COLLECTION";

    static final String PRODUCT = "PRODUCT";

    static final String EO_PREFIX = "eo";

    static final String SOURCE_ATTRIBUTE = "sourceAttribute";

    Repository repository;

    Name delegateStoreName;
    
    String namespaceURI;

    FeatureType collectionFeatureType;

    FeatureType productFeatureType;
    
    List<Name> typeNames;

    public JDBCOpenSearchAccess(Repository repository, Name delegateStoreName, String namespaceURI) throws IOException {
        // TODO: maybe get a direct Catalog reference so that we can lookup by store id, which is
        // stable though renames?
        this.repository = repository;
        this.delegateStoreName = delegateStoreName;
        this.namespaceURI = namespaceURI;

        // TODO: check the expected feature types are available
        DataStore delegate = getDelegateStore();
        verifyRequiredTables(delegate, COLLECTION, PRODUCT);

        
        collectionFeatureType = buildCollectionFeatureType(delegate);
        // TODO: build the complex feature type for product here
        productFeatureType = delegate.getSchema(PRODUCT);
        
    }

    private FeatureType buildCollectionFeatureType(DataStore delegate) throws IOException {
        SimpleFeatureType flatSchema = delegate.getSchema(COLLECTION);

        TypeBuilder collectionTypeBuilder = new TypeBuilder(CommonFactoryFinder.getFeatureTypeFactory(null));
        
        // map the source attributes
        AttributeTypeBuilder ab = new AttributeTypeBuilder();
        for (AttributeDescriptor ad : flatSchema.getAttributeDescriptors()) {
            String name = ad.getLocalName();
            String namespaceURI = delegateStoreName.getNamespaceURI();
            if (name.startsWith(EO_PREFIX)) {
                name = name.substring(EO_PREFIX.length());
                char c[] = name.toCharArray();
                c[0] = Character.toLowerCase(c[0]);
                name = new String(c);
                namespaceURI = EO_NAMESPACE;
            }
            // map into output type
            ab.init(ad);
            ab.name(name).namespaceURI(namespaceURI).userData(SOURCE_ATTRIBUTE, ad.getLocalName());
            AttributeType at = ab.buildType();
            AttributeDescriptor mappedDescriptor = ab.buildDescriptor(new NameImpl(namespaceURI, name), at);
            collectionTypeBuilder.add(mappedDescriptor);
        }
        
        // TODO: map OGC links and extra attributes

        collectionTypeBuilder.setName(COLLECTION);
        collectionTypeBuilder.setNamespaceURI(namespaceURI);
        return collectionTypeBuilder.feature();
    }

    private void verifyRequiredTables(DataStore delegate, String... tables) throws IOException {
        Set<String> availableNames = new HashSet<>(Arrays.asList(delegate.getTypeNames()));
        List<String> missingTables = Arrays.stream(tables)
                .filter(table -> !availableNames.contains(table)).collect(Collectors.toList());
        if (!missingTables.isEmpty()) {
            throw new IllegalArgumentException(
                    "OpenSearch source data store does not contain some required tables: " + Arrays.asList(missingTables));
        }
    }

    /**
     * Returns the store from the repository (which is based on GeoServer own resource pool)
     * 
     * @return
     */
    DataStore getDelegateStore() {
        return repository.dataStore(delegateStoreName);
    }

    @Override
    public ServiceInfo getInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void createSchema(FeatureType featureType) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateSchema(Name typeName, FeatureType featureType) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeSchema(Name typeName) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Name> getNames() throws IOException {
        return Arrays.asList(collectionFeatureType.getName(), productFeatureType.getName());
    }

    @Override
    public FeatureType getSchema(Name name) throws IOException {
        for (FeatureType ft : Arrays.asList(collectionFeatureType, productFeatureType)) {
            if(name.equals(ft.getName())) {
                return ft;
            }
        }
        return null;
    }

    @Override
    public FeatureSource<FeatureType, Feature> getFeatureSource(Name typeName) throws IOException {
        if(typeName.getLocalPart().equals(COLLECTION)) {
            return new CollectionFeatureSource(this, collectionFeatureType);
        }
        return null;
        
    }

    @Override
    public void dispose() {
        // nothing to dispose, the delegate store is managed by the resource pool
    }

    @Override
    public Name getCollectionName() {
        return collectionFeatureType.getName();
    }

}
