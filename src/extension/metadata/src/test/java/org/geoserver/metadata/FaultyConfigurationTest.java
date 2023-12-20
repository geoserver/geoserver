/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.metadata;

import org.apache.wicket.util.file.File;
import org.geoserver.metadata.data.service.impl.ConfigurationServiceImpl;
import org.geoserver.metadata.web.MetadataTemplatesPage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Tests that metadata module doesn't fail on faulty configuration file. */
public class FaultyConfigurationTest extends AbstractWicketMetadataTest {

    @Autowired protected ConfigurationServiceImpl configService;

    private File faultyConfigIgnored =
            new File(DATA_DIRECTORY.getDataDirectoryRoot(), "fouteinhoud.yaml.ignore");

    private File faultyConfig = new File(DATA_DIRECTORY.getDataDirectoryRoot(), "fouteinhoud.yaml");

    @Before
    public void initFaultyConfiguration() throws Exception {
        faultyConfigIgnored.renameTo(faultyConfig);
    }

    @After
    public void restoreConfiguration() throws Exception {
        faultyConfig.renameTo(faultyConfigIgnored);
        configService.reload();
    }

    @Test
    public void testRenderPage() {
        // just check if a page is rendered

        login();

        // Load the page
        MetadataTemplatesPage page = new MetadataTemplatesPage();
        tester.startPage(page);
        tester.assertRenderedPage(MetadataTemplatesPage.class);

        logout();
    }
}
