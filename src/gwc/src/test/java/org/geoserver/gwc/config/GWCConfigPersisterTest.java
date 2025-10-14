/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import org.apache.commons.io.FileUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Paths;
import org.geoserver.util.DimensionWarning.WarningType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;

public class GWCConfigPersisterTest {

    private GeoServerResourceLoader resourceLoader;

    private GWCConfigPersister persister;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        resourceLoader = mock(GeoServerResourceLoader.class);

        persister = new GWCConfigPersister(new XStreamPersisterFactory(), resourceLoader);
    }

    @Test
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

    @Test
    public void testSaveLoad() throws Exception {
        final File baseDirectory = new File("target");
        baseDirectory.mkdirs();
        final File configFile = new File(baseDirectory, GWCConfigPersister.GWC_CONFIG_FILE);
        if (configFile.exists()) {
            assertTrue(configFile.delete());
        }

        when(resourceLoader.get(Paths.BASE)).thenReturn(Files.asResource(baseDirectory));
        when(resourceLoader.get(eq(GWCConfigPersister.GWC_CONFIG_FILE))).thenReturn(Files.asResource(configFile));

        GWCConfig config = GWCConfig.getOldDefaults();
        config.setCacheNonDefaultStyles(true);
        config.setDirectWMSIntegrationEnabled(true);
        config.setMetaTilingThreads(12);
        config.setCacheWarningSkips(new LinkedHashSet<>(Arrays.asList(WarningType.Default, WarningType.FailedNearest)));

        persister.save(config);
        assertSame(config, persister.getConfig());

        persister = new GWCConfigPersister(new XStreamPersisterFactory(), resourceLoader);

        assertEquals(config, persister.getConfig());

        // provoke a IOException
        when(resourceLoader.get(eq(GWCConfigPersister.GWC_CONFIG_FILE)))
                .thenReturn(Files.asResource(tempFolder.newFile("shall_not_exist")));
        persister = new GWCConfigPersister(new XStreamPersisterFactory(), resourceLoader);

        GWCConfig expected = new GWCConfig();
        GWCConfig actual = persister.getConfig();
        assertEquals(expected, actual);

        // check the saved XML has the expected structure
        String xml = FileUtils.readFileToString(configFile, StandardCharsets.UTF_8);

        XpathEngine xpath = XMLUnit.newXpathEngine();
        Document doc = XMLUnit.buildControlDocument(xml);
        // no custom attribute for the class, we set a default
        assertEquals("", xpath.evaluate("//cacheWarningSkips/class", doc));
        assertEquals("Default", xpath.evaluate("//cacheWarningSkips/warning[1]", doc));
        assertEquals("FailedNearest", xpath.evaluate("//cacheWarningSkips/warning[2]", doc));
    }
}
