/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.py;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.script.ScriptManager;
import org.geoserver.test.GeoServerTestSupport;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class PyAppTest extends GeoServerTestSupport {

    File app;
   
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();

        app = getScriptManager().findOrCreateAppDir("foo");
    }

    protected ScriptManager getScriptManager() {
        return GeoServerExtensions.bean(ScriptManager.class);
    }

    public void testSimple() throws Exception {
        FileUtils.copyURLToFile(
            getClass().getResource("main-helloWorld.py"), new File(app, "main.py"));

        MockHttpServletResponse resp = getAsServletResponse("/script/apps/foo/main.py");
        assertEquals(200, resp.getStatusCode());
        assertEquals("Hello World!", resp.getOutputStreamContent());
    }

    public void testContentType() throws Exception {
        FileUtils.copyURLToFile(
            getClass().getResource("main-helloWorldJSON.py"), new File(app, "main.py"));

        MockHttpServletResponse resp = getAsServletResponse("/script/apps/foo/main.py");
        assertEquals(200, resp.getStatusCode());
        assertEquals("application/json", resp.getContentType());
    }
}
