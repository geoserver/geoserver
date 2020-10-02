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
import java.util.Arrays;
import java.util.List;
import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.dggs.DGGSInstance;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;

/**
 * A store returning DGGS zones as GeoTools {@link org.opengis.feature.simple.SimpleFeature},
 * without any actal data associated to them. It's pure DGGS structure description.
 */
public class DGGSGeometryStore extends ContentDataStore implements DGGSStore {

    public static final String ZONE_ID = "zoneId";
    public static final String RESOLUTION = "resolution";
    public static final String GEOMETRY = "geometry";

    DGGSInstance dggs;
    DGGSResolutionCalculator resolutions;

    public DGGSGeometryStore(DGGSInstance dggs) {
        this.dggs = dggs;
        this.resolutions = new DGGSResolutionCalculator(dggs);
    }

    @Override
    protected List<Name> createTypeNames() throws IOException {
        return Arrays.asList(new NameImpl(namespaceURI, dggs.getIdentifier()));
    }

    @Override
    protected DGGSGeometryFeatureSource createFeatureSource(ContentEntry entry) throws IOException {
        return new DGGSGeometryFeatureSource(entry, this);
    }

    @Override
    public void dispose() {
        dggs.close();
    }

    @Override
    public DGGSFeatureSource getDGGSFeatureSource(String typeName) throws IOException {
        ContentEntry entry = ensureEntry(new NameImpl(namespaceURI, typeName));
        return new DGGSGeometryFeatureSource(entry, this);
    }
}
