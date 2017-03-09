/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import java.awt.RenderingHints.Key;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;

import org.geotools.data.DataAccess;
import org.geotools.data.DefaultResourceInfo;
import org.geotools.data.FeatureListener;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.ResourceInfo;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.AttributeBuilder;
import org.geotools.feature.ComplexFeatureBuilder;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Attribute;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

public abstract class AbstractMappingSource implements FeatureSource<FeatureType, Feature> {

    static final Logger LOGGER = Logging.getLogger(AbstractMappingSource.class);

    protected static FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    protected JDBCOpenSearchAccess openSearchAccess;

    protected FeatureType schema;

    protected SourcePropertyMapper propertyMapper;

    protected SortBy[] defaultSort;

    public AbstractMappingSource(JDBCOpenSearchAccess openSearchAccess,
            FeatureType collectionFeatureType) throws IOException {
        this.openSearchAccess = openSearchAccess;
        this.schema = collectionFeatureType;
        this.propertyMapper = new SourcePropertyMapper(schema);
        this.defaultSort = buildDefaultSort(schema);
    }

    /**
     * Builds the default sort for the underlying feature source query
     * 
     * @param schema
     * @return
     */
    protected SortBy[] buildDefaultSort(FeatureType schema) {
        // workaround for "name" not being quoted in the sql schema... might want to fix it,
        // but at the same time we'll eventually need some mapping going to Oracle or some such
        String timeStart = propertyMapper.getSourceName("timeStart");
        String name = propertyMapper.getSourceName("name");
        return new SortBy[] { FF.sort(timeStart, SortOrder.DESCENDING),
                FF.sort(name, SortOrder.ASCENDING) };
    }

    @Override
    public Name getName() {
        return schema.getName();
    }

    @Override
    public ResourceInfo getInfo() {
        try {
            SimpleFeatureSource featureSource = getDelegateCollectionSource();
            ResourceInfo delegateInfo = featureSource.getInfo();
            DefaultResourceInfo result = new DefaultResourceInfo(delegateInfo);
            result.setSchema(new URI(schema.getName().getNamespaceURI()));
            return result;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /*+
     * Returns the underlying delegate source
     */
    protected abstract SimpleFeatureSource getDelegateCollectionSource() throws IOException;

    @Override
    public DataAccess<FeatureType, Feature> getDataStore() {
        return openSearchAccess;
    }

    @Override
    public QueryCapabilities getQueryCapabilities() {
        QueryCapabilities result = new QueryCapabilities() {
            @Override
            public boolean isOffsetSupported() {
                return true;
            }

            @Override
            public boolean isReliableFIDSupported() {
                // the delegate store should have a primary key on collections
                return true;
            }
        };
        return result;
    }

    @Override
    public void addFeatureListener(FeatureListener listener) {
        throw new UnsupportedOperationException();

    }

    @Override
    public void removeFeatureListener(FeatureListener listener) {
        throw new UnsupportedOperationException();

    }

    @Override
    public FeatureCollection<FeatureType, Feature> getFeatures(Filter filter) throws IOException {
        return getFeatures(new Query(getSchema().getName().getLocalPart(), filter));
    }

    @Override
    public FeatureCollection<FeatureType, Feature> getFeatures() throws IOException {
        return getFeatures(Query.ALL);
    }

    @Override
    public FeatureType getSchema() {
        return schema;
    }

    @Override
    public ReferencedEnvelope getBounds() throws IOException {
        return getDelegateCollectionSource().getBounds();
    }

    @Override
    public ReferencedEnvelope getBounds(Query query) throws IOException {
        Query mapped = mapToSimpleCollectionQuery(query);
        return getDelegateCollectionSource().getBounds(mapped);
    }

    @Override
    public Set<Key> getSupportedHints() {
        try {
            return getDelegateCollectionSource().getSupportedHints();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public int getCount(Query query) throws IOException {
        // TODO: check if the query hits linked tables, and in that case, run getFeatures(), scan
        // and count
        return getDelegateCollectionSource().getCount(mapToSimpleCollectionQuery(query));
    }

    /**
     * Maps query back the main underlying feature source 
     * @param query
     * @return
     * @throws IOException 
     */
    protected Query mapToSimpleCollectionQuery(Query query) throws IOException {
        Query result = new Query(getDelegateCollectionSource().getSchema().getTypeName());
        if (query.getFilter() != null) {
            MappingFilterVisitor visitor = new MappingFilterVisitor(propertyMapper);
            Filter mappedFilter = (Filter) query.getFilter().accept(visitor, null);
            result.setFilter(mappedFilter);
        }
        if (query.getPropertyNames() != null && query.getPropertyNames().length > 0) {
            String[] mappedPropertyNames = Arrays.stream(query.getPropertyNames())
                    .map(name -> propertyMapper.getSourceName(name)).filter(name -> name != null)
                    .toArray(size -> new String[size]);
            result.setPropertyNames(mappedPropertyNames);
        }
        if (query.getSortBy() != null && query.getSortBy().length > 0) {
            SortBy[] mappedSortBy = Arrays.stream(query.getSortBy()).map(sb -> {
                if (sb == SortBy.NATURAL_ORDER || sb == SortBy.REVERSE_ORDER) {
                    return sb;
                } else {
                    String name = sb.getPropertyName().getPropertyName();
                    String mappedName = propertyMapper.getSourceName(name);
                    if (mappedName == null) {
                        throw new IllegalArgumentException("Cannot sort on " + name);
                    }
                    return FF.sort(mappedName, sb.getSortOrder());
                }
            }).toArray(size -> new SortBy[size]);
            result.setSortBy(mappedSortBy);
        } else {
            // get stable results for paging
            result.setSortBy(defaultSort);
        }
        result.setStartIndex(query.getStartIndex());
        result.setMaxFeatures(query.getMaxFeatures());

        return result;
    }

    @Override
    public FeatureCollection<FeatureType, Feature> getFeatures(Query query) throws IOException {
        // TODO: check if the query hits the OGC links and do post filtering on it
        Query mappedQuery = mapToSimpleCollectionQuery(query);
        SimpleFeatureCollection fc = getDelegateCollectionSource().getFeatures(mappedQuery);
        LOGGER.severe(
                "Still need to write the code that actually performs the joins and maps the result into a complex feature");
        return new MappingFeatureCollection(schema, fc, this::mapToComplexFeature);

    }

    /**
     * Maps the underlying features (eventually joined) to the output complex feature
     * @param it
     * @return
     */
    protected Feature mapToComplexFeature(PushbackFeatureIterator<SimpleFeature> it) {
        SimpleFeature fi = it.next();

        ComplexFeatureBuilder builder = new ComplexFeatureBuilder(schema);
        AttributeBuilder ab = new AttributeBuilder(CommonFactoryFinder.getFeatureFactory(null));
        for (PropertyDescriptor pd : schema.getDescriptors()) {
            if (!(pd instanceof AttributeDescriptor)) {
                continue;
            }
            String localName = (String) pd.getUserData().get(JDBCOpenSearchAccess.SOURCE_ATTRIBUTE);
            if(localName == null) {
                continue;
            }
            Object value = fi.getAttribute(localName);
            if(value == null) {
                continue;
            }
            ab.setDescriptor((AttributeDescriptor) pd);
            Attribute attribute = ab.buildSimple(null, value);
            builder.append(pd.getName(), attribute);
        }
        //
        Feature feature = builder.buildFeature(fi.getID());
        return feature;
    }

}
