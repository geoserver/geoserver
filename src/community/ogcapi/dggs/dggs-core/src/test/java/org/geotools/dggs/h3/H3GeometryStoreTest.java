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
package org.geotools.dggs.h3;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collections;
import org.geotools.dggs.DGGSInstance;
import org.geotools.dggs.gstore.DGGSGeometryStore;
import org.junit.Test;

public class H3GeometryStoreTest {

    @Test
    public void testStoreCreation() throws IOException {
        try (DGGSInstance instance = new H3DGGSFactory().createInstance(Collections.emptyMap())) {
            DGGSGeometryStore store = new DGGSGeometryStore(instance);
            String[] typeNames = store.getTypeNames();
            assertEquals(1, typeNames.length);
            assertEquals("H3", typeNames[0]);
        }
    }
}
