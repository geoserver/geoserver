/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import static junit.framework.Assert.assertEquals;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;

import java.util.Arrays;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.TestData;
import org.geoserver.security.AccessMode;
import org.geoserver.security.AdminRequest;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.After;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class AdminRequestTest extends CatalogRESTTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
    
        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.setName( "global" );
        lg.getLayers().add( catalog.getLayerByName( "sf:PrimitiveGeoFeature" ) );
        lg.getLayers().add( catalog.getLayerByName( "sf:AggregateGeoFeature" ) );
        lg.getStyles().add( catalog.getStyleByName( StyleInfo.DEFAULT_POINT ) );
        lg.getStyles().add( catalog.getStyleByName( StyleInfo.DEFAULT_POINT ) );
        lg.setBounds( new ReferencedEnvelope( -180,-90,180,90, CRS.decode( "EPSG:4326") ) );
        catalog.add( lg );

        lg = catalog.getFactory().createLayerGroup();
        lg.setName( "local" );
        lg.setWorkspace(catalog.getWorkspaceByName("sf"));
        lg.getLayers().add( catalog.getLayerByName( "sf:PrimitiveGeoFeature" ) );
        lg.getLayers().add( catalog.getLayerByName( "sf:AggregateGeoFeature" ) );
        lg.getStyles().add( catalog.getStyleByName( StyleInfo.DEFAULT_POINT ) );
        lg.getStyles().add( catalog.getStyleByName( StyleInfo.DEFAULT_POINT ) );
        lg.setBounds( new ReferencedEnvelope( -180,-90,180,90, CRS.decode( "EPSG:4326") ) );
        catalog.add( lg );

        Catalog cat = getCatalog();

        //add two workspace specific styles
        StyleInfo s = cat.getFactory().createStyle();
        s.setName("sf_style");
        s.setWorkspace(cat.getWorkspaceByName("sf"));
        s.setFilename("sf.sld");
        cat.add(s);

        s = cat.getFactory().createStyle();
        s.setName("cite_style");
        s.setWorkspace(cat.getWorkspaceByName("cite"));
        s.setFilename("cite.sld");
        cat.add(s);

        addUser("cite", "cite", null, Arrays.asList("ROLE_CITE_ADMIN"));
        addUser("sf", "sf", null, Arrays.asList("ROLE_SF_ADMIN"));

        addLayerAccessRule("*", "*", AccessMode.ADMIN, "ROLE_ADMINISTRATOR");
        addLayerAccessRule("cite", "*", AccessMode.ADMIN, "ROLE_CITE_ADMIN");
        addLayerAccessRule("sf", "*", AccessMode.ADMIN, "ROLE_SF_ADMIN");
    }

    @After
    public void clearAdminRequest() {
        AdminRequest.finish();
    }

    @Override
    public void login() throws Exception {
        //skip the login by default
    }

    void loginAsCite() {
        login("cite", "cite","ROLE_CITE_ADMIN");
    }

    void loginAsSf() {
        login("sf", "sf","ROLE_SF_ADMIN");
    }

    @Test
    public void testWorkspaces() throws Exception {
        assertEquals(200, getAsServletResponse("/rest/workspaces.xml").getStatusCode());
        Document dom = getAsDOM("/rest/workspaces.xml");
        assertEquals(0, dom.getElementsByTagName("workspace").getLength());
        
        super.login();
        dom = getAsDOM("/rest/workspaces.xml");
        assertEquals(getCatalog().getWorkspaces().size(), 
                dom.getElementsByTagName("workspace").getLength());

        loginAsCite();
        assertEquals(200, getAsServletResponse("/rest/workspaces.xml").getStatusCode());
        dom = getAsDOM("/rest/workspaces.xml");
        assertEquals(1, dom.getElementsByTagName("workspace").getLength());
        
    }

    @Test
    public void testWorkspace() throws Exception {
        assertEquals(404, getAsServletResponse("/rest/workspaces/sf.xml").getStatusCode());
        assertEquals(404, getAsServletResponse("/rest/workspaces/cite.xml").getStatusCode());

        loginAsCite();
        assertEquals(404, getAsServletResponse("/rest/workspaces/sf.xml").getStatusCode());
        assertEquals(200, getAsServletResponse("/rest/workspaces/cite.xml").getStatusCode());
    }

    @Test
    public void testGlobalLayerGroupReadOnly() throws Exception {
        loginAsSf();

        Document dom = getAsDOM( "/rest/layergroups.xml");
        assertEquals( 1, dom.getElementsByTagName( "layerGroup").getLength());
        assertXpathEvaluatesTo("global", "//layerGroup/name", dom);
        
        dom = getAsDOM( "/rest/layergroups/global.xml");
        assertEquals( "layerGroup", dom.getDocumentElement().getNodeName());

        String xml = 
            "<layerGroup>" + 
                "<styles>" +
                  "<style>polygon</style>" +
                  "<style>line</style>" +
                "</styles>" +
              "</layerGroup>";
            
        MockHttpServletResponse response = 
            putAsServletResponse("/rest/layergroups/global", xml, "text/xml" );
        assertEquals(405, response.getStatusCode());

        xml = 
            "<layerGroup>" + 
                "<name>newLayerGroup</name>" +
                "<layers>" +
                  "<layer>Ponds</layer>" +
                  "<layer>Forests</layer>" +
                "</layers>" +
                "<styles>" +
                  "<style>polygon</style>" +
                  "<style>point</style>" +
                "</styles>" +
              "</layerGroup>"; 
        response = postAsServletResponse("/rest/layergroups", xml, "text/xml" );
        assertEquals(405, response.getStatusCode());
    }

    @Test
    public void testLocalLayerGroupHidden() throws Exception {
        loginAsSf();

        Document dom = getAsDOM( "/rest/layergroups.xml");
        assertEquals( 1, dom.getElementsByTagName( "layerGroup").getLength());
        assertXpathEvaluatesTo("global", "//layerGroup/name", dom);

        MockHttpServletResponse response = getAsServletResponse("/rest/workspaces/cite/layergroups.xml");
        assertEquals(404, response.getStatusCode());

        response = getAsServletResponse("/rest/workspaces/cite/layergroups.xml");
        assertEquals(404, response.getStatusCode());


        dom = getAsDOM( "/rest/layergroups.xml");
        assertEquals( 1, dom.getElementsByTagName( "layerGroup").getLength());
        assertXpathEvaluatesTo("global", "//layerGroup/name", dom);

        dom = getAsDOM( "/rest/workspaces/sf/layergroups.xml");
        assertEquals( 1, dom.getElementsByTagName( "layerGroup").getLength());
        assertXpathEvaluatesTo("local", "//layerGroup/name", dom);

    }

    @Test
    public void testGlobalStyleReadOnly() throws Exception {
        loginAsSf();

        Document dom = getAsDOM( "/rest/styles.xml");

        assertXpathNotExists("//style/name[text() = 'sf_style']", dom);
        assertXpathNotExists("//style/name[text() = 'cite_style']", dom);
        
        dom = getAsDOM( "/rest/styles/point.xml");
        assertEquals( "style", dom.getDocumentElement().getNodeName());

        String xml = 
            "<style>" +
              "<filename>foo.sld</filename>" + 
            "</style>";
            
        MockHttpServletResponse response = 
            putAsServletResponse("/rest/styles/point", xml, "text/xml" );
        assertEquals(405, response.getStatusCode());

        xml = 
            "<style>" + 
                "<name>foo</name>" +
                "<filename>foo.sld</filename>" + 
              "</style>"; 
        response = postAsServletResponse("/rest/styles", xml, "text/xml" );
        assertEquals(405, response.getStatusCode());
    }
   
    @Test
    public void testLocalStyleHidden() throws Exception {
        loginAsCite();

        Document dom = getAsDOM( "/rest/styles.xml");
        assertXpathNotExists("//style/name[text() = 'cite_style']", dom);
        assertXpathNotExists("//style/name[text() = 'sf_style']", dom);

        MockHttpServletResponse response = getAsServletResponse("/rest/workspaces/sf/styles.xml");
        assertEquals(404, response.getStatusCode());

        loginAsSf();

        dom = getAsDOM( "/rest/styles.xml");
       
        assertXpathNotExists("//style/name[text() = 'cite_style']", dom);
        assertXpathNotExists("//style/name[text() = 'sf_style']", dom);
        
        dom = getAsDOM( "/rest/workspaces/sf/styles.xml");
        assertEquals( 1, dom.getElementsByTagName( "style").getLength());
        assertXpathEvaluatesTo("sf_style", "//style/name", dom);
    }

}
