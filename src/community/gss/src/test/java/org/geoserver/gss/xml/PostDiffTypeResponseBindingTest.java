/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.xml;

import static org.custommonkey.xmlunit.XMLAssert.*;

import org.geoserver.gss.PostDiffResponseType;
import org.w3c.dom.Document;


public class PostDiffTypeResponseBindingTest extends GSSXMLTestSupport {

    
    public void testEncode() throws Exception {
        PostDiffResponseType response = new PostDiffResponseType();
        
        Document doc = encode(response, GSS.PostDiffResponse);
        // print(doc);
        
        assertXpathEvaluatesTo("true", "/gss:PostDiffResponse/@success", doc);
    }
    
    public void testParse() throws Exception {
        document = dom("PostDiffResponse.xml");
        PostDiffResponseType pd = (PostDiffResponseType) parse(GSS.PostDiffResponse);
        // nothing else to do, the type is actually empty, just there so that we can tell apart
        // a service exception from a good result
    }

}
