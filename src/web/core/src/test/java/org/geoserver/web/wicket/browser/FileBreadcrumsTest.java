/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket.browser;

import static org.junit.Assert.*;

import java.io.File;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.junit.Before;
import org.junit.Test;

public class FileBreadcrumsTest {

    private WicketTester tester;
    private File root;
    private File leaf;
    private File lastClicked;

    @Before
    public void setUp() throws Exception {
        tester = new WicketTester();
        root = new File("target/test-breadcrumbs");
        leaf = new File("target/test-breadcrumbs/one/two/three");
        if (!leaf.exists()) leaf.mkdirs();
        tester.startPage(
                new FormTestPage(
                        new ComponentBuilder() {

                            public Component buildComponent(String id) {
                                return new FileBreadcrumbs(id, new Model(root), new Model(leaf)) {

                                    @Override
                                    protected void pathItemClicked(
                                            File file, AjaxRequestTarget target) {
                                        lastClicked = file;
                                        setSelection(file);
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

        tester.assertLabel("form:panel:path:0:pathItemLink:pathItem", "test-breadcrumbs/");
        tester.assertLabel("form:panel:path:1:pathItemLink:pathItem", "one/");
        tester.assertLabel("form:panel:path:2:pathItemLink:pathItem", "two/");
        tester.assertLabel("form:panel:path:3:pathItemLink:pathItem", "three/");
    }

    @Test
    public void testFollowLink() throws Exception {
        tester.clickLink("form:panel:path:1:pathItemLink");

        tester.assertRenderedPage(FormTestPage.class);
        tester.assertNoErrorMessage();

        assertEquals(new File("target/test-breadcrumbs/one"), lastClicked);

        tester.assertLabel("form:panel:path:0:pathItemLink:pathItem", "test-breadcrumbs/");
        tester.assertLabel("form:panel:path:1:pathItemLink:pathItem", "one/");
        assertEquals(
                2,
                ((ListView) tester.getComponentFromLastRenderedPage("form:panel:path"))
                        .getList()
                        .size());
    }
}
