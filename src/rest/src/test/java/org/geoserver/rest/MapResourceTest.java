/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import org.restlet.data.Request;
import org.restlet.data.Response;
import org.w3c.dom.Document;

public class MapResourceTest extends RestletTestSupport {

    public void testMapGET() throws Exception {
        Request request = newRequestGET( "foo.xml" );
        Response response = new Response(request);
        
        FooMapResource resource = new FooMapResource( null, request, response );
        resource.handleGet();
        
        Document dom = getDOM( response );
        assertEquals( "root", dom.getDocumentElement().getNodeName() );
        assertXpathEvaluatesTo("one", "//prop1", dom);
        assertXpathEvaluatesTo("2", "//prop2", dom);
        assertXpathEvaluatesTo("3.0", "//prop3", dom);
    }
    
    public void testObjectPOST() throws Exception {
        String xml = 
            "<org.geoserver.rest.Foo>" + 
                "<prop1>one</prop1>" + 
                "<prop2>2</prop2>" + 
                "<prop3>3.0</prop3>" + 
            "</org.geoserver.rest.Foo>";
        Request request = newRequestPOST("foo",xml,"text/xml");
        Response response = new Response(request);
        
        FooMapResource resource = new FooMapResource( null, request, response );
        resource.handlePost();
        
        assertEquals( "one", resource.posted.get("prop1") );
        assertEquals( "2", resource.posted.get("prop2") );
        assertEquals( "3.0", resource.posted.get("prop3") );
    }
    
    public void testObjectPUT() throws Exception {
        String xml = 
            "<root>" + 
                "<prop1>one</prop1>" + 
                "<prop2>2</prop2>" + 
                "<prop3>3.0</prop3>" + 
            "</root>";
        Request request = newRequestPOST("foo",xml,"text/xml");
        Response response = new Response(request);
        
        FooMapResource resource = new FooMapResource( null, request, response );
        resource.handlePut();
        
        assertEquals( "one", resource.puted.get("prop1") );
        assertEquals( "2", resource.puted.get("prop2") );
        assertEquals( "3.0", resource.puted.get("prop3") );
    }
}
