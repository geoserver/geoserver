/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geootols.dggs.clickhouse;

import static org.geootols.dggs.clickhouse.ClickHouseDGGSDataStore.DEFAULT_GEOMETRY;
import static org.geootols.dggs.clickhouse.ClickHouseDGGSDataStore.GEOMETRY;

import java.awt.RenderingHints;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.geotools.data.DataAccess;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureListener;
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.ResourceInfo;
import org.geotools.data.crs.ForceCoordinateSystemFeatureResults;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.dggs.DGGSInstance;
import org.geotools.dggs.gstore.DGGSFeatureSource;
import org.geotools.feature.SchemaException;
import org.geotools.feature.collection.FilteringSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class ClickHouseDGGSFeatureSource implements DGGSFeatureSource {

    private final ClickHouseDGGSDataStore dataStore;
    private final SimpleFeatureType schema;
    private final SimpleFeatureSource delegate;
    private final DGGSQuerySplitter splitter;

    public ClickHouseDGGSFeatureSource(
            ClickHouseDGGSDataStore dataStore,
            SimpleFeatureSource delegate,
            SimpleFeatureType schema) {
        this.delegate = delegate;
        this.dataStore = dataStore;
        this.schema = schema;
        this.splitter =
                new DGGSQuerySplitter(dataStore.getDggs(), dataStore.getResolutions(), schema);
    }

    @Override
    public DataAccess<SimpleFeatureType, SimpleFeature> getDataStore() {
        return dataStore;
    }

    @Override
    public SimpleFeatureType getSchema() {
        return schema;
    }

    @Override
    public Set<RenderingHints.Key> getSupportedHints() {
        return delegate.getSupportedHints();
    }

    @Override
    public DGGSInstance getDGGS() {
        return dataStore.getDggs();
    }

    @Override
    public Name getName() {
        return getSchema().getName();
    }

    @Override
    public ResourceInfo getInfo() {
        // TODO: this is a copy from ContentFeatureSource, it really just depends on the
        //  FeatureSource interface, can be generalized
        return new ResourceInfo() {
            final Set<String> words = new HashSet<>();

            {
                words.add("features");
                words.add(ClickHouseDGGSFeatureSource.this.getSchema().getTypeName());
            }

            @Override
            public ReferencedEnvelope getBounds() {
                try {
                    return ClickHouseDGGSFeatureSource.this.getBounds();
                } catch (IOException e) {
                    return null;
                }
            }

            @Override
            public CoordinateReferenceSystem getCRS() {
                return ClickHouseDGGSFeatureSource.this.getSchema().getCoordinateReferenceSystem();
            }

            @Override
            public String getDescription() {
                return null;
            }

            @Override
            public Set<String> getKeywords() {
                return words;
            }

            @Override
            public String getName() {
                return ClickHouseDGGSFeatureSource.this.getSchema().getTypeName();
            }

            @Override
            public URI getSchema() {
                Name name = ClickHouseDGGSFeatureSource.this.getSchema().getName();
                URI namespace;
                try {
                    namespace = new URI(name.getNamespaceURI());
                    return namespace;
                } catch (URISyntaxException e) {
                    return null;
                }
            }

            @Override
            public String getTitle() {
                Name name = ClickHouseDGGSFeatureSource.this.getSchema().getName();
                return name.getLocalPart();
            }
        };
    }

    @Override
    public QueryCapabilities getQueryCapabilities() {
        return delegate.getQueryCapabilities();
    }

    @Override
    public void addFeatureListener(FeatureListener listener) {
        // no events sent, no reason to handle this method
    }

    @Override
    public void removeFeatureListener(FeatureListener listener) {
        // no events sent, no reason to handle this method
    }

    @Override
    public ReferencedEnvelope getBounds() throws IOException {
        return getBounds(Query.ALL);
    }

    @Override
    public ReferencedEnvelope getBounds(Query query) throws IOException {
        // no fast way to compute the bounds
        return null;
    }

    @Override
    public int getCount(Query query) throws IOException {
        // try to split, if there is a post-filter give up, otherwise delegate
        DGGSQuerySplitter.PrePost split = splitter.split(query);
        if (split.post != Filter.INCLUDE) return -1;
        return delegate.getCount(split.pre);
    }

    @Override
    public SimpleFeatureCollection getFeatures() throws IOException {
        return getFeatures(Query.ALL);
    }

    @Override
    public SimpleFeatureCollection getFeatures(Filter filter) throws IOException {
        return getFeatures(new Query(getSchema().getTypeName(), filter));
    }

    @Override
    public SimpleFeatureCollection getFeatures(Query query) throws IOException {
        DGGSQuerySplitter.PrePost split = splitter.split(query);

        SimpleFeatureCollection result = delegate.getFeatures(split.pre);
        String[] propertyNames = query.getPropertyNames();
        // need to wrap only if the geometry property is requested
        if (propertyNames == null
                || Arrays.stream(propertyNames)
                        .anyMatch(n -> GEOMETRY.equals(n) || DEFAULT_GEOMETRY.equals(n))) {
            SimpleFeatureType outputSchema = getSchema();
            if (propertyNames != null) {
                outputSchema = SimpleFeatureTypeBuilder.retype(getSchema(), propertyNames);
            }
            result = new DGGSFeatureCollection(result, outputSchema, getDGGS());
        }
        if (split.post != Filter.INCLUDE) {
            result = new FilteringSimpleFeatureCollection(result, split.post);
        } else if (delegate.getDataStore() instanceof JDBCDataStore) {
            result =
                    new ClickhouseAggregatorCollection(
                            result,
                            (JDBCDataStore) delegate.getDataStore(),
                            new Query(split.pre),
                            getSchema());
        }
        result = reprojectFeatureCollection(result, query);
        return result;
    }

    /**
     * Handles default and target CRS in a similar way to {@link
     * org.geotools.data.store.ContentFeatureSource}, but using {@link SimpleFeatureCollection} as
     * the source.
     *
     * @return The collection reprojected according to the Query instructions
     */
    private SimpleFeatureCollection reprojectFeatureCollection(
            SimpleFeatureCollection result, Query query) throws IOException {
        CoordinateReferenceSystem sourceCRS = query.getCoordinateSystem();
        CoordinateReferenceSystem targetCRS = query.getCoordinateSystemReproject();

        if (sourceCRS != null && !sourceCRS.equals(DefaultGeographicCRS.WGS84)) {
            // override the nativeCRS
            try {
                result =
                        DataUtilities.simple(
                                new ForceCoordinateSystemFeatureResults(result, sourceCRS));
            } catch (SchemaException e) {
                throw (IOException)
                        new IOException("Error occurred trying to force CRS").initCause(e);
            }
        } else {
            // no override
            sourceCRS = DefaultGeographicCRS.WGS84;
        }
        if (targetCRS != null) {
            if (sourceCRS == null) {
                throw new IOException("Cannot reproject data, the source CRS is not available");
            } else if (!sourceCRS.equals(targetCRS)) {
                try {
                    result = new ReprojectingFeatureCollection(result, targetCRS);
                } catch (Exception e) {
                    if (e instanceof IOException) throw (IOException) e;
                    else
                        throw (IOException)
                                new IOException("Error occurred trying to reproject data")
                                        .initCause(e);
                }
            }
        }
        return result;
    }
}
