/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.io.FileReader;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.util.file.File;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.junit.Before;
import org.junit.Test;

public class StyleNewPageTest extends GeoServerWicketTestSupport {
    
    @Before
    public void setUp() throws Exception {
        login();
        tester.startPage(StyleNewPage.class);
        // org.geoserver.web.wicket.WicketHierarchyPrinter.print(tester.getLastRenderedPage(), true, false);
    }

    @Test
    public void testLoad() throws Exception {
        tester.assertRenderedPage(StyleNewPage.class);
        tester.assertNoErrorMessage();
        
        tester.assertComponent("form:name", TextField.class);
        tester.assertComponent("form:SLD:editorContainer:editor", TextArea.class);
        tester.assertComponent("uploadForm:filename", FileUploadField.class);
       
        tester.assertComponent("form:onlineResource", TextField.class);
        tester.assertComponent("form:width", TextField.class);
        tester.assertComponent("form:height", TextField.class);
        tester.assertComponent("form:verifyImage", GeoServerAjaxFormLink.class);
        
        tester.assertModelValue("form:name", null);
    }
    
    @Test
    public void testUpload() throws Exception {
        FormTester upload = tester.newFormTester("uploadForm");
        File styleFile = new File(new java.io.File(getClass().getResource("default_point.sld").toURI()));
        String sld = IOUtils.toString(new FileReader(styleFile)).replaceAll("\r\n", "\n").replaceAll("\r", "\n");
        
        
        upload.setFile("filename", styleFile, "application/xml");
        upload.submit();
        
        tester.assertRenderedPage(StyleNewPage.class);
        tester.assertModelValue("form:SLD", sld);
    }
    
    @Test
    public void testNoLegend() throws Exception {
        FormTester form = tester.newFormTester("form");
        File styleFile = new File(new java.io.File(getClass().getResource("default_point.sld").toURI()));
        String sld = IOUtils.toString(new FileReader(styleFile)).replaceAll("\r\n", "\n").replaceAll("\r", "\n");
        form.setValue("SLD:editorContainer:editor", sld);
        form.setValue("name", "nolegendtest");
        form.submit();
        
        tester.assertRenderedPage(StylePage.class);
        StyleInfo style = getCatalog().getStyleByName("nolegendtest");
        assertNotNull(style);
        assertNull(style.getLegend());
    }
    
    @Test
    public void testLegend() throws Exception {
        FormTester form = tester.newFormTester("form");
        File styleFile = new File(new java.io.File(getClass().getResource("default_point.sld").toURI()));
        String sld = IOUtils.toString(new FileReader(styleFile)).replaceAll("\r\n", "\n").replaceAll("\r", "\n");
        form.setValue("SLD:editorContainer:editor", sld);
        form.setValue("name", "legendtest");
        form.setValue("onlineResource", "http://myurl.com/legend.png");
        form.setValue("width", "100");
        form.setValue("height", "100");
        form.setValue("format", "image/png");
        form.submit();
        tester.assertNoErrorMessage();
        tester.assertRenderedPage(StylePage.class);
        StyleInfo style = getCatalog().getStyleByName("legendtest");
        assertNotNull(style);
        assertNotNull(style.getLegend());
    }
    
    @Test
    public void testLegendWrongValues() throws Exception{
        FormTester form = tester.newFormTester("form");
        File styleFile = new File(new java.io.File(getClass().getResource("default_point.sld").toURI()));
        String sld = IOUtils.toString(new FileReader(styleFile)).replaceAll("\r\n", "\n").replaceAll("\r", "\n");
        form.setValue("SLD:editorContainer:editor", sld);
        form.setValue("name", "legendwrongvaluestest");
        form.setValue("onlineResource", "thisisnotavalidurl");
        form.setValue("width", "-1");
        form.setValue("height", "-1");
        form.setValue("format", "image/png");        
        form.submit();
        tester.assertErrorMessages(new String[] {"'thisisnotavalidurl' is not a valid URL.",
                                                "'-1' is smaller than the minimum of 0.", 
                                                "'-1' is smaller than the minimum of 0."});       
        
    }
    
    @Test
    public void testMissingName() throws Exception {
        FormTester form = tester.newFormTester("form");
        File styleFile = new File(new java.io.File(getClass().getResource("default_point.sld").toURI()));
        String sld = IOUtils.toString(new FileReader(styleFile)).replaceAll("\r\n", "\n").replaceAll("\r", "\n");
        form.setValue("SLD:editorContainer:editor", sld);
        form.submit();
       
        
        tester.assertRenderedPage(StyleNewPage.class);
        tester.assertErrorMessages(new String[] {"Field 'Name' is required."});
    }
    
    @Test
    public void testMissingStyle() throws Exception {
        FormTester form = tester.newFormTester("form");
        form.setValue("name", "test");
        form.submit();
       
        
        tester.assertRenderedPage(StyleNewPage.class);
        tester.assertErrorMessages(new String[] {"Field 'SLD' is required."});
    }
    
    @Test
    public void testNewStyleRepeatedName() throws Exception {
        FormTester form = tester.newFormTester("form");
        File styleFile = new File(new java.io.File(getClass().getResource("default_point.sld").toURI()));
        String sld = IOUtils.toString(new FileReader(styleFile)).replaceAll("\r\n", "\n").replaceAll("\r", "\n");
        form.setValue("SLD:editorContainer:editor", sld);
        form.setValue("name", "repeatedname");
        form.submit();               
        tester.assertRenderedPage(StylePage.class);
        
        tester.startPage(StyleNewPage.class);
        form = tester.newFormTester("form");                
        form.setValue("SLD:editorContainer:editor", sld);
        form.setValue("name", "repeatedname");
        form.submit();               
        tester.assertRenderedPage(StyleNewPage.class);
        
        tester.assertErrorMessages(new String[] {"java.lang.IllegalArgumentException: Style named 'repeatedname' already exists"});
    }

    @Test
    public void testNewStyle() throws Exception {        
        FormTester form = tester.newFormTester("form");
        File styleFile = new File(new java.io.File(getClass().getResource("default_point.sld").toURI()));
        String sld = IOUtils.toString(new FileReader(styleFile)).replaceAll("\r\n", "\n").replaceAll("\r", "\n");
        form.setValue("SLD:editorContainer:editor", sld);
        form.setValue("name", "test");
        form.submit(); 
        
        tester.assertRenderedPage(StylePage.class);
        assertNotNull(getCatalog().getStyleByName("test"));
    }
    
    @Test
    public void testNewStyleNoSLD() throws Exception {
        
        FormTester form = tester.newFormTester("form");
        form.setValue("name", "test");
        form.submit();
        
        tester.assertRenderedPage(StyleNewPage.class);
        assertTrue(tester.getMessages(FeedbackMessage.ERROR).size() > 0);
    }
    
//    Cannot make this one to work, the sld text area is not filled in the test
//    and I don't understand why, in the real world it is
//    public void testValidate() throws Exception {
//        tester.clickLink("form:sld:validate", false);
//        
//        tester.assertRenderedPage(StyleNewPage.class);
//        tester.assertErrorMessages(new String[] {"Invalid style"});
//    }
    
    
}
