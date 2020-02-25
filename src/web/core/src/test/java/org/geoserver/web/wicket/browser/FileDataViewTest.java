/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket.browser;

import static org.geoserver.web.GeoServerWicketTestSupport.initResourceSettings;
import static org.junit.Assert.*;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.junit.Before;
import org.junit.Test;

public class FileDataViewTest {

    private WicketTester tester;

    private File root;

    private File one;

    private File two;

    private File lastClicked;

    FileProvider fileProvider;

    @Before
    public void setUp() throws Exception {
        tester = new WicketTester();
        initResourceSettings(tester);

        root = new File("target/test-dataview");
        if (root.exists()) FileUtils.deleteDirectory(root);
        root.mkdirs();
        one = new File(root, "one.txt");
        one.createNewFile();
        two = new File(root, "two.sld");
        two.createNewFile();

        fileProvider = new FileProvider(root);

        tester.startPage(
                new FormTestPage(
                        new ComponentBuilder() {

                            public Component buildComponent(String id) {

                                return new FileDataView(id, fileProvider) {

                                    @Override
                                    protected void linkNameClicked(
                                            File file, AjaxRequestTarget target) {
                                        lastClicked = file;
                                    }
                                };
                            }
                        }));

        // WicketHierarchyPrinter.print(tester.getLastRenderedPage(), true, true);
    }

    @Test
    public void testLoad() throws Exception {
        tester.assertRenderedPage(FormTestPage.class);
        tester.assertNoErrorMessage();

        tester.assertLabel("form:panel:fileTable:fileContent:files:1:nameLink:name", "one.txt");
        tester.assertLabel("form:panel:fileTable:fileContent:files:2:nameLink:name", "two.sld");
        assertEquals(
                2,
                ((DataView)
                                tester.getComponentFromLastRenderedPage(
                                        "form:panel:fileTable:fileContent:files"))
                        .size());
    }

    @Test
    public void testClick() throws Exception {
        tester.clickLink("form:panel:fileTable:fileContent:files:1:nameLink");
        tester.assertRenderedPage(FormTestPage.class);
        tester.assertNoErrorMessage();
        assertEquals(one, lastClicked);
    }

    @Test
    public void testFilter() throws Exception {
        fileProvider.setFileFilter(new Model(new ExtensionFileFilter(".txt")));
        tester.startPage(tester.getLastRenderedPage());
        tester.assertLabel("form:panel:fileTable:fileContent:files:3:nameLink:name", "one.txt");
        assertEquals(
                1,
                ((DataView)
                                tester.getComponentFromLastRenderedPage(
                                        "form:panel:fileTable:fileContent:files"))
                        .size());
    }

    @Test
    public void testSortByName() throws Exception {

        // order by inverse name
        tester.clickLink("form:panel:fileTable:nameHeader:orderByLink", true);
        tester.clickLink("form:panel:fileTable:nameHeader:orderByLink", true);
        tester.assertRenderedPage(FormTestPage.class);

        tester.assertLabel("form:panel:fileTable:fileContent:files:5:nameLink:name", "two.sld");
        tester.assertLabel("form:panel:fileTable:fileContent:files:6:nameLink:name", "one.txt");
    }
}
