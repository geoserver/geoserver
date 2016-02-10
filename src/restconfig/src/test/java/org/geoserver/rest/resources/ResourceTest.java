/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.test.JSONAssert;

import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.rest.util.IOUtils;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

/**
 * 
 * @author Niels Charlier
 *
 */
public class ResourceTest extends GeoServerSystemTestSupport {
    
    private final NamespaceContext NS_XML, NS_HTML;
    private final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S z");
    private final DateFormat FORMAT_HEADER = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
    
    private Resource myRes; 
    
    public ResourceTest() {
        FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        FORMAT_HEADER.setTimeZone(TimeZone.getTimeZone("GMT"));
        Map<String, String> mapXML = new HashMap<String, String>();
        mapXML.put("atom", "http://www.w3.org/2005/Atom");
        NS_XML = new SimpleNamespaceContext(mapXML);
        Map<String, String> mapHTML = new HashMap<String, String>();        
        mapHTML.put("x", "http://www.w3.org/1999/xhtml");
        NS_HTML = new SimpleNamespaceContext(mapHTML);        
    }          
    
    @Before
    public void initialise() throws IOException {                
        myRes = getDataDirectory().get("/mydir/myres");
        try (OutputStreamWriter os = new OutputStreamWriter(myRes.out())) {
            os.append("This is my test.");
        }
        
        try (OutputStreamWriter os = new OutputStreamWriter(getDataDirectory().get("/mydir2/fake.png").out())) {
            os.append("This is not a real png file.");
        }

        IOUtils.copyStream(getClass().getResourceAsStream("testimage.png"),
                getDataDirectory().get("/mydir2/imagewithoutextension").out(), true, true);
    }
    
    @Test
    public void testResource() throws Exception {
        String str = getAsString("/rest/resource/mydir/myres");
        Assert.assertEquals("This is my test.\n", str);
    }
    
    @Test
    public void testResourceMetadataXML() throws Exception {
        XMLUnit.setXpathNamespaceContext(NS_XML);
        Document doc = getAsDOM("/rest/resource/mydir/myres?operation=metadata&format=xml");
        //print(doc);
        XMLAssert.assertXpathEvaluatesTo("myres", "/ResourceMetadata/name", doc);
        XMLAssert.assertXpathEvaluatesTo("/mydir", "/ResourceMetadata/parent/path", doc);
        XMLAssert.assertXpathEvaluatesTo("http://localhost:8080/geoserver/rest/resources/mydir?format=xml", 
                "/ResourceMetadata/parent/atom:link/@href", doc);
        XMLAssert.assertXpathEvaluatesTo(FORMAT.format(myRes.lastmodified()),
                "/ResourceMetadata/lastModified", doc);
    }
    
    @Test
    public void testResourceMetadataJSON() throws Exception {
        JSON json = getAsJSON("/rest/resource/mydir/myres?operation=metadata&format=json");
        //print(json);
        String expected = "{\"ResourceMetadata\": {"
                + "  \"name\": \"myres\","
                + "  \"parent\":   {"
                + "    \"path\": \"/mydir\","
                + "    \"link\": {"
                + "       \"href\": \"http://localhost:8080/geoserver/rest/resources/mydir?format=json\","
                + "       \"rel\": \"alternate\",                "
                + "       \"type\": \"application/json\""
                + "     }"
                + "   },"
                + "  \"lastModified\": \"" + FORMAT.format(myRes.lastmodified()) + "\","
                + "  \"type\": \"resource\""
                + "}}";
        JSONAssert.assertEquals(expected, (JSONObject) json);
    }
    
    @Test
    public void testResourceMetadataHTML() throws Exception {
        XMLUnit.setXpathNamespaceContext(NS_HTML);
        Document doc = getAsDOM("/rest/resource/mydir/myres?operation=metadata&format=html");
        print(doc);
        XMLAssert.assertXpathEvaluatesTo("Name: 'myres'", "/x:html/x:body/x:ul/x:li[1]", doc);
        XMLAssert.assertXpathEvaluatesTo("http://localhost:8080/geoserver/rest/resources/mydir?format=html", 
                "/x:html/x:body/x:ul/x:li[2]/x:a/@href", doc);
        XMLAssert.assertXpathEvaluatesTo("Type: resource", "/x:html/x:body/x:ul/x:li[3]", doc);
        XMLAssert.assertXpathEvaluatesTo("Last modified: " + new Date(myRes.lastmodified()).toString(), 
                "/x:html/x:body/x:ul/x:li[4]", doc);
    }
    
