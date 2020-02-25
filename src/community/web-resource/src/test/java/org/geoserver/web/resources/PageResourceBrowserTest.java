/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.treeview.TreeNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** @author Niels Charlier */
public class PageResourceBrowserTest extends GeoServerWicketTestSupport {

    protected final String PATH_DIR = "temp/dir";
    protected final String PATH_RES = "temp/dir/something";
    protected final String PATH_RES2 = "temp/dir/somethingelse";
    protected final String DATA = "foobar";
    protected final String DATA2 = "barfoo";

    protected PageResourceBrowser resourceBrowser;

    protected FormTester formTester;

    @Before
    public void initialize() throws Exception {
        login();

        resourceBrowser = new PageResourceBrowser();

        try (OutputStream os = resourceBrowser.store().get(PATH_RES).out()) {
            os.write(DATA.getBytes());
        }

        try (OutputStream os = resourceBrowser.store().get(PATH_RES2).out()) {
            os.write(DATA.getBytes());
        }

        tester.startPage(resourceBrowser);
    }

    @After
    public void shutdown() {
        logout();
    }

    @Test
    public void testCutPaste() throws Exception {
        assertFalse(tester.getComponentFromLastRenderedPage("cut").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("paste").isEnabled());

        // select dir
        tester.assertComponent(
                "treeview:rootView:/:children:temp:children:temp/dir", Component.class);
        tester.executeAjaxEvent(
                "treeview:rootView:/:children:temp:children:temp/dir:label:selectableLabel",
                "click");
        assertTrue(tester.getComponentFromLastRenderedPage("cut").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("paste").isEnabled());

        // cut
        tester.clickLink("cut");
        assertEquals(
                PATH_DIR,
                resourceBrowser.clipBoard.getItems().iterator().next().getObject().path());
        assertFalse(resourceBrowser.clipBoard.isCopy());
        assertTrue(tester.getComponentFromLastRenderedPage("paste").isEnabled());

        // paste in new directory
        tester.clickLink("paste");
        tester.assertComponent("dialog:dialog:content:form:userPanel", PanelPaste.class);
        formTester = tester.newFormTester("dialog:dialog:content:form");
        formTester.setValue("userPanel:directory", "/temp/new_dir");
        formTester.submit("submit");
        assertNull(tester.getComponentFromLastRenderedPage("dialog:dialog:content:form:userPanel"));
        assertFalse(Resources.exists(resourceBrowser.store().get(PATH_RES)));
        assertTrue(Resources.exists(resourceBrowser.store().get("/temp/new_dir")));
        assertTrue(Resources.exists(resourceBrowser.store().get("/temp/new_dir/dir/something")));
        tester.assertContainsNot("treeview:rootView:/:children:temp:children:temp/dir");
        tester.assertComponent(
                "treeview:rootView:/:children:temp:children:temp/new_dir", Component.class);

        // is selected
        assertEquals(
                "temp/new_dir/dir",
                resourceBrowser.treeView().getSelectedNode().getObject().path());
        // clipboard empty
        assertTrue(resourceBrowser.clipBoard.getItems().isEmpty());

        // clean up
        resourceBrowser.store().get("/temp/new_dir").delete();
    }

    @Test
    public void testCopyPaste() throws Exception {
        assertFalse(tester.getComponentFromLastRenderedPage("copy").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("paste").isEnabled());

        // select dir
        tester.executeAjaxEvent(
                "treeview:rootView:/:children:temp:children:temp/dir:label:selectableLabel",
                "click");
        assertFalse(tester.getComponentFromLastRenderedPage("copy").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("paste").isEnabled());

        // select two resources
        tester.executeAjaxEvent(
                "treeview:rootView:/:children:temp:children:temp/dir:children:temp/dir/something:selectableLabel",
                "click");
        tester.getRequest().addParameter("ctrl", "true");
        tester.executeAjaxEvent(
                "treeview:rootView:/:children:temp:children:temp/dir:children:temp/dir/somethingelse:selectableLabel",
                "click");
        assertTrue(tester.getComponentFromLastRenderedPage("copy").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("paste").isEnabled());

        // copy
        tester.clickLink("copy");
        assertContainsPaths(resourceBrowser.clipBoard.getItems(), PATH_RES, PATH_RES2);
        assertTrue(resourceBrowser.clipBoard.isCopy());

        // select dir
        tester.executeAjaxEvent(
                "treeview:rootView:/:children:temp:children:temp/dir:label:selectableLabel",
                "click");
        assertTrue(tester.getComponentFromLastRenderedPage("paste").isEnabled());

        // paste in same directory
        tester.clickLink("paste");
        tester.assertComponent("dialog:dialog:content:form:userPanel", PanelPaste.class);
        formTester = tester.newFormTester("dialog:dialog:content:form");
        formTester.submit("submit");
        assertNull(tester.getComponentFromLastRenderedPage("dialog:dialog:content:form:userPanel"));

        tester.assertComponent(
                "treeview:rootView:/:children:temp:children:temp/dir:children:temp/dir/something.1",
                Component.class);
        tester.assertComponent(
                "treeview:rootView:/:children:temp:children:temp/dir:children:temp/dir/somethingelse.1",
                Component.class);

        Resource copiedResource = resourceBrowser.store().get("temp/dir/something.1");
        assertTrue(Resources.exists(copiedResource));
        try (InputStream is = copiedResource.in()) {
            assertEquals(DATA, IOUtils.toString(is));
        }

        Resource copiedResource2 = resourceBrowser.store().get("temp/dir/somethingelse.1");
        assertTrue(Resources.exists(copiedResource2));
        try (InputStream is = copiedResource.in()) {
            assertEquals(DATA, IOUtils.toString(is));
        }

        // is selected
        assertContainsPaths(
                resourceBrowser.treeView().getSelectedNodes(),
                copiedResource.path(),
                copiedResource2.path());

        // clean up
        copiedResource.delete();
        copiedResource2.delete();
    }

