/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.web;

import java.io.File;
import java.io.InputStream;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.importer.Archive;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;
import org.geoserver.util.IOUtils;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** @author Kevin Smith, Boundless */
public class ImportTaskTableTest extends GeoServerWicketTestSupport {
    private ImportData data;
    private ImportContext context;
    private GeoServerDataProvider<ImportTask> provider;
    private FeedbackPanel feedback;

    @Rule public TemporaryFolder temp = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        // Create a test file.
        File file = temp.newFile("twoShapefilesNoPrj.zip");
        try (InputStream rin =
                ImportTaskTableTest.class.getResourceAsStream("twoShapefilesNoPrj.zip"); ) {
            IOUtils.copy(rin, file);
        }

        feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);

        data = new Archive(file);

        context = ImporterWebUtils.importer().createContext(data);

        provider = new ImportTaskProvider(context);

        ImportTaskTable table = new ImportTaskTable("taskTable", provider, true);
        table.setFeedbackPanel(feedback);
        table.setOutputMarkupId(true);

        tester.startComponentInPage(table);
    }

    @Test
    public void testTwoCRSSetByFindThenApply() {
        tester.assertComponent("taskTable", ImportTaskTable.class);

        // Click the Find CRS button for the first layer to import
        tester.clickLink(
                "taskTable:listContainer:items:1:itemProperties:2:component:form:crs:find", true);
        // Select the first CRS
        tester.clickLink(
                "taskTable:listContainer:items:1:itemProperties:2:component:form:crs:popup:content:table:listContainer:items:1:itemProperties:0:component:link",
                true);
        // Click the Find CRS button for the second layer to import
        tester.clickLink(
                "taskTable:listContainer:items:2:itemProperties:2:component:form:crs:find", true);
        // Select the first CRS
        tester.clickLink(
                "taskTable:listContainer:items:2:itemProperties:2:component:form:crs:popup:content:table:listContainer:items:2:itemProperties:0:component:link",
                true);

        // The EPSG codes should be set
        tester.assertModelValue(
                "taskTable:listContainer:items:1:itemProperties:2:component:form:crs:srs",
                "EPSG:2000");
        tester.assertModelValue(
                "taskTable:listContainer:items:2:itemProperties:2:component:form:crs:srs",
                "EPSG:2001");

        // Check that the WKT links set
        tester.assertModelValue(
                "taskTable:listContainer:items:1:itemProperties:2:component:form:crs:wkt:wktLabel",
                "EPSG:Anguilla 1957 / British West Indies Grid");
        tester.assertModelValue(
                "taskTable:listContainer:items:2:itemProperties:2:component:form:crs:wkt:wktLabel",
                "EPSG:Antigua 1943 / British West Indies Grid");

        // Apply the first
        tester.clickLink(
                "taskTable:listContainer:items:1:itemProperties:2:component:form:apply", true);
        // The first entry should be replaced with an "Advanced" link, the numbering continues from
        // those used before so the second item is 3
        tester.assertComponent(
                "taskTable:listContainer:items:3:itemProperties:2:component",
                ImportTaskTable.AdvancedOptionPanel.class);
        // The second (4) should still be set
        tester.assertModelValue(
                "taskTable:listContainer:items:4:itemProperties:2:component:form:crs:srs",
                "EPSG:2001");
    }

    void fill(String formPath, String fieldPath, String value) {
        FormTester form = tester.newFormTester(formPath);
        form.setValue(fieldPath, value);
        tester.executeAjaxEvent(String.format("%s:%s", formPath, fieldPath), "blur");
    }

    @Test
    public void testTwoCRSSetManuallyThenApply() {
        tester.assertComponent("taskTable", ImportTaskTable.class);

        // "Type" in the EPSG codes
        fill(
                "taskTable:listContainer:items:1:itemProperties:2:component:form",
                "crs:srs",
                "EPSG:3857");
        fill(
                "taskTable:listContainer:items:2:itemProperties:2:component:form",
                "crs:srs",
                "EPSG:4326");

        // Check that the WKT links set
        tester.assertModelValue(
                "taskTable:listContainer:items:1:itemProperties:2:component:form:crs:wkt:wktLabel",
                "EPSG:WGS 84 / Pseudo-Mercator");
        tester.assertModelValue(
                "taskTable:listContainer:items:2:itemProperties:2:component:form:crs:wkt:wktLabel",
                "EPSG:WGS 84");

        // Apply the first
        tester.clickLink(
                "taskTable:listContainer:items:1:itemProperties:2:component:form:apply", true);
        // The first entry should be replaced with an "Advanced" link, the numbering continues from
        // those used before so the second item is 3
        tester.assertComponent(
                "taskTable:listContainer:items:3:itemProperties:2:component",
                ImportTaskTable.AdvancedOptionPanel.class);
        // The second (4) should still be set
        tester.assertModelValue(
                "taskTable:listContainer:items:4:itemProperties:2:component:form:crs:srs",
                "EPSG:4326");
    }
}
