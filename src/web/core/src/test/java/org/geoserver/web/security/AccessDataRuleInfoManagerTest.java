/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

public class AccessDataRuleInfoManagerTest extends GeoServerWicketTestSupport {

    AccessDataRuleInfoManager ruleInfoMan;

    WorkspaceInfo ws;

    DataAccessRuleDAO dao;

    @Before
    public void setUp() {
        this.ruleInfoMan = new AccessDataRuleInfoManager();
        this.ws = getCatalog().getWorkspaceByName("cite");
        this.dao = DataAccessRuleDAO.get();
    }

    @Test
    public void testWsAccessDataRuleUpdate() throws IOException {
        Set<DataAccessRule> dataRules = ruleInfoMan.getResourceRule(ws.getName(), ws);
        assertTrue(dataRules.isEmpty() == true);
        Set<String> availableRoles = ruleInfoMan.getAvailableRoles();
        List<DataAccessRuleInfo> rulesInfo =
                ruleInfoMan.mapTo(dataRules, availableRoles, ws.getName(), null);
        rulesInfo.forEach(i -> i.setRead(true));
        Set<DataAccessRule> news =
                ruleInfoMan.mapFrom(rulesInfo, availableRoles, ws.getName(), null, false);
        ruleInfoMan.saveRules(dataRules, news);
        assertTrue(ruleInfoMan.getResourceRule(ws.getName(), ws).size() == 1);
        cleanRules(ws.getName(), ws);
    }

    @Test
    public void testLayerAccessDataRuleUpdate() throws IOException {
        LayerInfo layerInfo = getCatalog().getLayerByName("BasicPolygons");
        Set<DataAccessRule> dataRules = ruleInfoMan.getResourceRule(ws.getName(), layerInfo);
        assertTrue(dataRules.isEmpty() == true);
        Set<String> availableRoles = ruleInfoMan.getAvailableRoles();
        List<DataAccessRuleInfo> rulesInfo =
                ruleInfoMan.mapTo(dataRules, availableRoles, ws.getName(), layerInfo.getName());
        rulesInfo.forEach(i -> i.setRead(true));
        Set<DataAccessRule> news =
                ruleInfoMan.mapFrom(
                        rulesInfo, availableRoles, ws.getName(), layerInfo.getName(), false);
        ruleInfoMan.saveRules(dataRules, news);
        assertTrue(ruleInfoMan.getResourceRule(ws.getName(), layerInfo).size() == 1);
        cleanRules(ws.getName(), layerInfo);
    }

    public void cleanRules(String wsName, CatalogInfo info) throws IOException {
        ruleInfoMan.removeAllResourceRules(wsName, info);
        assertTrue(0 == ruleInfoMan.getResourceRule(wsName, info).size());
    }
}
