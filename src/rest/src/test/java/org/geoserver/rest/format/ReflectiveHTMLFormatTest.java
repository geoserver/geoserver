/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.format;

import junit.framework.TestCase;

import org.geoserver.rest.Foo;
import org.restlet.data.Request;
import org.restlet.resource.Representation;

public class ReflectiveHTMLFormatTest extends TestCase {

    public void test() throws Exception {
        Request request = new Request();
        request.setResourceRef( "http://localhost/rest/foo.html");
        request.getResourceRef().setBaseRef( "http://localhost/rest");
        
        ReflectiveHTMLFormat fmt = new ReflectiveHTMLFormat(Foo.class,request,null,null);
        Representation rep = fmt.toRepresentation(new Foo("one",2,3.0));
        rep.write(System.out);
    }
    
}
