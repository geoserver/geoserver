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

public class PostDiffTypeBindingTest extends GSSXMLTestSupport {

    public void testEncode() throws Exception {
        // build the transaction
        QName restricted = new QName(SF_NAMESPACE, "restricted");
        WfsFactory wfs = WfsFactory.eINSTANCE;
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        
        InsertElementType insert = wfs.createInsertElementType();
        SimpleFeatureType ft = DataUtilities.createType("restricted", "cat:java.lang.Long,the_geom:Polygon");
        Geometry polygon = new WKTReader().read("POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))");
        SimpleFeature f = SimpleFeatureBuilder.build(ft, new Object[] {123, polygon}, "restricted.105");
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
        
        PostDiffType postDiff = new PostDiffType();
        postDiff.setFromVersion(15);
        postDiff.setToVersion(47);
        postDiff.setTypeName(restricted);
        postDiff.setTransaction(transaction);
        
        Document doc = encode(postDiff, GSS.PostDiff);
        // print(doc);
        
        assertXpathEvaluatesTo("15", "/gss:PostDiff/@fromVersion", doc);
        assertXpathEvaluatesTo("47", "/gss:PostDiff/@toVersion", doc);
        assertXpathEvaluatesTo("sf:restricted", "/gss:PostDiff/@typeName", doc);
        // we trust the encoding to be working, just check the transaction is actually being encoded
        assertXpathExists("/gss:PostDiff/gss:Changes", doc);
        assertXpathExists("/gss:PostDiff/gss:Changes/wfs:Insert", doc);
        assertXpathExists("/gss:PostDiff/gss:Changes/wfs:Update", doc);
        assertXpathExists("/gss:PostDiff/gss:Changes/wfs:Delete", doc);
    }
    
    public void testParse() throws Exception {
        document = dom("PostDiffRequest.xml");
        PostDiffType pd = (PostDiffType) parse(GSS.PostDiffType);
        
        assertEquals(15, pd.getFromVersion());
        assertEquals(47, pd.getToVersion());
        assertEquals(SF_NAMESPACE, pd.getTypeName().getNamespaceURI());
        assertEquals("restricted", pd.getTypeName().getLocalPart());
        assertNotNull(pd.getTransaction());
        assertEquals(1, pd.getTransaction().getDelete().size());
        assertEquals(1, pd.getTransaction().getUpdate().size());
        assertEquals(1, pd.getTransaction().getInsert().size());
        
        // print(encode(pd, GSS.PostDiff));
    }
}
