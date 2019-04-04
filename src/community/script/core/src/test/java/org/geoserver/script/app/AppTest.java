/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.app;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.geoserver.script.ScriptIntTestSupport;
import org.springframework.mock.web.MockHttpServletResponse;

public class AppTest extends ScriptIntTestSupport {

    File app;
    String ext;

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();

        app = getScriptManager().app("foo").dir();
        ext = getExtension();
    }

    protected String getExtension() {
        return "js";
    }

    public void testSimple() throws Exception {
        FileUtils.copyURLToFile(
                getClass().getResource("main-helloWorld." + ext), new File(app, "main." + ext));

        MockHttpServletResponse resp = getAsServletResponse("/script/apps/foo/main." + ext);
        assertEquals(200, resp.getStatus());
        assertEquals("Hello World!", resp.getContentAsString());
    }

    public void testSimple2() throws Exception {
        FileUtils.copyURLToFile(
                getClass().getResource("main-helloWorld." + ext), new File(app, "main." + ext));

        MockHttpServletResponse resp = getAsServletResponse("/rest/apps/foo/main." + ext);
        assertEquals(200, resp.getStatus());
        assertEquals("Hello World!", resp.getContentAsString());
    }
}
