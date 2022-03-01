/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.AccessMode;
import org.geoserver.security.SecuredResourceNameChangeListener;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

/** Test for verifying if SecuredResourceNameChangeListener is thread-safe. */
public class RuleSyncedNameChangeConcurrencyTest extends GeoServerSystemTestSupport {

    protected SecuredResourceNameChangeListener securedResourceNameChangeListener;

    static DataAccessRuleDAO dao;
    static Catalog catalog;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        catalog = getCatalog();
        // create RULE
        setRules();
        dao = DataAccessRuleDAO.get();
        securedResourceNameChangeListener = new SecuredResourceNameChangeListener(catalog, dao);
    }

    @Test
    public void testConcurrentChanges() throws Exception {

        String oldLayerName1 = "Lines";
        String newLayerName1 = "Lines_123";
        String oldLayerName2 = "MLines";
        String newLayerName2 = "MLines_123";

        ResourceInfo resourceInfo1 = catalog.getLayerByName(oldLayerName1).getResource();
        ResourceInfo resourceInfo2 = catalog.getLayerByName(oldLayerName2).getResource();
        resourceInfo1.setName(newLayerName1);
        resourceInfo2.setName(newLayerName2);

        ExecutorService es = Executors.newCachedThreadPool();

        es.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        // save to invoke listner
                        catalog.save(resourceInfo1);
                    }
                });
        es.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        // save to invoke listner
                        catalog.save(resourceInfo2);
                    }
                });
        es.shutdown();
        assertTrue(es.awaitTermination(1, TimeUnit.MINUTES));

        // should update 2 rules p1
        int countOfRulesUpdated = 0;
        for (DataAccessRule rule : dao.getRules()) {
            if (rule.getLayer().equalsIgnoreCase(newLayerName1)
                    || rule.getLayer().equalsIgnoreCase(newLayerName2)) countOfRulesUpdated++;
        }
        assertEquals(2, countOfRulesUpdated);
    }

    private void setRules() throws Exception {
        addLayerAccessRule("cgf", "*", AccessMode.WRITE, "*");
        addLayerAccessRule("cgf", "Lines", AccessMode.WRITE, "*");
        addLayerAccessRule("cgf", "MLines", AccessMode.WRITE, "*");
    }
}