    protected boolean assertContainsPaths(Collection<TreeNode<Resource>> nodes, String... paths) {
        assertEquals(paths.length, nodes.size());
        Set<String> pathset = new HashSet<String>(Arrays.asList(paths));
        for (TreeNode<Resource> node : nodes) {
            assertTrue(pathset.remove(node.getObject().path()));
        }
        assertTrue(pathset.isEmpty());

        return true;
    }

    @Test
    public void testDelete() throws Exception {
        assertFalse(tester.getComponentFromLastRenderedPage("delete").isEnabled());

        // select resource
        tester.executeAjaxEvent(
                "treeview:rootView:/:children:temp:children:temp/dir:children:temp/dir/something:selectableLabel",
                "click");
        tester.getRequest().addParameter("ctrl", "true");
        tester.executeAjaxEvent(
                "treeview:rootView:/:children:temp:children:temp/dir:children:temp/dir/somethingelse:selectableLabel",
                "click");
        assertTrue(tester.getComponentFromLastRenderedPage("delete").isEnabled());

        // delete resource
        tester.clickLink("delete");
        tester.assertComponent("dialog:dialog:content:form:userPanel", Label.class);
        formTester = tester.newFormTester("dialog:dialog:content:form");
        formTester.submit("submit");
        assertNull(tester.getComponentFromLastRenderedPage("dialog:dialog:content:form:userPanel"));

        assertFalse(Resources.exists(resourceBrowser.store().get(PATH_RES)));
        assertFalse(Resources.exists(resourceBrowser.store().get(PATH_RES2)));
        tester.assertContainsNot(
                "treeview:rootView:/:children:temp:children:temp/dir:children:temp/dir/something");
    }

    @Test
    public void testRename() throws Exception {
        assertFalse(tester.getComponentFromLastRenderedPage("rename").isEnabled());

        // select resource
        tester.executeAjaxEvent(
                "treeview:rootView:/:children:temp:children:temp/dir:children:temp/dir/something:selectableLabel",
                "click");
        assertTrue(tester.getComponentFromLastRenderedPage("rename").isEnabled());

        // rename resource
        tester.clickLink("rename");
        tester.assertComponent("dialog:dialog:content:form:userPanel", PanelRename.class);
        formTester = tester.newFormTester("dialog:dialog:content:form");
        formTester.setValue("userPanel:name", "anotherthing");
        formTester.submit("submit");
        assertNull(tester.getComponentFromLastRenderedPage("dialog:dialog:content:form:userPanel"));

        assertFalse(Resources.exists(resourceBrowser.store().get(PATH_RES)));
        tester.assertContainsNot(
                "treeview:rootView:/:children:temp:children:temp/dir:children:temp/dir/something");
        tester.assertComponent(
                "treeview:rootView:/:children:temp:children:temp/dir:children:temp/dir/anotherthing",
                Component.class);

        Resource renamedResource = resourceBrowser.store().get("temp/dir/anotherthing");
        assertTrue(Resources.exists(renamedResource));
        try (InputStream is = renamedResource.in()) {
            assertEquals(DATA, IOUtils.toString(is));
        }

        // is selected
        assertEquals(
                renamedResource.path(),
                resourceBrowser.treeView().getSelectedNode().getObject().path());

        // clean up
        renamedResource.delete();
    }

