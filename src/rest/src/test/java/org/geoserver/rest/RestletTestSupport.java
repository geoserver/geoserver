/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import junit.framework.TestCase;
import net.sf.json.JSON;
import net.sf.json.JSONSerializer;

import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.InputRepresentation;
import org.w3c.dom.Document;

public abstract class RestletTestSupport extends TestCase {

    protected XpathEngine xp;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        xp = XMLUnit.newXpathEngine();
    }
    
    protected Request newRequestGET(String path) {
        Request request = new Request();
        request.setMethod( Method.GET );
        request.setResourceRef( "http://localhost/" + path );
        return request;
    }
    
    protected Request newRequestPOST(String path, String body, String contentType) {
        Request request = new Request();
        request.setMethod( Method.POST );
        request.setResourceRef( "http://localhost/" + path );
        request.setEntity(
            new InputRepresentation( new ByteArrayInputStream( body.getBytes() ), new MediaType( contentType ) )
        );
        return request;
    }

    protected Request newRequestPUT(String path, String body, String contentType) {
        Request request = newRequestPOST(path,body,contentType);
        request.setMethod( Method.PUT );
        return request;
    }
    
    protected Document getDOM( Response response ) throws Exception {
        return response.getEntityAsDom().getDocument();
    }
    
    protected JSON getJSON( Response response ) throws Exception {
        BufferedReader in = 
            new BufferedReader( new InputStreamReader ( response.getEntity().getStream() ) );
        
        StringBuffer json = new StringBuffer();
        String line = null;
        while( ( line = in.readLine() ) != null ) {
            json.append( line );
        }
        in.close();
        
        return JSONSerializer.toJSON( json.toString() );
    }
    
    protected void print( Document dom ) throws Exception {
        TransformerFactory txFactory = TransformerFactory.newInstance();
        try {
            txFactory.setAttribute("{http://xml.apache.org/xalan}indent-number", new Integer(2));
        } catch(Exception e) {
            // some 
        }
        
        Transformer tx = txFactory.newTransformer();
        tx.setOutputProperty(OutputKeys.METHOD,"xml");
        tx.setOutputProperty( OutputKeys.INDENT, "yes" );
          
        tx.transform( new DOMSource( dom ), new StreamResult(new OutputStreamWriter(System.out, "utf-8") ));
    }
}
