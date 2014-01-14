/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wfs.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.StringReader;
import java.util.Collections;

import javax.xml.namespace.QName;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;

import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wfs.WFSTestSupport;
import org.geoserver.wfs.notification.GMLNotificationSerializer;
import org.geotools.data.DataUtilities;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class WFSNotifyTest extends WFSTestSupport implements TestConstants {
    
    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        testData.setUpVectorLayer(QN_KNIGHT, Collections.EMPTY_MAP, getClass());
        testData.setUpVectorLayer(QN_BISHOP, Collections.EMPTY_MAP, getClass());
        testData.setUpVectorLayer(QN_BOARD, Collections.EMPTY_MAP, getClass());
    }

    @Test
    public void testDelete() throws Exception {
        GMLNotificationSerializer cb = (GMLNotificationSerializer) applicationContext.getBean("publishCallback", GMLNotificationSerializer.class);

        FeatureType sft =
            DataUtilities.createType("urn:c2rpc:test", "TestType", "");

        NamespaceInfo nsi = this.getCatalog().getFactory().createNamespace();
        
        nsi.setURI(sft.getName().getNamespaceURI());
        nsi.setPrefix("c2rpc");
        this.getCatalog().add(nsi);
        
        WorkspaceInfo wsi = this.getCatalog().getFactory().createWorkspace();
        wsi.setName("c2rpc");
        this.getCatalog().add(wsi);
        
        SimpleFeatureBuilder builder =
            new SimpleFeatureBuilder((SimpleFeatureType) sft);
        Feature feature = builder.buildFeature("feature-1");
        QName typeName =
            new QName(sft.getName().getNamespaceURI(), sft.getName()
                .getLocalPart(), "c2rpc");

        String xml = cb.getDeleteRawMessage(typeName, feature.getIdentifier());
        TransformerFactory tf = TransformerFactory.newInstance();
        DOMResult result = new DOMResult();
        tf.newTransformer().transform(new StreamSource(new StringReader(xml)),
            result);
        Document doc = result.getNode().getOwnerDocument();
        if(doc == null) doc = (Document) result.getNode();
        Element docElt = doc.getDocumentElement();
        assertEquals("http://www.opengis.net/wfs", docElt.getNamespaceURI());
        assertEquals("Delete", docElt.getLocalName());
        assertNotNull(docElt.getAttribute("typeName"));
        assertEquals("c2rpc:TestType", docElt.getAttribute("typeName"));
        Node text = docElt.getFirstChild();
        assertNotNull(text);
        assertEquals(Node.TEXT_NODE, text.getNodeType());
        assertEquals(feature.getIdentifier().getID(), text.getTextContent()
            .trim());
        cb.serializeDelete(feature.getType().getName(), feature.getIdentifier());
    }
}
