/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.internal.iso;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.csw.records.iso.MetaDataDescriptor;
import org.geoserver.csw.store.internal.CSWInternalTestSupport;
import org.geotools.csw.CSW;
import org.geotools.csw.DC;
import org.geotools.filter.v1_1.OGC;
import org.geotools.xlink.XLINK;
import org.geotools.xsd.ows.OWS;
import org.junit.BeforeClass;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** @author Niels Charlier */
public class MDTestSupport extends CSWInternalTestSupport {

    @BeforeClass
    public static void configureXMLUnit() throws Exception {
        // init xmlunit
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("csw", CSW.NAMESPACE);
        namespaces.put("ows", OWS.NAMESPACE);
        namespaces.put("ogc", OGC.NAMESPACE);
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("xlink", XLINK.NAMESPACE);
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("xsd", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("xs", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("gmd", MetaDataDescriptor.NAMESPACE_GMD);
        namespaces.put("gco", MetaDataDescriptor.NAMESPACE_GCO);
        namespaces.put("dc", MetaDataDescriptor.NAMESPACE_GCO);
        namespaces.put("dc", DC.NAMESPACE);

        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    };

    // Lazy Loading.
    private static Validator validator;

    protected static Validator getValidator() {
        if (validator == null) {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema;
            try {
                schema =
                        factory.newSchema(
                                new StreamSource(
                                        MDTestSupport.class
                                                .getResource(
                                                        "/net/opengis/schemas/iso/19139/20070417/gmd/metadataEntity.xsd")
                                                .toString()));
            } catch (SAXException e) {
                throw new RuntimeException(e);
            }
            validator = schema.newValidator();
        }
        return validator;
    }

    protected static void validateSchema(NodeList xml) throws SAXException, IOException {

        for (int i = 0; i < xml.getLength(); i++) {
            getValidator().validate(new DOMSource(xml.item(i)));
        }
    }
}
