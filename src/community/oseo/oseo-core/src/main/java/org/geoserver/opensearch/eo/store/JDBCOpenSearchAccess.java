/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
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
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
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

    public static final String COLLECTION = "collection";

    static final String PRODUCT = "product";

    static final String EO_PREFIX = "eo";

    static final String SAR_PREFIX = "sar";

    static final String SOURCE_ATTRIBUTE = "sourceAttribute";

    Repository repository;

    Name delegateStoreName;

    String namespaceURI;

    FeatureType collectionFeatureType;

    FeatureType productFeatureType;

    List<Name> typeNames;

    public JDBCOpenSearchAccess(Repository repository, Name delegateStoreName, String namespaceURI)
            throws IOException {
        // TODO: maybe get a direct Catalog reference so that we can lookup by store id, which is
        // stable though renames?
        this.repository = repository;
        this.delegateStoreName = delegateStoreName;
        this.namespaceURI = namespaceURI;

        // check the expected feature types are available
        DataStore delegate = getDelegateStore();
        List<String> missingTables = getMissingRequiredTables(delegate, COLLECTION, PRODUCT);
        if (!missingTables.isEmpty()) {
            throw new IOException("Missing required tables in the backing store " + missingTables);
        }

        collectionFeatureType = buildCollectionFeatureType(delegate);
        productFeatureType = buildProductFeatureType(delegate);
    }

    private FeatureType buildCollectionFeatureType(DataStore delegate) throws IOException {
        SimpleFeatureType flatSchema = delegate.getSchema(COLLECTION);

        TypeBuilder collectionTypeBuilder = new TypeBuilder(
                CommonFactoryFinder.getFeatureTypeFactory(null));

        // map the source attributes
        AttributeTypeBuilder ab = new AttributeTypeBuilder();
        for (AttributeDescriptor ad : flatSchema.getAttributeDescriptors()) {
            String name = ad.getLocalName();
            String namespaceURI = this.namespaceURI;
            if (name.startsWith(EO_PREFIX)) {
                name = name.substring(EO_PREFIX.length());
                char c[] = name.toCharArray();
                c[0] = Character.toLowerCase(c[0]);
                name = new String(c);
                namespaceURI = EO_NAMESPACE;
            }
            // get a more predictable name structure (will have to do something for oracle
            // like names too I guess)
            if (StringUtils.isAllUpperCase(name)) {
                name = name.toLowerCase();
            }
            // map into output type
            ab.init(ad);
            ab.name(name).namespaceURI(namespaceURI).userData(SOURCE_ATTRIBUTE, ad.getLocalName());
            AttributeDescriptor mappedDescriptor;
            if (ad instanceof GeometryDescriptor) {
                GeometryType at = ab.buildGeometryType();
                ab.setCRS(((GeometryDescriptor) ad).getCoordinateReferenceSystem());
                mappedDescriptor = ab.buildDescriptor(new NameImpl(namespaceURI, name), at);
            } else {
                AttributeType at = ab.buildType();
                mappedDescriptor = ab.buildDescriptor(new NameImpl(namespaceURI, name), at);
            }

            collectionTypeBuilder.add(mappedDescriptor);
        }

        // TODO: map OGC links and extra attributes

        collectionTypeBuilder.setName(COLLECTION);
        collectionTypeBuilder.setNamespaceURI(namespaceURI);
        return collectionTypeBuilder.feature();
    }

    private FeatureType buildProductFeatureType(DataStore delegate) throws IOException {
        SimpleFeatureType flatSchema = delegate.getSchema(PRODUCT);

        TypeBuilder collectionTypeBuilder = new TypeBuilder(
                CommonFactoryFinder.getFeatureTypeFactory(null));

        // map the source attributes
        AttributeTypeBuilder ab = new AttributeTypeBuilder();
        for (AttributeDescriptor ad : flatSchema.getAttributeDescriptors()) {
            String name = ad.getLocalName();
            String namespaceURI = this.namespaceURI;
            for (ProductClass pc : ProductClass.values()) {
                String prefix = pc.getPrefix();
                if (name.startsWith(prefix)) {
                    name = name.substring(prefix.length());
                    char c[] = name.toCharArray();
                    c[0] = Character.toLowerCase(c[0]);
                    name = new String(c);
                    namespaceURI = pc.getNamespace();
                    break;
                }
            }

            // get a more predictable name structure (will have to do something for oracle
            // like names too I guess)
            if (StringUtils.isAllUpperCase(name)) {
                name = name.toLowerCase();
            }
            // map into output type
            ab.init(ad);
            ab.name(name).namespaceURI(namespaceURI).userData(SOURCE_ATTRIBUTE, ad.getLocalName());
            AttributeDescriptor mappedDescriptor;
            if (ad instanceof GeometryDescriptor) {
                GeometryType at = ab.buildGeometryType();
                ab.setCRS(((GeometryDescriptor) ad).getCoordinateReferenceSystem());
                mappedDescriptor = ab.buildDescriptor(new NameImpl(namespaceURI, name), at);
            } else {
                AttributeType at = ab.buildType();
                mappedDescriptor = ab.buildDescriptor(new NameImpl(namespaceURI, name), at);
            }

            collectionTypeBuilder.add(mappedDescriptor);
        }

        // TODO: map OGC links and extra attributes

        collectionTypeBuilder.setName(PRODUCT);
        collectionTypeBuilder.setNamespaceURI(namespaceURI);
        return collectionTypeBuilder.feature();
    }

    private List<String> getMissingRequiredTables(DataStore delegate, String... tables)
            throws IOException {
        Set<String> availableNames = new HashSet<>(Arrays.asList(delegate.getTypeNames()));
        return Arrays.stream(tables).map(String::toLowerCase)
                .filter(table -> !availableNames.contains(table)).collect(Collectors.toList());
    }

    /**
     * Returns the store from the repository (which is based on GeoServer own resource pool)
     * 
     * @return
     * @throws IOException
     */
    DataStore getDelegateStore() throws IOException {
        DataStore store = repository.dataStore(delegateStoreName);
        return new LowercasingDataStore(store);

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
            if (name.equals(ft.getName())) {
                return ft;
            }
        }
        return null;
    }

    @Override
    public FeatureSource<FeatureType, Feature> getFeatureSource(Name typeName) throws IOException {
        if (collectionFeatureType.getName().equals(typeName)) {
            return getCollectionSource();
        } else if (productFeatureType.getName().equals(typeName)) {
            return getProductSource();
        }
        return null;

    }

    public FeatureSource<FeatureType, Feature> getProductSource() throws IOException {
        return new ProductFeatureSource(this, productFeatureType);
    }

    public FeatureSource<FeatureType, Feature> getCollectionSource() throws IOException {
        return new CollectionFeatureSource(this, collectionFeatureType);
    }

    @Override
    public void dispose() {
        // nothing to dispose, the delegate store is managed by the resource pool
    }

}
