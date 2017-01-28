/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTesterHelper;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;

import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.TestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.Before;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.w3c.dom.Document;

public class StyleEditPageTest extends GeoServerWicketTestSupport {
    
    StyleInfo buildingsStyle;
    StyleEditPage edit;

    @Before
    public void setUp() throws Exception {
        Catalog catalog = getCatalog();
        login();
        
        buildingsStyle = catalog.getStyleByName(MockData.BUILDINGS.getLocalPart());
        if(buildingsStyle == null) {
            // undo the rename performed in one of the test methods
            StyleInfo si = catalog.getStyleByName("BuildingsNew");
            if(si != null) {
                si.setName(MockData.BUILDINGS.getLocalPart());
                catalog.save(si);
            }
            buildingsStyle = catalog.getStyleByName(MockData.BUILDINGS.getLocalPart());
        }
        //Cleanup 'Deletes' layer
        LayerInfo layer0 = catalog.getLayers().get(0);
        StyleInfo defaultStyle = catalog.getStyleByName("Default");
        layer0.setDefaultStyle(defaultStyle);
        catalog.save(layer0);
        
        //Create an inaccesible layer
        DataStoreInfo  ds = catalog.getStoreByName("sf", "unstore", DataStoreInfo.class);
        if (ds == null) {
            CatalogBuilder cb = new CatalogBuilder(catalog);
            cb.setWorkspace(catalog.getWorkspaceByName("sf"));
            ds = cb.buildDataStore("unstore");
            catalog.add(ds);
            
            FeatureTypeInfo ft = catalog.getFactory().createFeatureType();
            ft.setName("unlayer");
            ft.setStore(catalog.getStoreByName("unstore", DataStoreInfo.class));
            ft.setCatalog(catalog);
            ft.setNamespace(catalog.getNamespaceByPrefix("sf"));
            ft.setSRS("EPSG:4326");
            CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:4326");
            ft.setNativeCRS(wgs84);
            ft.setLatLonBoundingBox(new ReferencedEnvelope(-110, 0, -60, 50, wgs84));
            ft.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);
            
            catalog.add(ft);
            
            LayerInfo ftl = catalog.getFactory().createLayer();
            ftl.setResource(ft);
            ftl.setDefaultStyle(getCatalog().getStyleByName("Default"));
            
            catalog.add(ftl);
        }
        
        //Create a cascaded WMS Layer
        WMSStoreInfo wms = catalog.getStoreByName("sf", "wmsstore", WMSStoreInfo.class);
        if (wms == null) {
            CatalogBuilder cb = new CatalogBuilder(catalog);
            cb.setWorkspace(catalog.getWorkspaceByName("sf"));
            wms = cb.buildWMSStore("wmsstore");
            wms.setCapabilitiesURL("http://demo.opengeo.org/geoserver/wms?");
            catalog.add(wms);
            
            WMSLayerInfo wmr = catalog.getFactory().createWMSLayer();
            wmr.setName("states");
            wmr.setNativeName("topp:states");
            wmr.setStore(catalog.getStoreByName("wmsstore", WMSStoreInfo.class));
            wmr.setCatalog(catalog);
            wmr.setNamespace(catalog.getNamespaceByPrefix("sf"));
            wmr.setSRS("EPSG:4326");
            CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:4326");
            wmr.setNativeCRS(wgs84);
            wmr.setLatLonBoundingBox(new ReferencedEnvelope(-110, 0, -60, 50, wgs84));
            wmr.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);
            
            catalog.add(wmr);
            
            LayerInfo wml = catalog.getFactory().createLayer();
            wml.setResource(wmr);
            wml.setDefaultStyle(getCatalog().getStyleByName("Default"));
            
