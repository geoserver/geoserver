/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.config;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import junit.framework.TestCase;

import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerResourceLoader;

public class GWCConfigPersisterTest extends TestCase {

    private GeoServerResourceLoader resourceLoader;

    private GWCConfigPersister persister;

    @Override
    protected void setUp() throws Exception {
        resourceLoader = mock(GeoServerResourceLoader.class);

        persister = new GWCConfigPersister(new XStreamPersisterFactory(), resourceLoader);
    }

    public void testPrecondition() throws Exception {
        // gwc-gs.xml shall exists, it's GWCInitializer responsibility
        when(resourceLoader.find(eq(GWCConfigPersister.GWC_CONFIG_FILE))).thenReturn(null);
        try {
            persister.getConfig();
            fail("Expected assertion error");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains(GWCConfigPersister.GWC_CONFIG_FILE));
        }
    }

    public void testSaveLoad() throws Exception {
        final File baseDirectory = new File("target");
        baseDirectory.mkdirs();
        final File configFile = new File(baseDirectory, GWCConfigPersister.GWC_CONFIG_FILE);
        if (configFile.exists()) {
            assertTrue(configFile.delete());
        }

        when(resourceLoader.getBaseDirectory()).thenReturn(baseDirectory);
        when(resourceLoader.find(eq(GWCConfigPersister.GWC_CONFIG_FILE))).thenReturn(configFile);

        GWCConfig config = GWCConfig.getOldDefaults();
        config.setCacheNonDefaultStyles(true);
        config.setDirectWMSIntegrationEnabled(true);

        persister.save(config);
        assertSame(config, persister.getConfig());

        persister = new GWCConfigPersister(new XStreamPersisterFactory(), resourceLoader);

        assertEquals(config, persister.getConfig());

        // provoque a IOException
        when(resourceLoader.find(eq(GWCConfigPersister.GWC_CONFIG_FILE))).thenReturn(
                new File("shall_not_exist"));
        persister = new GWCConfigPersister(new XStreamPersisterFactory(), resourceLoader);

        GWCConfig expected = new GWCConfig();
        GWCConfig actual = persister.getConfig();
        assertEquals(expected, actual);
    }
}
