/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.metadata.AbstractWicketMetadataTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test bulk page.
 *
 * @author Niels Charlier
 */
public class BulkOperationsPageTest extends AbstractWicketMetadataTest {

    @Before
    public void before() throws IOException {
        login();
        tester.startPage(new MetadataBulkOperationsPage());
        tester.assertRenderedPage(MetadataBulkOperationsPage.class);
    }

    @After
    public void after() throws Exception {
        restoreLayers();
        restoreTemplates();
        logout();
    }

    @Test
    public void testFixAll() {
        tester.clickLink("fix");
        tester.assertModelValue(
                "dialog:dialog:content:form:userPanel",
                "This will update 1 layers and might take a while.");
    }

    @Test
    public void testImport() throws IOException {
        FormTester formTester = tester.newFormTester("formImport");

        File csv = File.createTempFile("import", ".csv");
        FileUtils.writeStringToFile(csv, "a;aaa\nb;bbb\n", StandardCharsets.UTF_8);
        formTester.select("geonetworkName", 0);
        formTester.setFile("importCsv", new org.apache.wicket.util.file.File(csv), "text/csv");

        formTester.submit("import");
        tester.assertModelValue(
                "dialog:dialog:content:form:userPanel",
                "This will import 2 layers and might take a while. Existing data may be overwritten/removed.");
    }

    @Test
    public void testNativeToCustom() throws IOException {
        FormTester formTester = tester.newFormTester("formCustom");

        File csv = File.createTempFile("custom", ".csv");
        FileUtils.writeStringToFile(csv, "a\nb\n", StandardCharsets.UTF_8);
        formTester.setValue("ruleList", "1,2");
        formTester.setFile("customCsv", new org.apache.wicket.util.file.File(csv), "text/csv");

        formTester.submit("custom");
        tester.assertModelValue(
                "dialog:dialog:content:form:userPanel",
                "This will process 2 layers and might take a while. Existing data may be overwritten/removed.");
    }

    @Test
    public void testClearAll() {
        FormTester formTester = tester.newFormTester("formClear");
        formTester.submit("clear");
        tester.assertModelValue(
                "dialog:dialog:content:form:userPanel",
                "This will update 1 layers and might take a while. Warning: ALL metadata will be removed and this cannot be undone!");
    }
}
