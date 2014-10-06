/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;

import org.geoserver.platform.GeoServerResourceLoader;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.python.util.PythonInterpreter;
import org.vfny.geoserver.global.GeoserverDataDirectory;

public class PythonTestSupport {

    protected static Python python;
    
    protected PythonInterpreter pi;
    protected ByteArrayOutputStream out;
    
    @BeforeClass
    public static void setUpPython() throws Exception {
        GeoServerResourceLoader loader = new GeoServerResourceLoader(new File("target"));
        
        python = new Python(loader);
        GeoserverDataDirectory.setResourceLoader(loader);
    }
    
    @AfterClass
    public static void tearDownPython() throws Exception {
        GeoserverDataDirectory.setResourceLoader(null);
    }
    
    @Before
    public void setUpPythonInterpreter() throws Exception {
        out = new ByteArrayOutputStream();
        pi = python.interpreter();
        pi.setOut(out);
    }
    
    void print() {
        System.out.println(new String(out.toByteArray()));
    }
    
    void clear() {
        out = new ByteArrayOutputStream();
        pi.setOut(out);
    }
    
    void _assert(String result) {
        assertEquals(result, new String(out.toByteArray()).trim());
    }
}
