/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2012 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.csw.feature.AbstractFeatureCollection;
import org.geoserver.csw.feature.MemoryFeatureCollection;
import org.geoserver.csw.records.CSWRecordDescriptor;
import org.geoserver.csw.records.RecordDescriptor;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.filter.And;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.data.store.FilteringFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;

/**
 * Internal Catalog Store Feature Collection Creates a Catalog Store from a GeoServer Catalog
 * instance and a Mapping Will map data from GeoServer catalog to a particular CSW Record Type,
 * provided in Record Descriptor
 *
 * @author Niels Charlier
 */
class CatalogStoreFeatureCollection extends AbstractFeatureCollection<FeatureType, Feature> {

    private final class ResourceFilterVisitor extends DuplicatingFilterVisitor {
        @Override
        public Object visit(PropertyName expression, Object extraData) {
            return getFactory(extraData)
                    .property(
                            "resource." + expression.getPropertyName(),
                            expression.getNamespaceContext());
        }
    }

    protected static final FilterFactory ff = CommonFactoryFinder.getFilterFactory();

    protected int offset, count;
    protected SortBy[] sortOrder;
    protected Filter filter;
    protected Catalog catalog;
    protected CatalogStoreMapping mapping;
    protected RecordDescriptor rd;
    protected Map<String, String> interpolationProperties = new HashMap<>();

    /**
     * Create new CatalogStoreFeatureCollection
     *
     * @param offset Offset
     * @param count Count
     * @param sortOrder Sort Order
     * @param filter Filter
     * @param catalog The GeoServer Catalog
     * @param mapping The Mapping
     * @param rd Record Descriptor
     */
    public CatalogStoreFeatureCollection(
            int offset,
            int count,
            SortBy[] sortOrder,
            Filter filter,
            Catalog catalog,
            CatalogStoreMapping mapping,
            RecordDescriptor rd,
            Map<String, String> interpolationProperties) {
        super(CSWRecordDescriptor.RECORD_TYPE);
        this.offset = offset;
        this.count = count;
        this.filter = filter;
        this.catalog = catalog;
        this.mapping = mapping;
        this.sortOrder = sortOrder;
        this.interpolationProperties = interpolationProperties;
        this.rd = rd;
    }

    @Override
    protected Iterator<Feature> openIterator() {
        return new CatalogStoreFeatureIterator(
                offset,
                count,
                sortOrder,
                catalogFilter(),
                catalog,
                mapping,
                rd,
                interpolationProperties);
    }

    @Override
    protected void closeIterator(Iterator<Feature> close) {}

    @Override
    public FeatureCollection<FeatureType, Feature> subCollection(Filter filter) {
        return new FilteringFeatureCollection<>(this, filter);
    }

    @Override
    public FeatureCollection<FeatureType, Feature> sort(SortBy order) {
        List<Feature> features = new ArrayList<>();
        MemoryFeatureCollection memory = new MemoryFeatureCollection(getSchema(), features);
        return memory.sort(order);
    }

    private Filter catalogFilter() {
        Filter filter =
                Predicates.and(
                        // ignore catalog info's that are not enabled
                        Predicates.equal("enabled", true),
                        // ignore catalog info's that are not advertised
                        Predicates.equal("advertised", true),
                        // ignore catalog info's without id
                        ff.not(ff.isNull(this.mapping.getIdentifierElement().getContent())));
        filter = Predicates.and(this.filter, filter);
        // build filter compatible with layers
        List<Filter> filtersL = new ArrayList<>();
        filtersL.add(Predicates.isInstanceOf(LayerInfo.class));
        filtersL.addAll(((And) filter.accept(new ResourceFilterVisitor(), null)).getChildren());
        // ignore layer info's from stores that are not enabled
        filtersL.add(filtersL.size() - 2, Predicates.equal("resource.store.enabled", true));
        // build filter compatible with layer groups
        List<Filter> filtersG = new ArrayList<>();
        filtersG.add(Predicates.isInstanceOf(LayerGroupInfo.class));
        filtersG.addAll(((And) filter).getChildren());
        // build filter compatible with both layer groups and layers
        return Predicates.or(Predicates.and(filtersL), Predicates.and(filtersG));
    }

    @Override
    public int size() {
        int remainingSize =
                catalog.getFacade().count(PublishedInfo.class, catalogFilter()) - offset;
        return Math.min(count, Math.max(0, remainingSize));
    }
}
