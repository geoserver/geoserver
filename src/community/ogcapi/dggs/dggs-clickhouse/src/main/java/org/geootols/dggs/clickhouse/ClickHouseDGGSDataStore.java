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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geotools.data.DataStore;
import org.geotools.data.DefaultServiceInfo;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureWriter;
import org.geotools.data.LockingManager;
import org.geotools.data.Query;
import org.geotools.data.ServiceInfo;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.dggs.DGGSInstance;
import org.geotools.dggs.gstore.DGGSFeatureSource;
import org.geotools.dggs.gstore.DGGSResolutionCalculator;
import org.geotools.dggs.gstore.DGGSStore;
import org.geotools.feature.FeatureTypes;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

/**
 * Wraps another store containing feature types with the following characteristics:
 *
 * <ul>
 *   <li>A zoneId string attribute (TODO: make this configurable per DGGS, H3 should be using Long)
 *   <li>A resolution attribute (for ease of filtering)
 *   <li>Does not have a "geometry" attribute, the adds it
 * </ul>
 *
 * The zoneId is interpreted as a DGGS zone identifier, and common spatial queries and DGGS query
 * functions translated into tests against the zone identifiers.
 */
public class ClickHouseDGGSDataStore implements DGGSStore {

    static final Logger LOGGER = Logging.getLogger(ClickHouseDGGSDataStore.class);

    /** The geometry property, in the returned features */
    public static final String GEOMETRY = "geometry";
    /** The default geometry property, often used in filters */
    public static final String DEFAULT_GEOMETRY = "";
    /** The resolution property, in the source and returned features */
    private final DGGSInstance dggs;

    private final DataStore delegate;
    private final DGGSResolutionCalculator resolutions;

    public ClickHouseDGGSDataStore(DGGSInstance dggs, DataStore delegate) {
        this.delegate = delegate;
        this.dggs = dggs;
        this.resolutions = new DGGSResolutionCalculator(dggs);
    }

    @Override
    public ServiceInfo getInfo() {
        DefaultServiceInfo info = new DefaultServiceInfo();
        info.setDescription("Features from " + getClass().getSimpleName());
        info.setSchema(FeatureTypes.DEFAULT_NAMESPACE);
        return info;
    }

    @Override
    public List<Name> getNames() throws IOException {
        return delegate.getNames()
                .stream()
                .filter(
                        n -> {
                            try {
                                return isDGGSSchema(delegate.getSchema(n));
                            } catch (IOException e) {
                                LOGGER.log(Level.WARNING, "Failed to grab schema for " + n, e);
                                return false;
                            }
                        })
                .collect(Collectors.toList());
    }

    @Override
    public String[] getTypeNames() throws IOException {
        return Arrays.stream(delegate.getTypeNames())
                .filter(
                        t -> {
                            try {
                                return isDGGSSchema(delegate.getSchema(t));
                            } catch (IOException e) {
                                LOGGER.log(Level.WARNING, "Failed to grab schema for " + t, e);
                                return false;
                            }
                        })
                .toArray(n -> new String[n]);
    }

    private boolean isDGGSSchema(SimpleFeatureType schema) {
        return checkAttribute(schema, ZONE_ID, String.class)
                && checkAttribute(schema, RESOLUTION, Byte.class, Short.class, Integer.class)
                && schema.getDescriptor(GEOMETRY) == null;
    }

    private boolean checkAttribute(
            SimpleFeatureType schema, String name, Class<?>... expectedBindings) {
        return Optional.ofNullable(schema.getDescriptor(name))
                .filter(
                        ad -> {
                            for (Class<?> binding : expectedBindings) {
                                if (binding.isAssignableFrom(ad.getType().getBinding())) {
                                    return true;
                                }
                            }
                            return false;
                        })
                .isPresent();
    }

    @Override
    public SimpleFeatureType getSchema(Name name) throws IOException {
        SimpleFeatureType ft = delegate.getSchema(name);
        return wrapType(ft);
    }

    @Override
    public SimpleFeatureType getSchema(String typeName) throws IOException {
        SimpleFeatureType ft = delegate.getSchema(typeName);
        return wrapType(ft);
    }

    private SimpleFeatureType wrapType(SimpleFeatureType ft) {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName(ft.getName());
        for (AttributeDescriptor ad : ft.getAttributeDescriptors()) {
            tb.minOccurs(0);
            tb.add(ad.getLocalName(), ad.getType().getBinding());
        }
        tb.add("geometry", Polygon.class, DefaultGeographicCRS.WGS84);
        return tb.buildFeatureType();
    }

    @Override
    public DGGSFeatureSource getFeatureSource(String typeName) throws IOException {
        return new ClickHouseDGGSFeatureSource(
                this, delegate.getFeatureSource(typeName), getSchema(typeName));
    }

    @Override
    public DGGSFeatureSource getFeatureSource(Name typeName) throws IOException {
        return new ClickHouseDGGSFeatureSource(
                this, delegate.getFeatureSource(typeName), getSchema(typeName));
    }

    @Override
    public DGGSFeatureSource getDGGSFeatureSource(String typeName) throws IOException {
        return getFeatureSource(typeName);
    }

    @Override
    public void dispose() {
        try {
            delegate.dispose();
        } finally {
            dggs.close();
        }
    }

    @Override
    public FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(
            Query query, Transaction transaction) throws IOException {
        // just delegating to the FeatureSource machinery
        DGGSFeatureSource source = getFeatureSource(query.getTypeName());
        SimpleFeatureIterator features = source.getFeatures(query).features();
        return new FeatureReader<SimpleFeatureType, SimpleFeature>() {
            @Override
            public SimpleFeatureType getFeatureType() {
                return source.getSchema();
            }

            @Override
            public SimpleFeature next()
                    throws IOException, IllegalArgumentException, NoSuchElementException {
                return features.next();
            }

            @Override
            public boolean hasNext() throws IOException {
                return features.hasNext();
            }

            @Override
            public void close() throws IOException {
                features.next();
            }
        };
    }

    @Override
    public LockingManager getLockingManager() {
        // cannot write so not relevant, but still need to respect interface
        return delegate.getLockingManager();
    }

    // UNSUPPORTED WRITE STUFF -----------------------------------------------------

    @Override
    public void updateSchema(String typeName, SimpleFeatureType featureType) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeSchema(String typeName) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(
            String typeName, Filter filter, Transaction transaction) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(
            String typeName, Transaction transaction) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriterAppend(
            String typeName, Transaction transaction) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createSchema(SimpleFeatureType featureType) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateSchema(Name typeName, SimpleFeatureType featureType) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeSchema(Name typeName) throws IOException {
        throw new UnsupportedOperationException();
    }

    public DataStore getDelegate() {
        return delegate;
    }

    protected DGGSInstance getDggs() {
        return dggs;
    }

    protected DGGSResolutionCalculator getResolutions() {
        return resolutions;
    }
}
