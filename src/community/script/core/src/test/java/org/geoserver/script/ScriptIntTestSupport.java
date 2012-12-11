/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerTestSupport;

public class ScriptIntTestSupport extends GeoServerTestSupport {

    protected ScriptManager getScriptManager() {
        return GeoServerExtensions.bean(ScriptManager.class);
    }

}
