/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script;

import org.geoserver.platform.GeoServerExtensions;

/**
 * Base class for classes implementing factory extension points.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class ScriptFactory {

    /** script manager, lazily loaded */
    private ScriptManager scriptMgr;

    protected ScriptFactory() {
        this(null);
    }

    protected ScriptFactory(ScriptManager scriptMgr) {
        this.scriptMgr = scriptMgr;
    }

    /*
     * method to lookup script manager lazily, we do this because this factory is created as part
     * of the SPI plugin process, which happens before spring context creation. We don't store the
     * script manager in cases where it is looked up for testing reasons in which we don't want
     * the singleton factory to hold on to old instances of the script manager.
     */
    protected ScriptManager scriptMgr() {
        if (scriptMgr != null) {
            return scriptMgr;
        }

        return GeoServerExtensions.bean(ScriptManager.class);
    }
}
