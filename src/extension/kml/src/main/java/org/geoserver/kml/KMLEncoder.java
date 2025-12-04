/* (c) 2014 - 2017 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import de.micromata.opengis.kml.v_2_2_0.Kml;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import org.geoserver.platform.ServiceException;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * Encodes a KML object onto an output stream
 *
 * @author Andrea Aime - GeoSolutions
 */
public class KMLEncoder {

    private JAXBContext context;

    public KMLEncoder() throws JAXBException, TransformerException {
        // this creation is expensive, do it once and cache it
        context = JAXBContext.newInstance((Kml.class));
    }

    public void encode(Kml kml, OutputStream output, KmlEncodingContext context) {
        try {
            if ((context != null) && (context.getWms() == null)) {
                // No need to transform WFS KML.
                createMarshaller().marshal(kml, output);
            } else {
                createMarshaller().marshal(kml, new KMLDocumentHandler(output));
            }
        } catch (JAXBException | TransformerException e) {
            throw new ServiceException(e);
        } finally {
            if (context != null) {
                context.closeIterators();
            }
        }
    }

    private Marshaller createMarshaller() throws JAXBException {
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        // hmm... this one is nasty, without the reference implementation the prefixes
        // are going to be a bit ugly. Not a big deal, to solve look at
        // http://cglib.sourceforge.net/xref/samples/Beans.html
        // try {
        // Class.forName("com.sun.xml.internal.bind.marshaller.NamespacePrefixMapper");
        // m.setProperty("com.sun.xml.bind.namespacePrefixMapper", new JKD6PrefixMapper());
        // } catch(Exception e) {
        //
        // }

        return m;
    }

    /**
     * A custom ContentHandler that removes several invalid IconStyle elements from the JavaAPIforKml JAXB output that
     * will cause the generated KML to not validate against the KML schema. This bug is fixed in version 3.x of the
     * library which will be part of the Jakarta EE migration and this workaround can be removed then.
     */
    private static final class KMLDocumentHandler implements ContentHandler {

        private static final SAXTransformerFactory FACTORY = (SAXTransformerFactory) TransformerFactory.newInstance();

        private static final String ROOT_NAMESPACE = "http://www.opengis.net/kml/2.2";

        private static final Map<String, String> EXTRA_NAMESPACES = Map.of(
                "ns2", "http://www.google.com/kml/ext/2.2",
                "ns3", "http://www.w3.org/2005/Atom",
                "ns4", "urn:oasis:names:tc:ciq:xsdschema:xAL:2.0");

        // //kml:IconStyle/kml:Icon/kml:refreshInterval
        // //kml:IconStyle/kml:Icon/kml:viewRefreshTime
        // //kml:IconStyle/kml:Icon/kml:viewBoundScale
        private static final List<String> IGNORED_ELEMENTS =
                List.of("refreshInterval", "viewRefreshTime", "viewBoundScale");

        private final TransformerHandler transformerHandler;

        private boolean inIconStyle = false;

        private boolean inIcon = false;

        private boolean inIgnoredElement = false;

        private boolean rootProcessed = false;

        private KMLDocumentHandler(OutputStream output) throws TransformerException {
            transformerHandler = FACTORY.newTransformerHandler();
            transformerHandler.getTransformer().setOutputProperty("indent", "yes");
            transformerHandler.getTransformer().setOutputProperty(OutputKeys.METHOD, "xml");
            transformerHandler.getTransformer().setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformerHandler.getTransformer().setOutputProperty(OutputKeys.STANDALONE, "no");
            transformerHandler.setResult(new StreamResult(output));
        }

        @Override
        public void setDocumentLocator(Locator locator) {
            transformerHandler.setDocumentLocator(locator);
        }

        @Override
        public void startDocument() throws SAXException {
            transformerHandler.startDocument();
        }

        @Override
        public void endDocument() throws SAXException {
            transformerHandler.endDocument();
        }

        @Override
        public void startPrefixMapping(String prefix, String uri) {
            if (!rootProcessed) {
                try {
                    transformerHandler.startPrefixMapping("", ROOT_NAMESPACE);
                } catch (SAXException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public void endPrefixMapping(String prefix) {
            if (!rootProcessed) {
                try {
                    transformerHandler.endPrefixMapping("");
                } catch (SAXException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            if ("IconStyle".equals(localName)) {
                inIconStyle = true;
            } else if (inIconStyle && "Icon".equals(localName)) {
                inIcon = true;
            } else if (inIcon && IGNORED_ELEMENTS.contains(localName)) {
                inIgnoredElement = true;
                return;
            }

            if (!rootProcessed) {
                // Root element: use namespace and declare extras
                for (Map.Entry<String, String> ns : EXTRA_NAMESPACES.entrySet()) {
                    transformerHandler.startPrefixMapping(ns.getKey(), ns.getValue());
                }
                transformerHandler.startElement(ROOT_NAMESPACE, localName, localName, atts);
                rootProcessed = true;
            } else {
                // Keep prefix only for non-default namespaces
                String prefix = getPrefixForUri(uri);
                if (prefix != null && !prefix.isEmpty()) {
                    transformerHandler.startElement(uri, localName, prefix + ":" + localName, atts);
                } else {
                    transformerHandler.startElement("", localName, localName, atts);
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (inIcon && IGNORED_ELEMENTS.contains(localName)) {
                inIgnoredElement = false;
                return;
            } else if (inIconStyle && "Icon".equals(localName)) inIcon = false;
            else if ("IconStyle".equals(localName)) inIconStyle = false;

            if (rootProcessed && localName.equals("kml")) {
                transformerHandler.endElement(ROOT_NAMESPACE, localName, localName);
                for (String prefix : EXTRA_NAMESPACES.keySet()) {
                    transformerHandler.endPrefixMapping(prefix);
                }
            } else {
                String prefix = getPrefixForUri(uri);
                if (prefix != null && !prefix.isEmpty()) {
                    transformerHandler.endElement(uri, localName, prefix + ":" + localName);
                } else {
                    transformerHandler.endElement("", localName, localName);
                }
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (!inIgnoredElement) {
                transformerHandler.characters(ch, start, length);
            }
        }

        @Override
        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
            transformerHandler.ignorableWhitespace(ch, start, length);
        }

        @Override
        public void processingInstruction(String target, String data) throws SAXException {
            transformerHandler.processingInstruction(target, data);
        }

        @Override
        public void skippedEntity(String name) throws SAXException {
            transformerHandler.skippedEntity(name);
        }

        private String getPrefixForUri(String uri) {
            if (uri == null || uri.isEmpty() || uri.equals(ROOT_NAMESPACE)) {
                return null;
            }
            for (Map.Entry<String, String> e : EXTRA_NAMESPACES.entrySet()) {
                if (e.getValue().equals(uri)) return e.getKey();
            }
            return null;
        }
    }
}