            catalog.add(wml);
        }
        
        edit = new StyleEditPage(buildingsStyle);
        tester.startPage(edit);
    }

    @Test
    public void testLoad() throws Exception {
        tester.assertRenderedPage(StyleEditPage.class);
        tester.assertNoErrorMessage();

        tester.debugComponentTrees();
        tester.assertComponent("styleForm:context:panel:name", TextField.class);
        tester.assertComponent("styleForm:styleEditor:editorContainer:editorParent:editor", TextArea.class);
        
        tester.assertVisible("styleForm:context:panel:upload");
        
        //Load the legend
        tester.executeAjaxEvent("styleForm:context:panel:legendPanel:externalGraphicContainer:showhide:show", "click");
        
        tester.assertComponent("styleForm:context:panel:legendPanel", ExternalGraphicPanel.class);
        
        tester.assertComponent("styleForm:context:panel:legendPanel:externalGraphicContainer:list:onlineResource", TextField.class);
        tester.assertComponent("styleForm:context:panel:legendPanel:externalGraphicContainer:list:width", TextField.class);
        tester.assertComponent("styleForm:context:panel:legendPanel:externalGraphicContainer:list:height", TextField.class);
        tester.assertComponent("styleForm:context:panel:legendPanel:externalGraphicContainer:list:format", TextField.class);
        
        tester.assertModelValue("styleForm:context:panel:name", "Buildings");
        
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        assertNotNull( loader );
        
        String path = Paths.path("styles", Paths.convert(buildingsStyle.getFilename()));
        Resource styleFile = loader.get(path);
        
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document d1 = db.parse( styleFile.in() );

        //GEOS-3257, actually drag into xml and compare with xmlunit to avoid 
        // line ending problems
        String xml = tester.getComponentFromLastRenderedPage("styleForm:styleEditor").getDefaultModelObjectAsString();
        xml = xml.replaceAll("&lt;","<").replaceAll("&gt;",">").replaceAll("&quot;", "\"");
        Document d2 = db.parse( new ByteArrayInputStream(xml
            .getBytes()));

        assertXMLEqual(d1, d2);
    }
    
    @Test
    public void testLoadLegend() {
        
    }
    
    @Test
    public void testLayerAssociationsTab() {

        LayerInfo l = getCatalog().getLayers().get(0);
        assertFalse(l.getDefaultStyle() == buildingsStyle);
        tester.executeAjaxEvent("styleForm:context:tabs-container:tabs:1:link", "click");
        tester.assertComponent("styleForm:context:panel:layer.table", GeoServerTablePanel.class);
        
        //Set the form value of the checkbox to true and force an ajax form update
        FormTester form = tester.newFormTester("styleForm");
        form.setValue("context:panel:layer.table:listContainer:items:1:itemProperties:2:component:default.selected", true);
        AbstractAjaxBehavior behavior = (AbstractAjaxBehavior)WicketTesterHelper
                .findBehavior(tester.getComponentFromLastRenderedPage(
                        "styleForm:context:panel:layer.table:listContainer:items:1:itemProperties:2:component:default.selected"),
                        AjaxFormComponentUpdatingBehavior.class);
        tester.executeBehavior(behavior);
        
        l = getCatalog().getLayers().get(0);
        assertEquals(buildingsStyle, l.getDefaultStyle());
    }
    
    @Test
    public void testLayerAssociationsMissingStyle() {
        LayerInfo l = getCatalog().getLayers().get(0);
        StyleInfo s = l.getDefaultStyle();
        l.setDefaultStyle(null);
        //Save against the facade to skip validation
        getCatalog().getFacade().save(l);
        try {
            edit = new StyleEditPage(buildingsStyle);
            tester.startPage(edit);
            tester.executeAjaxEvent("styleForm:context:tabs-container:tabs:1:link", "click");
            tester.assertComponent("styleForm:context:panel:layer.table", GeoServerTablePanel.class);
        } finally {
            l.setDefaultStyle(s);
            getCatalog().save(l);
        }
    }
    
    @Test
    public void testLayerAttributesUnreachableLayer() throws Exception {
        tester.executeAjaxEvent("styleForm:context:tabs-container:tabs:3:link", "click");
        tester.executeAjaxEvent("styleForm:context:panel:changeLayer:link", "click");
        tester.assertComponent("styleForm:popup:content:layer.table", GeoServerTablePanel.class);
        tester.executeAjaxEvent("styleForm:popup:content:layer.table:navigatorBottom:navigator:last", "click");
        tester.assertLabel("styleForm:popup:content:layer.table:listContainer:items:30:itemProperties:2:component:link:layer.name", "unlayer");
        tester.executeAjaxEvent("styleForm:popup:content:layer.table:listContainer:items:30:itemProperties:2:component:link", "click");
        tester.assertContains("Failed to load attribute list, internal error is:");
    }
    
    @Test
    public void testLayerAttributesTabWMS() {
        tester.executeAjaxEvent("styleForm:context:tabs-container:tabs:3:link", "click");
        tester.executeAjaxEvent("styleForm:context:panel:changeLayer:link", "click");
        tester.assertComponent("styleForm:popup:content:layer.table", GeoServerTablePanel.class);
        
        //31 layers total, 25 layers per page; foo should not appear on page 1 or 2.
        tester.assertContainsNot("wmsstore");
        tester.executeAjaxEvent("styleForm:popup:content:layer.table:navigatorBottom:navigator:last", "click");
        tester.assertContainsNot("wmsstore");
    }
    
    @Test
    public void testMissingName() throws Exception {
        FormTester form = tester.newFormTester("styleForm");
        form.setValue("context:panel:name", "");
        form.submit();
        
        tester.assertRenderedPage(StyleEditPage.class);
        tester.assertErrorMessages(new String[] {"Field 'Name' is required."});
    }

    @Test
    public void testChangeName() throws Exception {
        FormTester form = tester.newFormTester("styleForm");
        form.setValue("context:panel:name", "BuildingsNew");
        form.submit();
        
        assertNull(getCatalog().getStyleByName("Buildings"));
        assertNotNull(getCatalog().getStyleByName("BuildingsNew"));
    }
    
    @Test
    public void testChangeNameAlreadyExists() throws Exception {
        FormTester form = tester.newFormTester("styleForm");
        form.setValue("context:panel:name", "Default");
        tester.executeAjaxEvent("submit", "click");
        
        tester.assertContains("java.lang.IllegalArgumentException: Style named &#039;Default&#039; already exists");
        tester.debugComponentTrees();
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

        // tester.debugComponentTrees();
        tester.newFormTester("styleForm").setValue("styleEditor:editorContainer:editorParent:editor", xml);

        tester.executeAjaxEvent("validate", "click");
        tester.assertNoErrorMessage();
    }
    
    @Test
    public void testValidateEntityExpansion() throws Exception {
        String xml = IOUtils.toString(TestData.class.getResource("externalEntities.sld"), "UTF-8");

        // tester.debugComponentTrees();
        tester.newFormTester("styleForm").setValue("styleEditor:editorContainer:editorParent:editor", xml);

        tester.executeAjaxEvent("validate", "click");
        List<Serializable> messages = tester.getMessages(FeedbackMessage.ERROR);
        assertEquals(1, messages.size());
        String message = messages.get(0).toString();
        assertThat(message, containsString("Entity resolution disallowed"));
        assertThat(message, containsString("/this/file/does/not/exist"));
    }
    
    @Test
    public void testGenerateTemplateFrenchLocale() throws Exception {
        final Session session = tester.getSession();
        try {
            session.clear();
            session.setLocale(Locale.FRENCH);
            
            StyleEditPage edit = new StyleEditPage(buildingsStyle);
            tester.startPage(edit);
            // print(tester.getLastRenderedPage(), true, true);
            
            // test the copy style link
            tester.newFormTester("styleForm").select("context:panel:templates", 1);
            tester.executeAjaxEvent("styleForm:context:panel:templates", "onchange");
            Component generateLink = tester.getComponentFromLastRenderedPage("styleForm:context:panel:generate");
            tester.executeAjaxEvent(generateLink, "onClick");
            // check single quote in the message has been escaped
            assertTrue(tester.getLastResponseAsString().contains("l\\'éditeur"));
        } finally {
            session.clear();
            session.setLocale(Locale.getDefault());
        }
    }
    
    @Test
    public void testCopyStyleFrenchLocale() throws Exception {
        final Session session = tester.getSession();
        try {
            session.clear();
            session.setLocale(Locale.FRENCH);
            
            StyleEditPage edit = new StyleEditPage(buildingsStyle);
            tester.startPage(edit);
            // print(tester.getLastRenderedPage(), true, true);
            
            // test the copy style link
            tester.newFormTester("styleForm").select("context:panel:existingStyles", 1);
            tester.executeAjaxEvent("styleForm:context:panel:existingStyles", "onchange");
            Component copyLink = tester.getComponentFromLastRenderedPage("styleForm:context:panel:copy");
            tester.executeAjaxEvent(copyLink, "onClick");
            // check single quote in the message has been escaped
            assertTrue(tester.getLastResponseAsString().contains("l\\'éditeur"));
        } finally {
            session.clear();
            session.setLocale(Locale.getDefault());
        }
    }

}
