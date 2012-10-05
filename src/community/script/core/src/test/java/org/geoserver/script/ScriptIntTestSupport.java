package org.geoserver.script;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerTestSupport;

public class ScriptIntTestSupport extends GeoServerTestSupport {

    protected ScriptManager getScriptManager() {
        return GeoServerExtensions.bean(ScriptManager.class);
    }

}
