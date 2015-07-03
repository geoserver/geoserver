/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.restlet.data.MediaType;
import org.restlet.data.Preference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.w3c.dom.Document;

public class ReflectiveResourceTest extends RestletTestSupport {

    public void testObjectGetAsXML() throws Exception {
        Request request = newRequestGET( "foo.xml" );
        Response response = new Response(request);
        
        FooReflectiveResource resource = new FooReflectiveResource( null, request, response );
        resource.handleGet();
        
        Document dom = getDOM( response );
        assertEquals( Foo.class.getName(), dom.getDocumentElement().getNodeName() );
        assertXpathEvaluatesTo("one", "//prop1", dom);
        assertXpathEvaluatesTo("2", "//prop2", dom);
        assertXpathEvaluatesTo("3.0", "//prop3", dom);
    }

    public void testObjectGetAsJSON() throws Exception {
        Request request = newRequestGET( "foo.json" );
        Response response = new Response(request);
        
        FooReflectiveResource resource = new FooReflectiveResource( null, request, response );
        resource.handleGet();
        
        JSON json = getJSON(response);
        
        JSONObject foo = ((JSONObject)json).getJSONObject(Foo.class.getName());
        assertNotNull( foo );
        
        assertEquals("one", foo.get( "prop1" ) );
        assertEquals(2, foo.get("prop2") );
        assertEquals(3.0, ((Number)foo.get("prop3")).doubleValue(), 0.1 );
    }

    public void testObjectPOSTAsXML() throws Exception {
        String xml = 
            "<org.geoserver.rest.Foo>" + 
                "<prop1>one</prop1>" + 
                "<prop2>2</prop2>" + 
                "<prop3>3.0</prop3>" + 
            "</org.geoserver.rest.Foo>";
        Request request = newRequestPOST("foo",xml,"text/xml");
        Response response = new Response(request);
        
        FooReflectiveResource resource = new FooReflectiveResource( null, request, response );
        resource.handlePost();
        
        assertEquals( "one", resource.posted.prop1 );
        assertEquals( new Integer(2), resource.posted.prop2 );
        assertEquals( new Double(3), resource.posted.prop3 );
    }
    
    public void testObjectPOSTAsJSON() throws Exception {
        String json = 
            "{'org.geoserver.rest.Foo':{" + 
                "'prop1':'one'," +
                "'prop2':2," +
                "'prop3':3" + 
             "}}";

        Request request = newRequestPOST("foo",json,"text/json");
        Response response = new Response(request);
        
        FooReflectiveResource resource = new FooReflectiveResource( null, request, response );
        resource.handlePost();
        
        assertEquals( "one", resource.posted.prop1 );
        assertEquals( new Integer(2), resource.posted.prop2 );
        assertEquals( new Double(3), resource.posted.prop3 );
    }
    
    public void testObjectPUT() throws Exception {
        String xml = 
            "<org.geoserver.rest.Foo>" + 
                "<prop1>one</prop1>" + 
                "<prop2>2</prop2>" + 
                "<prop3>3.0</prop3>" + 
            "</org.geoserver.rest.Foo>";
        Request request = newRequestPOST("foo",xml,"text/xml");
        Response response = new Response(request);
        
        FooReflectiveResource resource = new FooReflectiveResource( null, request, response );
        resource.handlePut();
        
        assertEquals( "one", resource.puted.prop1 );
        assertEquals( new Integer(2), resource.puted.prop2 );
        assertEquals( new Double(3), resource.puted.prop3 );
    }
    
    public void testGetWithAcceptsHeader() throws Exception {
        Request request = newRequestGET( "foo" );
        request.getClientInfo().getAcceptedMediaTypes().add( new Preference<MediaType>(MediaType.TEXT_XML) );
        Response response = new Response(request);
        
        FooReflectiveResource resource = new FooReflectiveResource( null, request, response );
        resource.handleGet();
        
        Document dom = getDOM( response );
        assertEquals( Foo.class.getName(), dom.getDocumentElement().getNodeName() );
        assertXpathEvaluatesTo("one", "//prop1", dom);
        assertXpathEvaluatesTo("2", "//prop2", dom);
        assertXpathEvaluatesTo("3.0", "//prop3", dom);
    }
}