    @Test
    public void testResourceHeaders() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("/rest/resource/mydir2/fake.png");
        Assert.assertEquals(FORMAT_HEADER.format(myRes.lastmodified()), response.getHeader("Last-modified"));
        Assert.assertEquals("/mydir2", response.getHeader("Resource-parent"));
        Assert.assertEquals("resource", response.getHeader("Resource-type"));
        Assert.assertEquals("image/png", response.getHeader("Content-type"));
    }
    
    @Test
    public void testDirectoryXML() throws Exception {
        XMLUnit.setXpathNamespaceContext(NS_XML);
        Document doc = getAsDOM("/rest/resource/mydir?format=xml");
        //print(doc);
        XMLAssert.assertXpathEvaluatesTo("mydir", "/ResourceDirectory/name", doc);
        XMLAssert.assertXpathEvaluatesTo("/", "/ResourceDirectory/parent/path", doc);
        XMLAssert.assertXpathEvaluatesTo("http://localhost:8080/geoserver/rest/resources/?format=xml", 
                "/ResourceDirectory/parent/atom:link/@href", doc);
        XMLAssert.assertXpathEvaluatesTo(FORMAT.format(myRes.lastmodified()),
                "/ResourceDirectory/lastModified", doc);
        XMLAssert.assertXpathEvaluatesTo("myres", "/ResourceDirectory/children/child/name", doc);
        XMLAssert.assertXpathEvaluatesTo("http://localhost:8080/geoserver/rest/resources/mydir/myres", 
                "/ResourceDirectory/children/child/atom:link/@href", doc);
    }
    
    @Test
    public void testDirectoryJSON() throws Exception {
        JSON json = getAsJSON("/rest/resource/mydir?format=json");
        //print(json);
        String expected = "{\"ResourceDirectory\": {"
                + "\"name\": \"mydir\","
                + "\"parent\":   {"
                + "  \"path\": \"/\","
                + "    \"link\":     {"
                + "      \"href\": \"http://localhost:8080/geoserver/rest/resources/?format=json\","
                + "      \"rel\": \"alternate\","
                + "      \"type\": \"application/json\""
                + "  }"
                + "},"
                + "\"lastModified\": \"" + FORMAT.format(myRes.lastmodified()) + "\","
                + "  \"children\": {\"child\": [  {"
                + "    \"name\": \"myres\","
                + "    \"link\":     {"
                + "      \"href\": \"http://localhost:8080/geoserver/rest/resources/mydir/myres\","
                + "      \"rel\": \"alternate\","
                + "      \"type\": \"application/octet-stream\""
                + "    }"
                + "  }]}"
                + "}}";
        JSONAssert.assertEquals(expected, (JSONObject) json);
    }
    
    @Test
    public void testDirectoryHTML() throws Exception {
        XMLUnit.setXpathNamespaceContext(NS_HTML);
        Document doc = getAsDOM("/rest/resource/mydir?format=html");
        //print(doc);
        XMLAssert.assertXpathEvaluatesTo("Name: 'mydir'", "/x:html/x:body/x:ul/x:li[1]", doc);
        XMLAssert.assertXpathEvaluatesTo("http://localhost:8080/geoserver/rest/resources/?format=html", 
                "/x:html/x:body/x:ul/x:li[2]/x:a/@href", doc);
        XMLAssert.assertXpathEvaluatesTo("Last modified: " + new Date(myRes.lastmodified()).toString(), 
                "/x:html/x:body/x:ul/x:li[3]", doc);
        XMLAssert.assertXpathEvaluatesTo("http://localhost:8080/geoserver/rest/resources/mydir/myres", 
                "/x:html/x:body/x:ul/x:li[4]/x:ul/x:li/x:a/@href", doc);
    }
    
    @Test
    public void testDirectoryHeaders() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("/rest/resource/mydir?format=xml");
        Assert.assertEquals(FORMAT_HEADER.format(myRes.lastmodified()), response.getHeader("Last-modified"));
        Assert.assertEquals("/", response.getHeader("Resource-parent"));
        Assert.assertEquals("directory", response.getHeader("Resource-type"));
        Assert.assertEquals("application/xml", response.getHeader("Content-type"));
    }
    
    @Test
    public void testDirectoryMimeTypes() throws Exception {
        XMLUnit.setXpathNamespaceContext(NS_XML);
        Document doc = getAsDOM("/rest/resource/mydir2?format=xml");
        //print(doc);
        XMLAssert.assertXpathEvaluatesTo("image/png", "/ResourceDirectory/children/child[name='imagewithoutextension']/atom:link/@type", doc);
        XMLAssert.assertXpathEvaluatesTo("image/png", "/ResourceDirectory/children/child[name='fake.png']/atom:link/@type", doc);
    }
        
    @Test
    public void testUpload() throws Exception {
        put("/rest/resource/mydir/mynewres", "This is my new test.");
        
        Resource newRes = getDataDirectory().get("/mydir/mynewres");
        try (InputStream is = newRes.in()) {
            Assert.assertEquals("This is my new test.", IOUtils.toString(is));
        }
        
        newRes.delete();
    }
    
    @Test
    public void testCopy() throws Exception {
        put("/rest/resource/mydir/mynewres?operation=copy", "/mydir/myres");
        
        Resource newRes = getDataDirectory().get("/mydir/mynewres");
        Assert.assertTrue(Resources.exists(myRes));
        Assert.assertTrue(Resources.exists(newRes));    
        try (InputStream is = newRes.in()) {
            Assert.assertEquals("This is my test.", IOUtils.toString(is));
        }
        
        newRes.delete();
    }
    
    @Test
    public void testMove() throws Exception {
        put("/rest/resource/mydir/mynewres?operation=move", "/mydir/myres");
        
        Resource newRes = getDataDirectory().get("/mydir/mynewres");        
        Assert.assertFalse(Resources.exists(myRes));
        Assert.assertTrue(Resources.exists(newRes));        
        try (InputStream is = newRes.in()) {
            Assert.assertEquals("This is my test.", IOUtils.toString(is));
        }
        
        newRes.renameTo(myRes);
    }
    
    @Test
    public void testMoveDirectory() throws Exception {
        put("/rest/resource/mydir/mynewdir?operation=move", "/mydir");
        put("/rest/resource/mynewdir?operation=move", "/mydir");
        
        Resource newDir = getDataDirectory().get("/mynewdir");
        Assert.assertTrue(Resources.exists(newDir));      
        Assert.assertTrue(newDir.getType() == Resource.Type.DIRECTORY);    
        Assert.assertFalse(Resources.exists(myRes));      
        Assert.assertTrue(Resources.exists(getDataDirectory().get("/mynewdir/myres")));
        
        newDir.renameTo(getDataDirectory().get("/mydir"));
    }
    
    @Test
    public void testDelete() throws Exception {
        Resource newRes = getDataDirectory().get("/mydir/mynewres"); 
        Resources.copy(myRes, newRes);
        Assert.assertTrue(Resources.exists(newRes));
        
        deleteAsServletResponse("/rest/resource/mydir/mynewres");
        
        Assert.assertFalse(Resources.exists(newRes));
    }
    
    @Test
    public void testErrorResponseCodes() throws Exception {
        MockHttpServletResponse response;
                
        //get resource that doesn't exist
        response = getAsServletResponse("/rest/resource/doesntexist");
        Assert.assertEquals(404, response.getStatusCode());
        
        //delete resource that doesn't exist
        response = deleteAsServletResponse("/rest/resource/doesntexist");
        Assert.assertEquals(404, response.getStatusCode());
        
        //upload to dir
        response = putAsServletResponse("/rest/resource/mydir");
        Assert.assertEquals(405, response.getStatusCode());

        //copy dir
        response = putAsServletResponse("/rest/resource/mynewdir?operation=copy", "/rest/resource/mydir", "text/plain");
        Assert.assertEquals(405, response.getStatusCode());

        //copy resource that doesn't exist
        response = putAsServletResponse("/rest/resource/mynewres?operation=copy", "/doesntexist", "text/plain");
        Assert.assertEquals(404, response.getStatusCode());

        //move resource that doesn't exist
        response = putAsServletResponse("/rest/resource/mynewres?operation=move", "/doesntexist", "text/plain");
        Assert.assertEquals(404, response.getStatusCode());
        
        //post
        response = postAsServletResponse("/rest/resource/mydir", "blabla");
        Assert.assertEquals(405, response.getStatusCode());
        
    }

}
