/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.function;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.GWCSynchEnv;
import org.geotools.filter.FilterFactoryImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.expression.Function;

public class IsCachedFunctionTest {

    private GWC mockGWC;

    private GWCSynchEnv mockGWCSynchEnv;

    private LayerInfo layerInfo;

    @Before
    public void setUp() throws Exception {
        mockGWC = mock(GWC.class);
        mockGWCSynchEnv = mock(GWCSynchEnv.class);
        GWC.set(mockGWC, mockGWCSynchEnv);
        layerInfo = mock(LayerInfo.class);

        when(mockGWC.hasTileLayer(eq(layerInfo))).thenReturn(true);
    }

    @After
    public void tearDown() throws Exception {
        GWC.set(null, null);
    }

    @Test
    public void testIsCachedFunction() {
        FilterFactoryImpl ff = new FilterFactoryImpl();

        Function exp = ff.function("isCached", ff.property("."));
        Object value = exp.evaluate(layerInfo);
        assertEquals(true, value);
    }
}
