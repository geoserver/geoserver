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
import org.geoserver.csw.feature.AbstractFeatureCollection;
import org.geoserver.csw.feature.MemoryFeatureCollection;
import org.geoserver.csw.records.CSWRecordDescriptor;
import org.geoserver.csw.records.RecordDescriptor;
import org.geotools.data.store.FilteringFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

/**
 * Internal Catalog Store Feature Collection Creates a Catalog Store from a GeoServer Catalog
 * instance and a Mapping Will map data from GeoServer catalog to a particular CSW Record Type,
 * provided in Record Descriptor
 *
 * @author Niels Charlier
 */
class CatalogStoreFeatureCollection extends AbstractFeatureCollection<FeatureType, Feature> {

    protected int offset, count;
    protected SortBy[] sortOrder;
    protected Filter filter;
    protected Catalog catalog;
    protected CatalogStoreMapping mapping;
    protected RecordDescriptor rd;
    protected Map<String, String> interpolationProperties = new HashMap<String, String>();

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
                offset, count, sortOrder, filter, catalog, mapping, rd, interpolationProperties);
    }

    @Override
    protected void closeIterator(Iterator<Feature> close) {}

    @Override
    public FeatureCollection<FeatureType, Feature> subCollection(Filter filter) {
        return new FilteringFeatureCollection<FeatureType, Feature>(this, filter);
    }

    @Override
    public FeatureCollection<FeatureType, Feature> sort(SortBy order) {
        List<Feature> features = new ArrayList<Feature>();
        MemoryFeatureCollection memory = new MemoryFeatureCollection(getSchema(), features);
        return memory.sort(order);
    }
}
