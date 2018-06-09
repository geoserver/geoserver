/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.web;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.wicket.markup.repeater.data.DataView;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.script.ScriptManager;
import org.geoserver.script.ScriptType;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

public class ScriptPageTest extends GeoServerWicketTestSupport {

    private ScriptManager scriptManager;

    @Before
    public void init() throws IOException {
        // Add a few scripts
        scriptManager = GeoServerExtensions.bean(ScriptManager.class);
        File appDir = scriptManager.script("apps/app1").dir();
        FileUtils.writeStringToFile(new File(appDir, "main.py"), "print 'foo'");
        File wpsDir = scriptManager.script("wps").dir();
        FileUtils.writeStringToFile(new File(wpsDir, "buffer.groovy"), "buffer");
        // Login and load the page
        login();
        tester.startPage(ScriptPage.class);
    }

    @Test
    public void testLoad() {
        tester.assertRenderedPage(ScriptPage.class);
        tester.assertNoErrorMessage();
        DataView dv =
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(dv.size(), 2);
        Script script = (Script) dv.getDataProvider().iterator(0, 1).next();
        assertEquals("buffer", script.getName());
        assertEquals(ScriptType.WPS, ScriptType.getByLabel(script.getType()));
    }
}
