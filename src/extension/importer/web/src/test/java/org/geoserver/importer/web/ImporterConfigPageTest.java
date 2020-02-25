/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import org.apache.wicket.serialize.java.JavaSerializer;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.importer.Importer;
import org.geoserver.importer.ImporterInfo;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class ImporterConfigPageTest extends GeoServerWicketTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no data needed
    }

    @Test
    public void testLoadAndSave() throws IOException {
        Importer importer = getGeoServerApplication().getBeanOfType(Importer.class);
        ImporterInfo config = importer.getConfiguration();
        String temp = System.getProperty("java.io.tmpdir");
        config.setUploadRoot(temp);
        config.setMaxSynchronousImports(4);
        config.setMaxAsynchronousImports(2);
        importer.setConfiguration(config);

        // make sure it's populated correctly
        login();
        ImporterConfigPage page = tester.startPage(ImporterConfigPage.class);
        tester.assertModelValue("form:uploadRoot:border:border_body:paramValue", temp);
        tester.assertModelValue("form:maxSync", 4);
        tester.assertModelValue("form:maxAsync", 2);

        // change and save
        FormTester form = tester.newFormTester("form");
        String newUploadRoot = new File(temp, "test").getAbsolutePath();
        form.setValue("uploadRoot:border:border_body:paramValue", newUploadRoot);
        form.setValue("maxSync", "2");
        form.setValue("maxAsync", "1");
        form.submit("submit");

        ImporterInfo newConfiguration = importer.getConfiguration();
        assertEquals(newUploadRoot, newConfiguration.getUploadRoot());
        assertEquals(2, newConfiguration.getMaxSynchronousImports());
        assertEquals(1, newConfiguration.getMaxAsynchronousImports());
    }

    @Test
    public void testImporterConfigPageSerialization() {
        // acquire wicket javaSerializer
        JavaSerializer javaSeializer =
                new JavaSerializer(getGeoServerApplication().getApplicationKey());

        login();
        // int the page
        ImporterConfigPage page = tester.startPage(ImporterConfigPage.class);

        // JavaSerializer logs an exception and returns null in case there are serialization errors
        byte[] bytes = javaSeializer.serialize(page);
        assertNotNull(bytes);
    }
}
