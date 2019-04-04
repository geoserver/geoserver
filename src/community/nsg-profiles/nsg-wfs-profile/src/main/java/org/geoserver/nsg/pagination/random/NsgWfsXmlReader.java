/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.nsg.pagination.random;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.XmlRequestReader;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.xml.v2_0.WfsXmlReader;
import org.geotools.util.Version;
import org.geotools.wfs.v2_0.WFS;
import org.springframework.util.xml.SimpleNamespaceContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class NsgWfsXmlReader extends XmlRequestReader implements ExtensionPriority {

    // WFS 2.0 namespaces
    private static SimpleNamespaceContext WFS_20_NAMESPACES;

    static {
        Map<String, String> namespaces = new HashMap<>();
        // populate WFS 2.0 namespaces map
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xs", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("ows", "http://www.opengis.net/ows/1.1");
        namespaces.put("fes", "http://www.opengis.net/fes/2.0");
        namespaces.put("gml", "http://www.opengis.net/gml/3.2");
        namespaces.put("wfs", "http://www.opengis.net/wfs/2.0");
        namespaces.put("", "http://www.opengis.net/wfs/2.0");
        // set this as the default namespaces
        WFS_20_NAMESPACES = new SimpleNamespaceContext();
        WFS_20_NAMESPACES.setBindings(namespaces);
    }

    // XPATH used to check if index result type is being used
    private static final ThreadLocal<XPathExpression> INDEX_RESULT_TYPE_XPATH =
            ThreadLocal.withInitial(
                    () -> {
                        XPath xpath = XPathFactory.newInstance().newXPath();
                        xpath.setNamespaceContext(WFS_20_NAMESPACES);
                        try {
                            return xpath.compile("/wfs:GetFeature[@resultType='index']");
                        } catch (Exception exception) {
                            throw new RuntimeException(
                                    "Error compiling result type xpath expression.");
                        }
                    });

    // WFS 2.0 XML reader that will be used to parse WFS 2.0 GetFeature operation
    private final WfsXmlReader wfsXmlReader;

    public NsgWfsXmlReader(GeoServer geoserver) {
        super(new QName(WFS.NAMESPACE, "GetFeature"), new Version("2.0.0"), "wfs");
        wfsXmlReader = new WfsXmlReader("GetFeature", geoserver);
    }

    @Override
    public Object read(Object request, Reader reader, Map kvp) throws Exception {
        Document document = parse(reader);
        Node node = searchIndexedGetFeatureRequest(document);
        if (node == null) {
            byte[] bytes = documentToBytes(document);
            InputStreamReader input = new InputStreamReader(new ByteArrayInputStream(bytes));
            return wfsXmlReader.read(request, input, kvp);
        }
        node.getAttributes().getNamedItem("resultType").setNodeValue("results");
        byte[] bytes = documentToBytes(document);
        kvp.put("RESULT_TYPE_INDEX", true);
        kvp.put("POST_REQUEST", new String(bytes, StandardCharsets.UTF_8));
        node.getAttributes().getNamedItem("resultType").setNodeValue("hits");
        bytes = documentToBytes(document);
        InputStreamReader input = new InputStreamReader(new ByteArrayInputStream(bytes));
        return wfsXmlReader.read(request, input, kvp);
    }

    @Override
    public int getPriority() {
        return 100;
    }

    private static Node searchIndexedGetFeatureRequest(Document document) {
        XPathExpression expression = INDEX_RESULT_TYPE_XPATH.get();
        try {
            return (Node) expression.evaluate(document, XPathConstants.NODE);
        } catch (Exception exception) {
            throw new ServiceException("Error applying XPath to XML POST GetFeature request.");
        }
    }

    private static Document parse(Reader reader) {
        DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
        xmlFactory.setNamespaceAware(true);
        try {
            DocumentBuilder parser = xmlFactory.newDocumentBuilder();
            return parser.parse(new InputSource(reader));
        } catch (Exception exception) {
            throw new ServiceException("Error parsing XML POST GetFeature request.");
        }
    }

    private byte[] documentToBytes(Document document) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Source xmlSource = new DOMSource(document);
        Result outputTarget = new StreamResult(outputStream);
        try {
            TransformerFactory.newInstance().newTransformer().transform(xmlSource, outputTarget);
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return outputStream.toByteArray();
    }
}
