/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.python.app.PythonResource;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

import static org.junit.Assert.assertEquals;

public class PythonResourceTest {

    static Python python;
    
    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        python = new Python(new GeoServerResourceLoader(new File("target")));
        
        File f = new File( python.getLibRoot(), "bar.py");
        FileWriter w = new FileWriter(f);
        w.write("class Bar:\n  pass\n\n");
        w.close();
        
        f = new File( python.getScriptRoot(), "foo.py");
        w = new FileWriter(f);
        w.write("print 'foo'");
        w.close();
        
        f = new File( python.getScriptRoot(), "foo_import.py");
        w = new FileWriter(f);
        w.write("import bar;b = bar.Bar();print b.__class__.__name__");
        w.close();
        
        f = new File( python.getScriptRoot(), "foo_args.py");
        w = new FileWriter(f);
        w.write("print thearg");
        w.close();
    }
    
    @Test
    public void testBasic() throws Exception {
        Request req = request();
        Response resp = new Response(req);
        PythonResource r = new PythonResource(python, 
            new File(python.getScriptRoot(), "foo.py"), req, resp);
        
        r.handleGet();
        assertResponse(resp, "foo");
    }
    
    @Test
    public void testImport() throws Exception {
        Request req = request();
        Response resp = new Response(req);
        PythonResource r = new PythonResource(python, 
            new File(python.getScriptRoot(), "foo_import.py"), req, resp);
        
        r.handleGet();
        assertResponse( resp, "Bar");
    }
    
    @Test
    public void testWithArguments() throws Exception {
        Request req = request();
        Reference ref = new Reference();
        ref.setQuery("thearg=theval");
        req.setResourceRef(ref);
        
        Response resp = new Response(req);
        PythonResource r = new PythonResource(python, 
            new File(python.getScriptRoot(), "foo_args.py"), req, resp);
        
        r.handleGet();
        assertResponse(resp, "theval");
    }
    
    Request request() {
        Request req = new Request();
        Reference ref = new Reference();
        req.setResourceRef(ref);
        return req;
    }
    
    void assertResponse(Response r, String content) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        r.getEntity().write(bout);
        
        assertEquals(Status.SUCCESS_OK, r.getStatus());
        assertEquals(content, new String(bout.toByteArray()).trim());
    }
}
