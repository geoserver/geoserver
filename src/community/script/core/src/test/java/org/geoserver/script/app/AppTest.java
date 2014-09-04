/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.app;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.geoserver.script.ScriptIntTestSupport;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class AppTest extends ScriptIntTestSupport {

    File app;
    String ext;
    
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
    
        app = getScriptManager().findOrCreateAppDir("foo");
        ext = getExtension();
    }

    protected String getExtension() {
        return "js";
    }

    public void testSimple() throws Exception {
        FileUtils.copyURLToFile(
            getClass().getResource("main-helloWorld."+ext), new File(app, "main."+ext));
    
        MockHttpServletResponse resp = getAsServletResponse("/script/apps/foo/main."+ext);
        assertEquals(200, resp.getStatusCode());
        assertEquals("Hello World!", resp.getOutputStreamContent());
    
    }
}
