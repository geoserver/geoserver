/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.geoserver.catalog.DimensionDefaultValueSetting;
import org.geoserver.catalog.DimensionDefaultValueSetting.Strategy;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

public class DimensionInfoSerializationTest extends WMSTestSupport {

    @Test
    public void testMinStrategyXMLSerialization() throws Exception {
        assertBackAndForthSerialization(Strategy.MINIMUM);
    }

    @Test
    public void testMaxStrategyXMLSerialization() throws Exception {
        assertBackAndForthSerialization(Strategy.MAXIMUM);
    }

    @Test
    public void testFixedStrategyXMLSerialization() throws Exception {
        assertBackAndForthSerialization(Strategy.FIXED);
        assertBackAndForthSerialization(Strategy.FIXED, "2014-01-24T13:25:00.000Z");
    }

    @Test
    public void testNearestStrategyXMLSerialization() throws Exception {
        assertBackAndForthSerialization(Strategy.NEAREST);
        assertBackAndForthSerialization(Strategy.NEAREST, "2014-01-24T13:25:00.000Z");
    }

    protected void assertBackAndForthSerialization(Strategy used) throws Exception {
        assertBackAndForthSerialization(used, null);
    }

    protected void assertBackAndForthSerialization(Strategy used, String referenceValue)
            throws Exception {
        DimensionDefaultValueSetting setting = new DimensionDefaultValueSetting();
        setting.setStrategyType(used);
        if (referenceValue != null) {
            setting.setReferenceValue(referenceValue);
        }
        DimensionInfo di = new DimensionInfoImpl();
        di.setDefaultValue(setting);
        Document diDOM = marshallToXML(di);
        assertXpathExists("//defaultValue/strategy", diDOM);
        assertXpathEvaluatesTo(used.name(), "//defaultValue/strategy", diDOM);
        if (referenceValue != null) {
            assertXpathExists("//defaultValue/referenceValue", diDOM);
            assertXpathEvaluatesTo(referenceValue, "//defaultValue/referenceValue", diDOM);
        }

        DimensionInfo di2 = unmarshallFromXML(diDOM);
        assertTrue(
                "Unmarshalled strategy does not match the original one",
                di2.getDefaultValue().getStrategyType() == used);

        if (referenceValue != null) {
            assertTrue(
                    "Unmarshalled referenceValue does not match the original one",
                    di2.getDefaultValue().getReferenceValue().equals(referenceValue));
        }
    }

    protected Document marshallToXML(DimensionInfo di) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XStreamPersisterFactory xpf = GeoServerExtensions.bean(XStreamPersisterFactory.class);
        XStreamPersister persister = xpf.createXMLPersister();
        persister.save(di, baos);
        baos.flush();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(baos.toByteArray()));
    }

    protected DimensionInfo unmarshallFromXML(Document doc) throws Exception {
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(baos);
        transformer.transform(source, result);
        XStreamPersisterFactory xpf = GeoServerExtensions.bean(XStreamPersisterFactory.class);
        XStreamPersister persister = xpf.createXMLPersister();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        return persister.load(bais, DimensionInfo.class);
    }
}
