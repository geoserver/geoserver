package org.geoserver.wms.web.data;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.vfny.geoserver.global.GeoserverDataDirectory;
import org.w3c.dom.Document;

public class StyleEditPageTest extends GeoServerWicketTestSupport {
    
    StyleInfo buildingsStyle;

    @Override
    protected void setUpInternal() throws Exception {
        login();
        buildingsStyle = getCatalog().getStyleByName(MockData.BUILDINGS.getLocalPart());
        StyleEditPage edit = new StyleEditPage(buildingsStyle);
        tester.startPage(edit);
//        org.geoserver.web.wicket.WicketHierarchyPrinter.print(tester.getLastRenderedPage(), true, false);
    }

    public void testLoad() throws Exception {
        tester.assertRenderedPage(StyleEditPage.class);
        tester.assertNoErrorMessage();
        
        tester.assertComponent("form:name", TextField.class);
        tester.assertComponent("form:SLD:editorContainer:editor", TextArea.class);
        
        tester.assertModelValue("form:name", "Buildings");

        File styleFile = GeoserverDataDirectory.findStyleFile( buildingsStyle.getFilename() );
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document d1 = db.parse( new FileInputStream(styleFile) );

        //GEOS-3257, actually drag into xml and compare with xmlunit to avoid 
        // line ending problems
        String xml = tester.getComponentFromLastRenderedPage("form:SLD").getDefaultModelObjectAsString();
        xml = xml.replaceAll("&lt;","<").replaceAll("&gt;",">").replaceAll("&quot;", "\"");
        Document d2 = db.parse( new ByteArrayInputStream(xml
            .getBytes()));

        assertXMLEqual(d1, d2);
    }
    
    public void testMissingName() throws Exception {
        FormTester form = tester.newFormTester("form");
        form.setValue("name", "");
        form.submit();
        
        tester.assertRenderedPage(StyleEditPage.class);
        tester.assertErrorMessages(new String[] {"Field 'Name' is required."});
    }
    
    public void testChangeName() throws Exception {
        FormTester form = tester.newFormTester("form");
        form.setValue("name", "BuildingsNew");
        form.submit();
        
        assertNull(getCatalog().getStyleByName("Buildings"));
        assertNotNull(getCatalog().getStyleByName("BuildingsNew"));
    }
}
