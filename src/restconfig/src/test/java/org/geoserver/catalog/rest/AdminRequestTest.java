package org.geoserver.catalog.rest;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.security.AdminRequest;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

import static org.custommonkey.xmlunit.XMLAssert.*;

public class AdminRequestTest extends CatalogRESTTestSupport {

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();

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
    }

    @Override
    protected void setUpUsers(Properties props) {
        super.setUpUsers(props);

        //add a new user with only admin privileges to a single workspace
        props.put("cite", "cite,ROLE_CITE_ADMIN");
        props.put("sf", "sf,ROLE_SF_ADMIN");
    }

    @Override
    protected void setUpLayerRoles(Properties props) {
        super.setUpLayerRoles(props);

        props.put("*.*.a", "ROLE_ADMINISTRATOR");
        props.put("cite.*.a", "ROLE_CITE_ADMIN");
        props.put("sf.*.a", "ROLE_SF_ADMIN");
    }

    @Override
    protected void tearDownInternal() throws Exception {
        AdminRequest.finish();
        super.tearDownInternal();
    }

    void loginAsCite() {
        SecurityContextHolder.setContext(new SecurityContextImpl());
        List<GrantedAuthority> l= new ArrayList<GrantedAuthority>();
        l.add(new GrantedAuthorityImpl("ROLE_CITE_ADMIN"));
        
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("cite","cite",l));
    }

    void loginAsSf() {
        SecurityContextHolder.setContext(new SecurityContextImpl());
        List<GrantedAuthority> l= new ArrayList<GrantedAuthority>();
        l.add(new GrantedAuthorityImpl("ROLE_SF_ADMIN"));
        
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("sf","sf",l));
    }

    @Override
    protected void doLogin() throws Exception {
    }

    public void testWorkspaces() throws Exception {
        assertEquals(200, getAsServletResponse("/rest/workspaces.xml").getStatusCode());
        Document dom = getAsDOM("/rest/workspaces.xml");
        assertEquals(0, dom.getElementsByTagName("workspace").getLength());
        
        super.doLogin();
        dom = getAsDOM("/rest/workspaces.xml");
        assertEquals(getCatalog().getWorkspaces().size(), 
                dom.getElementsByTagName("workspace").getLength());

        loginAsCite();
        assertEquals(200, getAsServletResponse("/rest/workspaces.xml").getStatusCode());
        dom = getAsDOM("/rest/workspaces.xml");
        assertEquals(1, dom.getElementsByTagName("workspace").getLength());
        
    }

    public void testWorkspace() throws Exception {
        assertEquals(404, getAsServletResponse("/rest/workspaces/sf.xml").getStatusCode());
        assertEquals(404, getAsServletResponse("/rest/workspaces/cite.xml").getStatusCode());

        loginAsCite();
        assertEquals(404, getAsServletResponse("/rest/workspaces/sf.xml").getStatusCode());
        assertEquals(200, getAsServletResponse("/rest/workspaces/cite.xml").getStatusCode());
    }

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
