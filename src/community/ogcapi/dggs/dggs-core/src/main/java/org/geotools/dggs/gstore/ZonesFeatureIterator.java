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
package org.geotools.dggs.gstore;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.geotools.data.simple.SimpleFeatureReader;
import org.geotools.dggs.Zone;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

class ZonesFeatureIterator implements SimpleFeatureReader {
    private final Iterator<Zone> iterator;
    private final List<String> requestedProperties;
    SimpleFeatureBuilder fb;
    List<Boolean> includeExtraProperties;

    public ZonesFeatureIterator(
            Iterator<Zone> iterator,
            SimpleFeatureType schema,
            List<AttributeDescriptor> extraProperties) {
        this.iterator = iterator;
        fb = new SimpleFeatureBuilder(schema);
        requestedProperties =
                schema.getAttributeDescriptors()
                        .stream()
                        .map(ad -> ad.getLocalName())
                        .collect(Collectors.toList());
    }

    @Override
    public SimpleFeatureType getFeatureType() {
        return fb.getFeatureType();
    }

    @Override
    public SimpleFeature next()
            throws IOException, IllegalArgumentException, NoSuchElementException {
        Zone zone = iterator.next();
        String id = zone.getId();
        for (String p : requestedProperties) {
            if (DGGSGeometryStore.GEOMETRY.equals(p)) {
                fb.add(zone.getBoundary());
            } else if (DGGSGeometryStore.ZONE_ID.equals(p)) {
                fb.add(id);
            } else if (DGGSGeometryStore.RESOLUTION.equals(p)) {
                fb.add(zone.getResolution());
            } else {
                fb.add(zone.getExtraProperty(p));
            }
        }

        return fb.buildFeature(fb.getFeatureType().getTypeName() + "." + id);
    }

    @Override
    public boolean hasNext() throws IOException {
        return iterator.hasNext();
    }

    @Override
    public void close() throws IOException {
        // nothing to do
    }
}
