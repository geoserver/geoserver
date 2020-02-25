/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.Collections;
import java.util.List;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geotools.data.Parameter;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.GenericApplicationContext;

public class ProcessParameterIOTest {

    public static class TestType {};

    private static ProcessParameterIO testPPIO =
            new ProcessParameterIO(TestType.class, TestType.class, "testPPIO") {};

    private static GenericApplicationContext context = new GenericApplicationContext();

    @BeforeClass
    public static void initAppContext() {
        PPIOFactory testPPIOFactory = () -> Collections.singletonList(testPPIO);
        context.getBeanFactory().registerSingleton("testPPIOFactory", testPPIOFactory);
        context.refresh();
        new GeoServerExtensions().setApplicationContext(context);
    }

    @AfterClass
    public static void destroyAppContext() {
        GeoServerExtensionsHelper.init(null);
    }

    @Test
    public void testFindAllWithNullContext() {
        List<ProcessParameterIO> matches =
                ProcessParameterIO.findAll(new Parameter<>("testPPIO", TestType.class), null);
        assertEquals(1, matches.size());
        assertSame(testPPIO, matches.get(0));
    }

    @Test
    public void testFindAllWithSameContext() {
        List<ProcessParameterIO> matches =
                ProcessParameterIO.findAll(new Parameter<>("testPPIO", TestType.class), context);
        assertEquals(1, matches.size());
        assertSame(testPPIO, matches.get(0));
    }

    @Test
    public void testFindAllWithDifferentContext() {
        GenericApplicationContext myContext = new GenericApplicationContext();
        myContext.refresh();
        List<ProcessParameterIO> matches =
                ProcessParameterIO.findAll(new Parameter<>("testPPIO", TestType.class), myContext);
        assertEquals(0, matches.size());
    }
}
