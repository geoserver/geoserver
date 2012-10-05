package org.geoserver.script;

import java.io.File;

import junit.framework.TestCase;

import org.geoserver.config.GeoServerDataDirectory;

public class ScriptTestSupport extends TestCase {

    protected ScriptManager scriptMgr;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        File dir = File.createTempFile("data", "tmp", new File("target"));
        dir.delete();
        dir.mkdirs();

        scriptMgr = new ScriptManager(new GeoServerDataDirectory(dir));
    }

}
