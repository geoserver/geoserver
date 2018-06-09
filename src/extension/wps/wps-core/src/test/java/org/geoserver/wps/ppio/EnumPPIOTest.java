/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import org.geotools.data.Parameter;
import org.junit.Test;

public class EnumPPIOTest {

    @Test
    public void test() throws Exception {
        EnumPPIO ppio = new EnumPPIO(TestEnum.class);
        assertEquals(TestEnum.FOO, ppio.decode("FOO"));
        assertEquals(TestEnum.FOO, ppio.decode("foo"));
        assertEquals(TestEnum.BAR, ppio.decode("BAR"));

        try {
            ppio.decode("BAZ");
            fail("Invalid value should have thrown an exception");
        } catch (Exception e) {
        }
    }

    @Test
    public void testFind() {
        Parameter p = new Parameter("test", TestEnum.class, "Test", "This is a test");
        ProcessParameterIO ppio = ProcessParameterIO.find(p, null, null);
        assertTrue(ppio instanceof EnumPPIO);
    }

    @Test
    public void testFindAll() {
        Parameter p = new Parameter("test", TestEnum.class, "Test", "This is a test");
        List<ProcessParameterIO> ppio = ProcessParameterIO.findAll(p, null);

        assertEquals(1, ppio.size());
        assertTrue(ppio.get(0) instanceof EnumPPIO);
    }

    enum TestEnum {
        FOO,
        BAR
    }
}
