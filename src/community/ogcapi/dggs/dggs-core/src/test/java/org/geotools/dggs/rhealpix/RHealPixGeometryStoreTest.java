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
package org.geotools.dggs.rhealpix;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collections;
import jep.JepException;
import org.geotools.dggs.DGGSInstance;
import org.geotools.dggs.gstore.DGGSGeometryStore;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class RHealPixGeometryStoreTest {

    @Before
    public void ensureRHealPixAvailable() {
        Assume.assumeTrue(new RHealPixDGGSFactory().isAvailable());
    }

    @After
    public void cleanup() throws JepException {
        JEPWebRuntime.closeThreadIntepreter();
    }

    @Test
    public void testStoreCreation() throws IOException {
        try (DGGSInstance instance =
                new RHealPixDGGSFactory().createInstance(Collections.emptyMap())) {
            DGGSGeometryStore store = new DGGSGeometryStore(instance);
            String[] typeNames = store.getTypeNames();
            assertEquals(1, typeNames.length);
            assertEquals("TB16-Pix", typeNames[0]);
        }
    }
}
