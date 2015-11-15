/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.format;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;

import org.geoserver.rest.Foo;
import org.restlet.data.Request;
import org.restlet.resource.Representation;

import junit.framework.TestCase;

public class ReflectiveHTMLFormatTest extends TestCase {

    public void test() throws Exception {
        Request request = new Request();
        request.setResourceRef( "http://localhost/rest/foo.html");
        request.getResourceRef().setBaseRef( "http://localhost/rest");
        
        ReflectiveHTMLFormat fmt = new ReflectiveHTMLFormat(Foo.class,request,null,null);
        Representation rep = fmt.toRepresentation(new Foo("one",2,3.0));
        rep.write(System.out);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        rep.write(bos);
        String value = bos.toString();
        assertThat(value, containsString("prop2: 2"));
        assertThat(value, containsString("prop1: one"));
        assertThat(value, containsString("prop3: 3"));
    }
    
    public void testExceptionProperty() throws Exception {
        Request request = new Request();
        request.setResourceRef( "http://localhost/rest/foo.html");
        request.getResourceRef().setBaseRef( "http://localhost/rest");

        
        ReflectiveHTMLFormat fmt = new ReflectiveHTMLFormat(ExceptionBean.class,request,null,null);
        Representation rep = fmt.toRepresentation(new ExceptionBean());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        rep.write(bos);
        String value = bos.toString();
        assertThat(value, containsString("Failed to retrieve value of property Exception. Error message is: "));
    }
    
    static final class ExceptionBean {
        
        public void getException() {
            throw new RuntimeException("Busted!");
        }
    }
    
}
