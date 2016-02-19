/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import java.io.ByteArrayInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class StyleEditPageTest extends GeoServerWicketTestSupport {
    
    StyleInfo buildingsStyle;

    @Before
    public void setUp() throws Exception {
        login();
        buildingsStyle = getCatalog().getStyleByName(MockData.BUILDINGS.getLocalPart());
        if(buildingsStyle == null) {
            // undo the rename performed in one of the test methods
            StyleInfo si = getCatalog().getStyleByName("BuildingsNew");
            if(si != null) {
                si.setName(MockData.BUILDINGS.getLocalPart());
                getCatalog().save(si);
            }
            buildingsStyle = getCatalog().getStyleByName(MockData.BUILDINGS.getLocalPart());
        }
        StyleEditPage edit = new StyleEditPage(buildingsStyle);
        tester.startPage(edit);
    }

    @Test
    public void testLoad() throws Exception {
        tester.assertRenderedPage(StyleEditPage.class);
        tester.assertNoErrorMessage();

        tester.debugComponentTrees();
        tester.assertComponent("form:name", TextField.class);
        tester.assertComponent("form:styleEditor:editorContainer:editorParent:editor", TextArea.class);
        
        //Load the legend
        tester.executeAjaxEvent("form:legendPanel:container:showhide:show", "click");
        
        tester.assertComponent("form:legendPanel", ExternalGraphicPanel.class);
        
        tester.assertComponent("form:legendPanel:container:list:onlineResource", TextField.class);
        tester.assertComponent("form:legendPanel:container:list:width", TextField.class);
        tester.assertComponent("form:legendPanel:container:list:height", TextField.class);
        tester.assertComponent("form:legendPanel:container:list:format", TextField.class);
        
        tester.assertModelValue("form:name", "Buildings");
        
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        assertNotNull( loader );
        
        String path = Paths.path("styles", Paths.convert(buildingsStyle.getFilename()));
        Resource styleFile = loader.get(path);
        
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document d1 = db.parse( styleFile.in() );

        //GEOS-3257, actually drag into xml and compare with xmlunit to avoid 
        // line ending problems
        String xml = tester.getComponentFromLastRenderedPage("form:styleEditor").getDefaultModelObjectAsString();
        xml = xml.replaceAll("&lt;","<").replaceAll("&gt;",">").replaceAll("&quot;", "\"");
        Document d2 = db.parse( new ByteArrayInputStream(xml
            .getBytes()));

        assertXMLEqual(d1, d2);
    }
    
    @Test
    public void testLoadLegend() {
        
    }
    
    @Test
    public void testMissingName() throws Exception {
        FormTester form = tester.newFormTester("form");
        form.setValue("name", "");
        form.submit();
        
        tester.assertRenderedPage(StyleEditPage.class);
        tester.assertErrorMessages(new String[] {"Field 'Name' is required."});
    }

    @Test
    public void testChangeName() throws Exception {
        FormTester form = tester.newFormTester("form");
        form.setValue("name", "BuildingsNew");
        form.submit();
        
        assertNull(getCatalog().getStyleByName("Buildings"));
        assertNotNull(getCatalog().getStyleByName("BuildingsNew"));
    }

    @Test
    public void testValidate() throws Exception {
        String xml =
            "<StyledLayerDescriptor version='1.0.0' " +
                " xsi:schemaLocation='http://www.opengis.net/sld StyledLayerDescriptor.xsd' " +
                " xmlns='http://www.opengis.net/sld' " +
                " xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" +
                "  <NamedLayer>" +
                "    <Name>Style</Name>" +
                "  </NamedLayer>" +
            "</StyledLayerDescriptor>";

        tester.debugComponentTrees();
        tester.newFormTester("form").setValue("styleEditor:editorContainer:editorParent:editor", xml);

        tester.executeAjaxEvent("validate", "click");
        tester.assertNoErrorMessage();
    }
}
