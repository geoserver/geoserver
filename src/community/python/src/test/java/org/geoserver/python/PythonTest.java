/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python;

import org.junit.Test;
import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.python.core.PyString;
import org.python.core.PyType;

import com.vividsolutions.jts.geom.Point;

import static org.junit.Assert.assertEquals;

public class PythonTest {

    @Test
    public void testToJavaClass() {
        assertEquals(String.class, Python.toJavaClass(PyString.TYPE));
        assertEquals(Integer.class, Python.toJavaClass(PyInteger.TYPE));
        assertEquals(Double.class, Python.toJavaClass(PyFloat.TYPE));
        assertEquals(Point.class, Python.toJavaClass(PyType.fromClass(Point.class)));

    }
}
