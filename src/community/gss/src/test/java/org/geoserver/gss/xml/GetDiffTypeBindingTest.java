/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.xml;

import static org.custommonkey.xmlunit.XMLAssert.*;

import javax.xml.namespace.QName;

import org.geoserver.gss.GetDiffType;
import org.w3c.dom.Document;

public class GetDiffTypeBindingTest extends GSSXMLTestSupport {

    public void testParse() throws Exception {
        document = dom("GetDiffRequest.xml");
        GetDiffType gd = (GetDiffType) parse(GSS.GetDiffType);

        assertEquals("GSS", gd.getService());
        assertEquals("1.0.0", gd.getVersion());
        assertEquals("http://www.openplans.org/spearfish", gd.getTypeName().getNamespaceURI());
        assertEquals("restricted", gd.getTypeName().getLocalPart());
    }

    public void testEncode() throws Exception {
        GetDiffType gd = new GetDiffType();
        gd.setTypeName(new QName(SF_NAMESPACE, "restricted"));
        gd.setFromVersion(15);

        Document doc = encode(gd, GSS.GetDiff);
        print(doc);
        assertXpathEvaluatesTo("GSS", "/gss:GetDiff/@service", doc);
        assertXpathEvaluatesTo("1.0.0", "/gss:GetDiff/@version", doc);
        assertXpathEvaluatesTo("sf:restricted", "/gss:GetDiff/@typeName", doc);
        assertXpathEvaluatesTo("15", "/gss:GetDiff/@fromVersion", doc);
    }
}
