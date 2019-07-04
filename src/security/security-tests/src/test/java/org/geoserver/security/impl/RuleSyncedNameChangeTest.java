/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.junit.Assert.assertEquals;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.AccessMode;
import org.geoserver.security.SecuredResourceNameChangeListener;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

/**
 * Test for verifying if Data Access rules change when a Workspace or Layername having a Data
 * Security Rule experience change of Name
 *
 * @author ImranR
 */
public class RuleSyncedNameChangeTest extends GeoServerSystemTestSupport {

    protected SecuredResourceNameChangeListener securedResourceNameChangeListener;

    static DataAccessRuleDAO dao;
    static Catalog catalog;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        catalog = getCatalog();
        setRules();
        dao = DataAccessRuleDAO.get();
        securedResourceNameChangeListener = new SecuredResourceNameChangeListener(catalog, dao);
    }

    @Test
    public void testChangeWorkspaceNameWithDataSecurityRule() throws Exception {

        String oldWorkspaceName = "cgf";
        String newWorkspaceName = "cgf_123";
        WorkspaceInfo wp1 = catalog.getWorkspaceByName(oldWorkspaceName);
        wp1.setName(newWorkspaceName);
        // save to invoke listner
        catalog.save(wp1);
        // should update 2 rules which were previously named cgf
        int countOfRulesUpdated = 0;
        for (DataAccessRule rule : dao.getRules()) {
            if (rule.getRoot().equalsIgnoreCase(newWorkspaceName)) countOfRulesUpdated++;
        }

        assertEquals(2, countOfRulesUpdated);
    }

    @Test
    public void testChangLayerNameWithDataSecurityRule() throws Exception {

        String oldLayerName = "Lines";
        String newLayerName = "Lines_123";

        ResourceInfo resourceInfo = catalog.getLayerByName(oldLayerName).getResource();

        resourceInfo.setName(newLayerName);
        // save to invoke listner
        catalog.save(resourceInfo);
        // should update 2 rules which were previously named wp1
        String workspaceName = resourceInfo.getStore().getWorkspace().getName();
        int countOfRulesUpdated = 0;
        for (DataAccessRule rule : dao.getRules()) {
            if (rule.getRoot().equalsIgnoreCase(workspaceName)
                    && rule.getLayer().equalsIgnoreCase(newLayerName)) countOfRulesUpdated++;
        }
        // there are two rules with same layer name but different worspace
        // only one should change
        assertEquals(1, countOfRulesUpdated);
    }

    private void setRules() throws Exception {

        addLayerAccessRule("cgf", "*", AccessMode.WRITE, "*");
        addLayerAccessRule("cgf", "Lines", AccessMode.WRITE, "*");
        addLayerAccessRule("wp2", "Lines", AccessMode.WRITE, "*");
    }
}
