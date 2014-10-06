/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.xml;

import static org.custommonkey.xmlunit.XMLAssert.*;

import java.util.Collections;

import javax.xml.namespace.QName;

import net.opengis.wfs.DeleteElementType;
import net.opengis.wfs.InsertElementType;
import net.opengis.wfs.PropertyType;
import net.opengis.wfs.TransactionType;
import net.opengis.wfs.UpdateElementType;
import net.opengis.wfs.WfsFactory;

import org.geoserver.gss.GetDiffResponseType;
import org.geoserver.gss.PostDiffType;
import org.geotools.data.DataUtilities;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory;
import org.w3c.dom.Document;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;

public class GetDiffResponseTypeBindingTest extends GSSXMLTestSupport {

    public void testEncode() throws Exception {
        // build the transaction
        QName restricted = new QName(SF_NAMESPACE, "restricted");
        WfsFactory wfs = WfsFactory.eINSTANCE;
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

        InsertElementType insert = wfs.createInsertElementType();
        SimpleFeatureType ft = DataUtilities.createType("restricted",
                "cat:java.lang.Long,the_geom:Polygon");
        Geometry polygon = new WKTReader().read("POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))");
        SimpleFeature f = SimpleFeatureBuilder.build(ft, new Object[] { 123, polygon },
                "restricted.105");
        insert.getFeature().add(f);

        DeleteElementType delete = wfs.createDeleteElementType();
        delete.setTypeName(restricted);
        delete.setFilter(ff.id(Collections.singleton(ff.featureId("restricted.23"))));

        UpdateElementType update = wfs.createUpdateElementType();
        update.setTypeName(restricted);
        update.setFilter(ff.id(Collections.singleton(ff.featureId("restricted.21"))));
        PropertyType property = wfs.createPropertyType();
        property.setName(new QName(SF_NAMESPACE, "cat"));
        property.setValue(-48);
        update.getProperty().add(property);

        TransactionType transaction = wfs.createTransactionType();
        transaction.getInsert().add(insert);
        transaction.getUpdate().add(update);
        transaction.getDelete().add(delete);

        GetDiffResponseType response = new GetDiffResponseType();
        response.setFromVersion(15);
        response.setToVersion(47);
        response.setTypeName(restricted);
        response.setTransaction(transaction);

        Document doc = encode(response, GSS.GetDiffResponse);
        // print(doc);

        assertXpathEvaluatesTo("15", "/gss:GetDiffResponse/@fromVersion", doc);
        assertXpathEvaluatesTo("47", "/gss:GetDiffResponse/@toVersion", doc);
        assertXpathEvaluatesTo("sf:restricted", "/gss:GetDiffResponse/@typeName", doc);
        // we trust the encoding to be working, just check the transaction is actually being encoded
        assertXpathExists("/gss:GetDiffResponse/gss:Changes", doc);
        assertXpathExists("/gss:GetDiffResponse/gss:Changes/wfs:Insert", doc);
        assertXpathExists("/gss:GetDiffResponse/gss:Changes/wfs:Update", doc);
        assertXpathExists("/gss:GetDiffResponse/gss:Changes/wfs:Delete", doc);
    }

    public void testParse() throws Exception {
        document = dom("GetDiffResponse.xml");
        GetDiffResponseType gdr = (GetDiffResponseType) parse(GSS.GetDiffResponseType);

        assertEquals(15, gdr.getFromVersion());
        assertEquals(47, gdr.getToVersion());
        assertEquals(SF_NAMESPACE, gdr.getTypeName().getNamespaceURI());
        assertEquals("restricted", gdr.getTypeName().getLocalPart());
        assertNotNull(gdr.getTransaction());
        assertEquals(1, gdr.getTransaction().getDelete().size());
        assertEquals(1, gdr.getTransaction().getUpdate().size());
        assertEquals(1, gdr.getTransaction().getInsert().size());
    }
}
