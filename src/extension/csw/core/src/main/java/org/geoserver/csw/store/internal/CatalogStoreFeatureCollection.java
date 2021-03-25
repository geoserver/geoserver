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
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.csw.feature.AbstractFeatureCollection;
import org.geoserver.csw.feature.MemoryFeatureCollection;
import org.geoserver.csw.records.CSWRecordDescriptor;
import org.geoserver.csw.records.RecordDescriptor;
import org.geotools.data.store.FilteringFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;

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

    protected static final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

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

        // ignore catalog info's without id
        Filter result =
                ff.and(filter, ff.not(ff.isNull(mapping.getIdentifierElement().getContent())));

        // build filter compatible with layergroups and resources
        result =
                ff.and(
                        ff.equals(ff.property("advertised"), ff.literal(true)),
                        ff.or(
                                /* Layergroup Filter */
                                ff.and(
                                        ff.equals(
                                                ff.property("type"),
                                                ff.literal(PublishedType.GROUP)),
                                        result),
                                /* Resource Filter */
                                ff.and(
                                        ff.notEqual(
                                                ff.property("type"),
                                                ff.literal(PublishedType.GROUP)),
                                        (Filter)
                                                result.accept(new ResourceFilterVisitor(), null))));
        return result;
    }

    @Override
    public int size() {
        int remainingSize =
                catalog.getFacade().count(PublishedInfo.class, catalogFilter()) - offset;
        return Math.min(count, Math.max(0, remainingSize));
    }
}
