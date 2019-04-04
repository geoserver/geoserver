/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script;

import java.io.File;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.geoserver.config.GeoServerDataDirectory;

public abstract class ScriptTestSupport extends TestCase {

    protected ScriptManager scriptMgr;

    private File dir;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        dir = File.createTempFile("data", "tmp", new File("target"));
        dir.delete();
        dir.mkdirs();

        scriptMgr = new ScriptManager(new GeoServerDataDirectory(dir));
    }

    @Override
    protected void tearDown() throws Exception {
        FileUtils.deleteQuietly(dir);
    }
}
