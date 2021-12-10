/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.junit.Test;

/** Tests expiration of contexts in the store */
public class ImporterExpiryTest extends ImporterTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // also available as a class field, but not yet initialized now
        Importer importer = (Importer) applicationContext.getBean("importer");
        ImporterInfo config = importer.getConfiguration();
        assertEquals(1440, config.getContextExpiration(), 0d);
        // lower to two seconds to allow test run in reasonable amounts of time
        config.setContextExpiration(1d / 60 * 2);
        importer.setConfiguration(config);
    }

    @Test
    public void testExpiry() throws Exception {
        File archsitesDir = unpack("shape/archsites_epsg_prj.zip");

        // A pending context. Just create it
        ImportContext pending =
                importer.createContext(new SpatialFile(new File(archsitesDir, "archsites.shp")));

        // A fake initializing context
        ImportContext init =
                importer.createContext(new SpatialFile(new File(archsitesDir, "archsites.shp")));
        init.setState(ImportContext.State.INIT);
        importer.getStore().save(init);

        // A fake failed initialization context
        ImportContext initError =
                importer.createContext(new SpatialFile(new File(archsitesDir, "archsites.shp")));
        initError.setState(ImportContext.State.INIT);
        importer.getStore().save(initError);

        // A running context... actually running would be unreliable, just mark it as such
        ImportContext running =
                importer.createContext(new SpatialFile(new File(archsitesDir, "archsites.shp")));
        running.setState(ImportContext.State.RUNNING);
        importer.getStore().save(running);

        // run a full import, a completed context
        ImportContext completed =
                importArchSites(new SpatialFile(new File(archsitesDir, "archsites.shp")));

        // sleep a bit longer than the expiry time
        Thread.sleep(3000);

        // grab the cleaner, and make it run manually
        ImporterContextCleaner cleaner = GeoServerExtensions.bean(ImporterContextCleaner.class);
        cleaner.run();

        AtomicInteger counter = new AtomicInteger(0);
        importer.getStore()
                .query(
                        ic -> {
                            // these should all be cleaned
                            assertNotEquals(ic, init);
                            assertNotEquals(ic, initError);
                            assertNotEquals(ic, pending);
                            assertNotEquals(ic, completed);

                            // however this should have been spared
                            assertEquals(ic, running);

                            counter.incrementAndGet();
                        });
        assertEquals(1, counter.get());
    }

    private ImportContext importArchSites(SpatialFile archSitesSpatialFile) throws Exception {
        ImportContext context = importer.createContext(archSitesSpatialFile);

        assertEquals(1, context.getTasks().size());

        ImportTask task = context.getTasks().get(0);
        assertEquals(ImportTask.State.READY, task.getState());
        assertEquals("archsites", task.getLayer().getResource().getName());

        importer.run(context);
        assertEquals(ImportTask.State.COMPLETE, task.getState());

        return context;
    }
}