    @Test
    public void testDownload() throws Exception {
        assertFalse(tester.getComponentFromLastRenderedPage("download").isEnabled());

        // select resource
        tester.executeAjaxEvent(
                "treeview:rootView:/:children:temp:children:temp/dir:children:temp/dir/something:selectableLabel",
                "click");
        assertTrue(tester.getComponentFromLastRenderedPage("download").isEnabled());

        // rename resource
        tester.clickLink("download");

        assertTrue(Arrays.equals(DATA.getBytes(), tester.getLastResponse().getBinaryContent()));
    }

    @Test
    public void testUpload() throws Exception {
        assertFalse(tester.getComponentFromLastRenderedPage("upload").isEnabled());

        // select resource
        tester.executeAjaxEvent(
                "treeview:rootView:/:children:temp:children:temp/dir:label:selectableLabel",
                "click");
        assertTrue(tester.getComponentFromLastRenderedPage("upload").isEnabled());

        // create file to upload
        TemporaryFolder folder = new TemporaryFolder();
        folder.create();
        final File file = folder.newFile("anewthing");
        try (OutputStream os = new FileOutputStream(file)) {
            os.write(DATA2.getBytes());
        }

        // upload file
        tester.clickLink("upload");
        tester.assertComponent("dialog:dialog:content:form:userPanel", PanelUpload.class);
        formTester = tester.newFormTester("dialog:dialog:content:form");
        formTester.setFile(
                "userPanel:file", new org.apache.wicket.util.file.File(file), "text/plain");
        formTester.submit("submit");
        assertNull(tester.getComponentFromLastRenderedPage("dialog:dialog:content:form:userPanel"));

        tester.assertComponent(
                "treeview:rootView:/:children:temp:children:temp/dir:children:temp/dir/anewthing",
                Component.class);

        Resource uploadedResource = resourceBrowser.store().get("temp/dir/anewthing");
        assertTrue(Resources.exists(uploadedResource));
        try (InputStream is = uploadedResource.in()) {
            assertEquals(DATA2, IOUtils.toString(is));
        }

        // is selected
        assertEquals(
                uploadedResource.path(),
                resourceBrowser.treeView().getSelectedNode().getObject().path());

        // clean up
        uploadedResource.delete();
    }

    @Test
    public void testNew() throws Exception {
        assertFalse(tester.getComponentFromLastRenderedPage("new").isEnabled());

        // select resource
        tester.executeAjaxEvent(
                "treeview:rootView:/:children:temp:children:temp/dir:label:selectableLabel",
                "click");
        assertTrue(tester.getComponentFromLastRenderedPage("new").isEnabled());

        // new file
        tester.clickLink("new");
        tester.assertComponent("dialog:dialog:content:form:userPanel", PanelEdit.class);
        formTester = tester.newFormTester("dialog:dialog:content:form");
        formTester.setValue("userPanel:resource", "/temp/dir/anewthing");
        formTester.setValue("userPanel:contents", DATA2);
        formTester.submit("submit");
        assertNull(tester.getComponentFromLastRenderedPage("dialog:dialog:content:form:userPanel"));

        tester.assertComponent(
                "treeview:rootView:/:children:temp:children:temp/dir:children:temp/dir/anewthing",
                Component.class);

        Resource newResource = resourceBrowser.store().get("temp/dir/anewthing");
        assertTrue(Resources.exists(newResource));
        try (InputStream is = newResource.in()) {
            assertEquals(DATA2 + System.lineSeparator(), IOUtils.toString(is));
        }

        // is selected
        assertEquals(
                newResource.path(),
                resourceBrowser.treeView().getSelectedNode().getObject().path());

        // clean up
        newResource.delete();
    }

    @Test
    public void testEdit() throws Exception {
        assertFalse(tester.getComponentFromLastRenderedPage("edit").isEnabled());

        // select resource
        tester.executeAjaxEvent(
                "treeview:rootView:/:children:temp:children:temp/dir:children:temp/dir/something:selectableLabel",
                "click");
        assertTrue(tester.getComponentFromLastRenderedPage("edit").isEnabled());

        // new file
        tester.clickLink("edit");
        tester.assertComponent("dialog:dialog:content:form:userPanel", PanelEdit.class);
        formTester = tester.newFormTester("dialog:dialog:content:form");
        assertEquals(DATA, formTester.getTextComponentValue("userPanel:contents"));
        formTester.setValue("userPanel:contents", DATA2);
        formTester.submit("submit");
        assertNull(tester.getComponentFromLastRenderedPage("dialog:dialog:content:form:userPanel"));

        try (InputStream is = resourceBrowser.store().get(PATH_RES).in()) {
            assertEquals(DATA2 + System.lineSeparator(), IOUtils.toString(is));
        }
    }
}
