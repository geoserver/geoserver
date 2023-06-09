/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.metadata;

import org.apache.wicket.util.file.File;
import org.geoserver.metadata.web.MetadataTemplatesPage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests that metadata module works 'out of the box', without any configuration present yet.
 *
 * @author niels
 */
public class EmptyConfigurationTest extends AbstractWicketMetadataTest {

    private File metadataRenamed =
            new File(DATA_DIRECTORY.getDataDirectoryRoot(), "metadata.renamed");

    @Before
    public void initEmptyConfiguration() throws Exception {
        metadata.renameTo(metadataRenamed);
    }

    @After
    public void restoreConfiguration() throws Exception {
        metadataRenamed.renameTo(metadata);
    }

    @Test
    public void testRenderPage() {
        // just check if a page is rendered

        login();

        // Load the page
        MetadataTemplatesPage page = new MetadataTemplatesPage();
        tester.startPage(page);
        tester.assertRenderedPage(MetadataTemplatesPage.class);
    }
}
