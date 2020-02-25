/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.py;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.geoserver.script.ScriptIntTestSupport;
import org.springframework.mock.web.MockHttpServletResponse;

public class PyAppTest extends ScriptIntTestSupport {

    File app;

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();

        app = getScriptManager().app("foo").dir();
    }

    public void testSimple() throws Exception {
        FileUtils.copyURLToFile(
                getClass().getResource("main-helloWorld.py"), new File(app, "main.py"));

        MockHttpServletResponse resp = getAsServletResponse("rest/apps/foo/main.py");
        assertEquals(200, resp.getStatus());
        assertEquals("Hello World!", resp.getContentAsString());
    }

    public void testContentType() throws Exception {
        FileUtils.copyURLToFile(
                getClass().getResource("main-helloWorldJSON.py"), new File(app, "main.py"));

        MockHttpServletResponse resp = getAsServletResponse("/rest/apps/foo/main.py");
        assertEquals(200, resp.getStatus());
        assertEquals("application/json", resp.getContentType());
    }
}
