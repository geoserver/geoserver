/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.xml;

import static org.custommonkey.xmlunit.XMLAssert.*;

import java.util.List;

import javax.xml.namespace.QName;

import org.geoserver.gss.CentralRevisionsType;
import org.geoserver.gss.CentralRevisionsType.LayerRevision;
import org.w3c.dom.Document;

public class CentralRevisionsTypeBindingTest extends GSSXMLTestSupport {

    public void testParse() throws Exception {
        document = dom("CentralRevisionsResponse.xml");
        CentralRevisionsType cr = (CentralRevisionsType) parse(GSS.CentralRevisionsType);
        
        List<LayerRevision> layerRevisions = cr.getLayerRevisions();
        assertEquals(2, layerRevisions.size());
        
        LayerRevision layerRevision = layerRevisions.get(0);
        assertEquals("archsites", layerRevision.getTypeName().getLocalPart());
        assertEquals(SF_NAMESPACE, layerRevision.getTypeName().getNamespaceURI());
        assertEquals(15, layerRevision.getCentralRevision());
        
        layerRevision = layerRevisions.get(1);
        assertEquals("restricted", layerRevision.getTypeName().getLocalPart());
        assertEquals(SF_NAMESPACE, layerRevision.getTypeName().getNamespaceURI());
        assertEquals(22, layerRevision.getCentralRevision());
    }
    
    public void testEncode() throws Exception {
        CentralRevisionsType cr = new CentralRevisionsType();
        cr.getLayerRevisions().add(new LayerRevision(new QName(SF_NAMESPACE, "archsites"), 15));
        
        Document doc = encode(cr, GSS.CentralRevisions);
        // print(doc);
        assertXpathEvaluatesTo("1", "count(//gss:CentralRevisions/gss:LayerRevision)", doc);
        assertXpathEvaluatesTo("sf:archsites", "//gss:CentralRevisions/gss:LayerRevision/@typeName", doc);
        assertXpathEvaluatesTo("15", "//gss:CentralRevisions/gss:LayerRevision/@centralRevision", doc);
    }
}
