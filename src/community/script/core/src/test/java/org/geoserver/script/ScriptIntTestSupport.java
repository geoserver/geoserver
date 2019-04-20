/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script;

import static org.easymock.EasyMock.*;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.test.GeoServerTestSupport;

public abstract class ScriptIntTestSupport extends GeoServerTestSupport {

    protected ScriptManager scriptMgr;

    @Override
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();
        scriptMgr = getScriptManager();

        // mock security manager to facilitate the requred admin access
        GeoServerSecurityManager secMgr = createNiceMock(GeoServerSecurityManager.class);
        expect(secMgr.checkAuthenticationForAdminRole()).andReturn(true).anyTimes();
        replay(secMgr);
        scriptMgr.setSecurityManager(secMgr);
    }

    protected ScriptManager getScriptManager() {
        return GeoServerExtensions.bean(ScriptManager.class);
    }
}
