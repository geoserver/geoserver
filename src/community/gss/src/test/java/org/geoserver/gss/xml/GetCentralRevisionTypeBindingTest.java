/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.xml;

import static org.custommonkey.xmlunit.XMLAssert.*;

import javax.xml.namespace.QName;

import org.geoserver.gss.GetCentralRevisionType;
import org.w3c.dom.Document;

public class GetCentralRevisionTypeBindingTest extends GSSXMLTestSupport {

    public void testParseOne() throws Exception {
        document = dom("GetCentralRevisionRequest.xml");
        GetCentralRevisionType gcr = (GetCentralRevisionType) parse(GSS.GetCentralRevisionType);
        
        assertEquals(1, gcr.getTypeNames().size());
        assertEquals("archsites", gcr.getTypeNames().get(0).getLocalPart());
        assertEquals(SF_NAMESPACE, gcr.getTypeNames().get(0).getNamespaceURI());
        assertEquals("GSS", gcr.getService());
        assertEquals("1.0.0", gcr.getVersion());
    }
    
    public void testParseTwo() throws Exception {
        document = dom("GetCentralRevisionRequestTwoTypes.xml");
        GetCentralRevisionType gcr = (GetCentralRevisionType) parse(GSS.GetCentralRevisionType);
        
        assertEquals(2, gcr.getTypeNames().size());
        assertEquals("archsites", gcr.getTypeNames().get(0).getLocalPart());
        assertEquals(SF_NAMESPACE, gcr.getTypeNames().get(0).getNamespaceURI());
        assertEquals("restricted", gcr.getTypeNames().get(1).getLocalPart());
        assertEquals(SF_NAMESPACE, gcr.getTypeNames().get(1).getNamespaceURI());
    }
    
    public void testEncode() throws Exception {
        GetCentralRevisionType gcr = new GetCentralRevisionType();
        gcr.getTypeNames().add(new QName(SF_NAMESPACE, "archsites"));
        gcr.getTypeNames().add(new QName(SF_NAMESPACE, "restricted"));
        
        Document doc = encode(gcr, GSS.GetCentralRevision);
        // print(doc);
        assertXpathEvaluatesTo("2", "count(//gss:GetCentralRevision/gss:TypeName)", doc);
        assertXpathEvaluatesTo("sf:archsites", "//gss:GetCentralRevision/gss:TypeName[1]", doc);
        assertXpathEvaluatesTo("sf:restricted", "//gss:GetCentralRevision/gss:TypeName[2]", doc);
    }
}
