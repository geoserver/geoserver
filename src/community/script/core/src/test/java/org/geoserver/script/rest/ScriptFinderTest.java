/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.rest;

import java.io.File;

import net.sf.json.JSON;
import net.sf.json.JSONArray;

import org.apache.commons.io.FileUtils;
import org.geoserver.script.ScriptManager;
import org.geoserver.test.GeoServerTestSupport;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class ScriptFinderTest extends GeoServerTestSupport {

    ScriptManager scriptMgr;
    
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        scriptMgr = new ScriptManager(getDataDirectory());
    }

    public void testGet() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("/script/scripts/wps/foo.py");
        assertEquals(404, resp.getStatusCode());

        File dir = scriptMgr.findOrCreateScriptDir("wps");
        FileUtils.writeStringToFile(new File(dir, "foo.py"), "print 'foo'");

        resp = getAsServletResponse("/script/scripts/wps/foo.py");
        assertEquals(200, resp.getStatusCode());
        assertEquals("print 'foo'", resp.getOutputStreamContent());
    }

    public void testGetAll() throws Exception {
        JSON json = getAsJSON("/script/scripts/wps");
        JSONArray files = (JSONArray) json;
        assertTrue(files.isEmpty());

        File dir = scriptMgr.findOrCreateScriptDir("wps");
        FileUtils.writeStringToFile(new File(dir, "foo.py"), "print 'foo'");
        FileUtils.writeStringToFile(new File(dir, "bar.py"), "print 'bar'");

        files = (JSONArray) getAsJSON("/script/scripts/wps");
        assertEquals(2, files.size());
        assertTrue(files.contains("foo.py"));
        assertTrue(files.contains("bar.py"));
    }
}
