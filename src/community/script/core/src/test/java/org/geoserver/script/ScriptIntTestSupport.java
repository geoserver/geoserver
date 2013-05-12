/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerTestSupport;

public abstract class ScriptIntTestSupport extends GeoServerTestSupport {

    protected ScriptManager scriptMgr;

    @Override
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();
        scriptMgr = getScriptManager();
    }

    protected ScriptManager getScriptManager() {
        return GeoServerExtensions.bean(ScriptManager.class);
    }
}
