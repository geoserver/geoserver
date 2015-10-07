/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.rest;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.io.FileUtils;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.script.ScriptIntTestSupport;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class ScriptFinderTest extends ScriptIntTestSupport {

    protected XpathEngine xp;

    @Override
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("html", "http://www.w3.org/1999/xhtml");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xp = XMLUnit.newXpathEngine();
    }

    // Apps

    public void testGetApp() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("/rest/scripts/apps/app1/main.py");
        assertEquals(404, resp.getStatusCode());

        File dir = scriptMgr.script("apps/app1").dir();
        FileUtils.writeStringToFile(new File(dir, "main.py"), "print 'foo'");

        resp = getAsServletResponse("/rest/scripts/apps/app1/main.py");
        assertEquals(200, resp.getStatusCode());
        assertEquals("print 'foo'", resp.getOutputStreamContent());
    }

    public void testPutApp() throws Exception {
        assertNull(scriptMgr.findScriptFile("apps/app1/main.py"));

        String body = "print 'hello';";
        MockHttpServletResponse resp = putAsServletResponse("/rest/scripts/apps/app1/main.py",
                body, "text/plain");
        assertEquals(201, resp.getStatusCode());

        assertNotNull(scriptMgr.findScriptFile("apps/app1/main.py"));
    }

    public void testGetAllApps() throws Exception {
        // Make sure we get an empty response
        JSON json = getAsJSON("/rest/scripts/apps.json");
        assertTrue(((JSONObject) json).getString("scripts").isEmpty());

        // Add two Functions scripts
        File dir = scriptMgr.script("apps").dir();
        FileUtils.writeStringToFile(new File(new File(dir, "foo"), "main.py"), "print 'foo'");
        FileUtils.writeStringToFile(new File(new File(dir, "bar"), "main.py"), "print 'bar'");

        // JSON
        json = getAsJSON("/rest/scripts/apps.json");
        JSONArray scripts = ((JSONObject) json).getJSONObject("scripts").getJSONArray("script");
        assertEquals(2, scripts.size());
        for (int i = 0; i < scripts.size(); i++) {
            JSONObject script = scripts.getJSONObject(i);
            assertTrue(script.containsKey("name"));
            assertTrue(script.containsKey("href"));
            String name = script.getString("name");
            assertTrue(name.equals("foo/main.py") || name.equals("bar/main.py"));
            String href = script.getString("href");
            assertTrue(href.equals("http://localhost/geoserver/rest/scripts/apps/foo/main.py")
                    || href.equals("http://localhost/geoserver/rest/scripts/apps/bar/main.py"));
        }

        // XML
        Document doc = getAsDOM("/rest/scripts/apps.xml");
        assertEquals("scripts", doc.getDocumentElement().getTagName());
        NodeList scriptNodes = doc.getElementsByTagName("script");
        assertEquals(2, scriptNodes.getLength());

        // HTML
        Document htmlDom = getAsDOM("/rest/scripts/apps.html");
        NodeList links = xp.getMatchingNodes("//html:a", htmlDom);
        assertEquals(2, links.getLength());

        // HTML - No extension
        htmlDom = getAsDOM("/rest/scripts/apps");
        links = xp.getMatchingNodes("//html:a", htmlDom);
        assertEquals(2, links.getLength());
    }

    public void testDeleteApp() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("/rest/scripts/apps/app1/main.py");
        assertEquals(404, resp.getStatusCode());

        File dir = scriptMgr.script("apps/app1").dir();
        FileUtils.writeStringToFile(new File(dir, "main.py"), "print 'foo'");

        resp = getAsServletResponse("/rest/scripts/apps/app1/main.py");
        assertEquals(200, resp.getStatusCode());
        assertEquals("print 'foo'", resp.getOutputStreamContent());

        resp = deleteAsServletResponse("/rest/scripts/apps/app1/main.py");
        assertEquals(200, resp.getStatusCode());

        resp = getAsServletResponse("/rest/scripts/apps/app1/main.py");
        assertEquals(404, resp.getStatusCode());
        assertFalse(dir.exists());
    }

    // WFS TX

    public void testGetWfsTx() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("/rest/scripts/wfs/tx/foo.py");
        assertEquals(404, resp.getStatusCode());

        File dir = scriptMgr.script("wfs/tx").dir();
        FileUtils.writeStringToFile(new File(dir, "foo.py"), "print 'foo'");

        resp = getAsServletResponse("/rest/scripts/wfs/tx/foo.py");
        assertEquals(200, resp.getStatusCode());
        assertEquals("print 'foo'", resp.getOutputStreamContent());
    }

    public void testPutWfsTx() throws Exception {
        assertNull(scriptMgr.findScriptFile("wfs/tx/bar.py"));

        String body = "print 'hello';";
        MockHttpServletResponse resp = putAsServletResponse("/rest/scripts/wfs/tx/bar.py", body,
                "text/plain");
        assertEquals(201, resp.getStatusCode());

        assertNotNull(scriptMgr.findScriptFile("wfs/tx/bar.py"));
    }

    public void testGetAllWfsTx() throws Exception {
        // Make sure we get an empty response
        JSON json = getAsJSON("/rest/scripts/wfs/tx.json");
        assertTrue(((JSONObject) json).getString("scripts").isEmpty());

        // Add two Functions scripts
        File dir = scriptMgr.script("wfs/tx").dir();
        FileUtils.writeStringToFile(new File(dir, "foo.py"), "print 'foo'");
        FileUtils.writeStringToFile(new File(dir, "bar.py"), "print 'bar'");

        // JSON
        json = getAsJSON("/rest/scripts/wfs/tx.json");
        JSONArray scripts = ((JSONObject) json).getJSONObject("scripts").getJSONArray("script");
        assertEquals(2, scripts.size());
        for (int i = 0; i < scripts.size(); i++) {
            JSONObject script = scripts.getJSONObject(i);
            assertTrue(script.containsKey("name"));
            assertTrue(script.containsKey("href"));
            String name = script.getString("name");
            assertTrue(name.equals("foo.py") || name.equals("bar.py"));
            String href = script.getString("href");
            assertTrue(href.equals("http://localhost/geoserver/rest/scripts/wfs/tx/foo.py")
                    || href.equals("http://localhost/geoserver/rest/scripts/wfs/tx/bar.py"));
        }

        // XML
        Document doc = getAsDOM("/rest/scripts/wfs/tx.xml");
        assertEquals("scripts", doc.getDocumentElement().getTagName());
        NodeList scriptNodes = doc.getElementsByTagName("script");
        assertEquals(2, scriptNodes.getLength());

        // HTML
        Document htmlDom = getAsDOM("/rest/scripts/wfs/tx.html");
        NodeList links = xp.getMatchingNodes("//html:a", htmlDom);
        assertEquals(2, links.getLength());

        // HTML - No extension
        htmlDom = getAsDOM("/rest/scripts/wfs/tx");
        links = xp.getMatchingNodes("//html:a", htmlDom);
        assertEquals(2, links.getLength());
    }

    public void testDeleteWfsTx() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("/rest/scripts/wfs/tx/foo.py");
        assertEquals(404, resp.getStatusCode());

        File dir = scriptMgr.script("wfs/tx").dir();
        FileUtils.writeStringToFile(new File(dir, "foo.py"), "print 'foo'");

        resp = getAsServletResponse("/rest/scripts/wfs/tx/foo.py");
        assertEquals(200, resp.getStatusCode());
        assertEquals("print 'foo'", resp.getOutputStreamContent());

        resp = deleteAsServletResponse("/rest/scripts/wfs/tx/foo.py");
        assertEquals(200, resp.getStatusCode());

        resp = getAsServletResponse("/rest/scripts/wfs/tx/foo.py");
        assertEquals(404, resp.getStatusCode());
    }

    // Function

    public void testGetFunction() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("/rest/scripts/function/foo.py");
        assertEquals(404, resp.getStatusCode());

        File dir = scriptMgr.script("function").dir();
        FileUtils.writeStringToFile(new File(dir, "foo.py"), "print 'foo'");

        resp = getAsServletResponse("/rest/scripts/function/foo.py");
        assertEquals(200, resp.getStatusCode());
        assertEquals("print 'foo'", resp.getOutputStreamContent());
    }

    public void testPutFunction() throws Exception {
        assertNull(scriptMgr.findScriptFile("function/bar.py"));

        String body = "print 'hello';";
        MockHttpServletResponse resp = putAsServletResponse("/rest/scripts/function/bar.py", body,
                "text/plain");
        assertEquals(201, resp.getStatusCode());

        assertNotNull(scriptMgr.findScriptFile("function/bar.py"));
    }

    public void testGetAllFunctions() throws Exception {
        // Make sure we get an empty response
        JSON json = getAsJSON("/rest/scripts/function.json");
        assertTrue(((JSONObject) json).getString("scripts").isEmpty());

        // Add two Functions scripts
        File dir = scriptMgr.script("function").dir();
        FileUtils.writeStringToFile(new File(dir, "foo.py"), "print 'foo'");
        FileUtils.writeStringToFile(new File(dir, "bar.py"), "print 'bar'");

        // JSON
        json = getAsJSON("/rest/scripts/function.json");
        JSONArray scripts = ((JSONObject) json).getJSONObject("scripts").getJSONArray("script");
        assertEquals(2, scripts.size());
        for (int i = 0; i < scripts.size(); i++) {
            JSONObject script = scripts.getJSONObject(i);
            assertTrue(script.containsKey("name"));
            assertTrue(script.containsKey("href"));
            String name = script.getString("name");
            assertTrue(name.equals("foo.py") || name.equals("bar.py"));
            String href = script.getString("href");
            assertTrue(href.equals("http://localhost/geoserver/rest/scripts/function/foo.py")
                    || href.equals("http://localhost/geoserver/rest/scripts/function/bar.py"));
        }

        // XML
        Document doc = getAsDOM("/rest/scripts/function.xml");
        assertEquals("scripts", doc.getDocumentElement().getTagName());
        NodeList scriptNodes = doc.getElementsByTagName("script");
        assertEquals(2, scriptNodes.getLength());

        // HTML
        Document htmlDom = getAsDOM("/rest/scripts/function.html");
        NodeList links = xp.getMatchingNodes("//html:a", htmlDom);
        assertEquals(2, links.getLength());

        // HTML - No extension
        htmlDom = getAsDOM("/rest/scripts/function");
        links = xp.getMatchingNodes("//html:a", htmlDom);
        assertEquals(2, links.getLength());
    }

    public void testDeleteFunction() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("/rest/scripts/function/foo.py");
        assertEquals(404, resp.getStatusCode());

        File dir = scriptMgr.script("function").dir();
        FileUtils.writeStringToFile(new File(dir, "foo.py"), "print 'foo'");

        resp = getAsServletResponse("/rest/scripts/function/foo.py");
        assertEquals(200, resp.getStatusCode());
        assertEquals("print 'foo'", resp.getOutputStreamContent());

        resp = deleteAsServletResponse("/rest/scripts/function/foo.py");
        assertEquals(200, resp.getStatusCode());

        resp = getAsServletResponse("/rest/scripts/function/foo.py");
        assertEquals(404, resp.getStatusCode());
    }

    // WPS

    public void testGetWps() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("/rest/scripts/wps/foo.py");
        assertEquals(404, resp.getStatusCode());

        File dir = scriptMgr.script("wps").dir();
        FileUtils.writeStringToFile(new File(dir, "foo.py"), "print 'foo'");

        resp = getAsServletResponse("/rest/scripts/wps/foo.py");
        assertEquals(200, resp.getStatusCode());
        assertEquals("print 'foo'", resp.getOutputStreamContent());
    }

    public void testGetWpsWithNamespace() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("/rest/scripts/wps/foo.py");
        assertEquals(404, resp.getStatusCode());

        File dir = scriptMgr.script("wps").dir();
        File nsDir = new File(dir, "bar");
        FileUtils.writeStringToFile(new File(nsDir, "foo.py"), "print 'foo'");

        resp = getAsServletResponse("/rest/scripts/wps/bar:foo.py");
        assertEquals(200, resp.getStatusCode());
        assertEquals("print 'foo'", resp.getOutputStreamContent());
    }
    
    public void testPutWps() throws Exception {
        assertNull(scriptMgr.findScriptFile("wps/bar.py"));

        String body = "print 'hello';";
        MockHttpServletResponse resp = putAsServletResponse("/rest/scripts/wps/bar.py", body,
                "text/plain");
        assertEquals(201, resp.getStatusCode());

        assertNotNull(scriptMgr.findScriptFile("wps/bar.py"));
    }

    public void testPutWpsWithNamespace() throws Exception {
        assertNull(scriptMgr.findScriptFile("wps/foo/bar.py"));

        String body = "print 'hello';";
        MockHttpServletResponse resp = putAsServletResponse("/rest/scripts/wps/foo:bar.py", body,
                "text/plain");
        assertEquals(201, resp.getStatusCode());

        assertNotNull(scriptMgr.findScriptFile("wps/foo/bar.py"));
    }
    
    public void testGetAllWps() throws Exception {
        // Make sure we get an empty response
        JSON json = getAsJSON("/rest/scripts/wps.json");
        assertTrue(((JSONObject) json).getString("scripts").isEmpty());

        // Add two WPS scripts
        File dir = scriptMgr.script("wps").dir();
        FileUtils.writeStringToFile(new File(dir, "foo.py"), "print 'foo'");
        FileUtils.writeStringToFile(new File(dir, "bar.py"), "print 'bar'");
        // Add WPS script with custom namespace
        File subDir = new File(dir, "custom");
        FileUtils.writeStringToFile(new File(subDir, "buffer.py"), "print 'buffer'");
        
        // JSON
        json = getAsJSON("/rest/scripts/wps.json");
        JSONArray scripts = ((JSONObject) json).getJSONObject("scripts").getJSONArray("script");
        assertEquals(3, scripts.size());
        for (int i = 0; i < scripts.size(); i++) {
            JSONObject script = scripts.getJSONObject(i);
            assertTrue(script.containsKey("name"));
            assertTrue(script.containsKey("href"));
            String name = script.getString("name");
            assertTrue(name.equals("foo.py") || name.equals("bar.py") || name.equals("custom:buffer.py"));
            String href = script.getString("href");
            assertTrue(href.equals("http://localhost/geoserver/rest/scripts/wps/foo.py")
                    || href.equals("http://localhost/geoserver/rest/scripts/wps/bar.py")
                    || href.equals("http://localhost/geoserver/rest/scripts/wps/custom:buffer.py"));
        }

        // XML
        Document doc = getAsDOM("/rest/scripts/wps.xml");
        assertEquals("scripts", doc.getDocumentElement().getTagName());
        NodeList scriptNodes = doc.getElementsByTagName("script");
        assertEquals(3, scriptNodes.getLength());

        // HTML
        Document htmlDom = getAsDOM("/rest/scripts/wps.html");
        NodeList links = xp.getMatchingNodes("//html:a", htmlDom);
        assertEquals(3, links.getLength());

        // HTML - No extension
        htmlDom = getAsDOM("/rest/scripts/wps");
        links = xp.getMatchingNodes("//html:a", htmlDom);
        assertEquals(3, links.getLength());
    }

    public void testDeleteWps() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("/rest/scripts/wps/foo.py");
        assertEquals(404, resp.getStatusCode());

        File dir = scriptMgr.script("wps").dir();
        FileUtils.writeStringToFile(new File(dir, "foo.py"), "print 'foo'");

        resp = getAsServletResponse("/rest/scripts/wps/foo.py");
        assertEquals(200, resp.getStatusCode());
        assertEquals("print 'foo'", resp.getOutputStreamContent());

        resp = deleteAsServletResponse("/rest/scripts/wps/foo.py");
        assertEquals(200, resp.getStatusCode());

        resp = getAsServletResponse("/rest/scripts/wps/foo.py");
        assertEquals(404, resp.getStatusCode());
    }
    
    public void testDeleteWpsWithNamespace() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("/rest/scripts/wps/bar:foo.py");
        assertEquals(404, resp.getStatusCode());

        File dir = scriptMgr.script("wps").dir();
        File nsDir = new File(dir, "bar");
        FileUtils.writeStringToFile(new File(nsDir, "foo.py"), "print 'foo'");

        resp = getAsServletResponse("/rest/scripts/wps/bar:foo.py");
        assertEquals(200, resp.getStatusCode());
        assertEquals("print 'foo'", resp.getOutputStreamContent());

        resp = deleteAsServletResponse("/rest/scripts/wps/bar:foo.py");
        assertEquals(200, resp.getStatusCode());

        resp = getAsServletResponse("/rest/scripts/wps/bar:foo.py");
        assertEquals(404, resp.getStatusCode());

    }
}
