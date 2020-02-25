/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.script.ScriptManager;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.CodeMirrorEditor;
import org.junit.Before;
import org.junit.Test;

public class ScriptNewPageTest extends GeoServerWicketTestSupport {

    @Before
    public void init() {
        login();
        tester.startPage(ScriptNewPage.class);

        // print(tester.getLastRenderedPage(), true, true);
    }

    @Test
    public void testLoad() {
        tester.assertRenderedPage(ScriptNewPage.class);
        tester.assertNoErrorMessage();

        tester.assertComponent("form:name", TextField.class);
        tester.assertComponent("form:type", DropDownChoice.class);
        tester.assertComponent("form:extension", DropDownChoice.class);
        tester.assertComponent("form:contents", CodeMirrorEditor.class);
    }

    @Test
    public void testValid() throws IOException {
        FormTester form = tester.newFormTester("form");
        form.setValue("name", "hello");
        form.select("type", 0);
        ((DropDownChoice) form.getForm().get("extension"))
                .setChoices(Lists.newArrayList("py", "js", "groovy"));
        form.select("extension", 1);
        form.setValue("contents:editorContainer:editorParent:editor", "console.log('Hi');");
        form.submit();

        tester.assertRenderedPage(ScriptPage.class);
        tester.assertNoErrorMessage();

        ScriptManager scriptManager = GeoServerExtensions.bean(ScriptManager.class);
        File file = new File(new File(scriptManager.app().dir(), "hello"), "main.js");
        assertTrue(file.exists());

        assertEquals("console.log('Hi');", FileUtils.readFileToString(file));
    }

    @Test
    public void testNameRequired() {
        FormTester form = tester.newFormTester("form");
        form.select("type", 0);
        ((DropDownChoice) form.getForm().get("extension"))
                .setChoices(Lists.newArrayList("py", "js", "groovy"));
        form.select("extension", 0);
        form.setValue("contents:editorContainer:editorParent:editor", "console.log('Hi');");
        form.submit();

        tester.assertRenderedPage(ScriptNewPage.class);
        tester.assertErrorMessages(new String[] {"Field 'Name' is required."});
    }

    @Test
    public void testTypeRequired() {
        FormTester form = tester.newFormTester("form");
        form.setValue("name", "hello");
        ((DropDownChoice) form.getForm().get("extension"))
                .setChoices(Lists.newArrayList("py", "js", "groovy"));
        form.select("extension", 0);
        form.setValue("contents:editorContainer:editorParent:editor", "console.log('Hi');");
        form.submit();

        tester.assertRenderedPage(ScriptNewPage.class);
        tester.assertErrorMessages(new String[] {"Field 'type' is required."});
    }

    @Test
    public void testExtensionRequired() {
        FormTester form = tester.newFormTester("form");
        form.setValue("name", "hello");
        form.select("type", 0);
        form.setValue("contents:editorContainer:editorParent:editor", "console.log('Hi');");
        form.submit();

        tester.assertRenderedPage(ScriptNewPage.class);
        tester.assertErrorMessages(new String[] {"Field 'extension' is required."});
    }

    @Test
    public void testContentsRequired() {
        FormTester form = tester.newFormTester("form");
        form.setValue("name", "hello");
        form.select("type", 0);
        ((DropDownChoice) form.getForm().get("extension"))
                .setChoices(Lists.newArrayList("py", "js", "groovy"));
        form.select("extension", 0);
        form.submit();

        tester.assertRenderedPage(ScriptNewPage.class);
        tester.assertErrorMessages(new String[] {"Field 'contents' is required."});
    }
}
