package org.geoserver.web.wicket.browser;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.Model;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.WicketHierarchyPrinter;
import org.geoserver.web.wicket.GeoServerDialog.DialogDelegate;

public class GeoServerFileChooserTest extends GeoServerWicketTestSupport {

    private File root;
    private File one;
    private File two;
    private File child;

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        
        root = new File("target/test-filechooser");
        if(root.exists())
            FileUtils.deleteDirectory(root);
        child = new File(root, "child");
        child.mkdirs();
        one = new File(child, "one.txt");
        one.createNewFile();
        two = new File(child, "two.sld");
        two.createNewFile();
    }
    
    public void setupChooser(final File file) {
        tester.startPage(new FormTestPage(new ComponentBuilder() {
            
            public Component buildComponent(String id) {
                return new GeoServerFileChooser(id, new Model(file));
            }
        }));
        
        //WicketHierarchyPrinter.print(tester.getLastRenderedPage(), true, true);
    }
    
    public void testLoad() {
        setupChooser(root);
        
        tester.assertRenderedPage(FormTestPage.class);
        tester.assertNoErrorMessage();
        
        tester.assertLabel("form:panel:fileTable:fileTable:fileContent:files:1:nameLink:name", "child/");
        assertEquals(1, ((DataView) tester.getComponentFromLastRenderedPage("form:panel:fileTable:fileTable:fileContent:files")).size());
    }
    
    public void testNullRoot() {
        setupChooser(null);

        // make sure it does not now blow out because of the null
        tester.assertRenderedPage(FormTestPage.class);
        tester.assertNoErrorMessage();
        
        tester.assertLabel("form:panel:breadcrumbs:path:0:pathItemLink:pathItem", getTestData().getDataDirectoryRoot().getName() + "/");
    }
    
    public void testInDialog() throws Exception {
        tester.startPage(new FormTestPage(new ComponentBuilder() {
            
            public Component buildComponent(String id) {
                return new GeoServerDialog(id);
            }
        }));
        
        tester.assertRenderedPage(FormTestPage.class);

        tester.debugComponentTrees();
        
        GeoServerDialog dialog = (GeoServerDialog) tester.getComponentFromLastRenderedPage("form:panel");
        assertNotNull(dialog);

        dialog.showOkCancel(new AjaxRequestTarget(tester.getLastRenderedPage()), 
            new DialogDelegate() {
                @Override
                protected Component getContents(String id) {
                    return new GeoServerFileChooser(id, new Model(root));
                }

                @Override
                protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                    assertNotNull(contents);
                    assertTrue(contents instanceof GeoServerFileChooser);
                    return true;
                }
        });
        
        dialog.submit(new AjaxRequestTarget(tester.getLastRenderedPage()));
    }
}
