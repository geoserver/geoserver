/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.response;

import java.io.StringReader;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import net.opengis.cat.csw20.RequestBaseType;
import org.apache.xml.serializer.TreeWalker;
import org.geoserver.csw.xml.v2_0_2.CSWRecordingXmlReader;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.ServiceException;
import org.geotools.csw.CSW;
import org.geotools.util.Converters;
import org.geotools.xlink.XLINK;
import org.geotools.xml.transform.Translator;
import org.geotools.xsd.ows.OWS;
import org.w3c.dom.Document;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Encodes a Acknoledgement response
 *
 * @author Andrea Aime - GeoSolutions
 */
public class AcknowledgementTransformer extends AbstractCSWTransformer {

    public AcknowledgementTransformer(RequestBaseType request, boolean canonicalSchemaLocation) {
        super(request, canonicalSchemaLocation);
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new AcknowledgementTranslator(handler);
    }

    class AcknowledgementTranslator extends AbstractCSWTranslator {

        public AcknowledgementTranslator(ContentHandler handler) {
            super(handler);
        }

        @Override
        public void encode(Object o) throws IllegalArgumentException {
            AttributesImpl attributes = new AttributesImpl();
            addAttribute(attributes, "xmlns:csw", CSW.NAMESPACE);
            addAttribute(attributes, "xmlns:ows", OWS.NAMESPACE);
            addAttribute(attributes, "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            addAttribute(attributes, "xmlns:xlink", XLINK.NAMESPACE);

            String locationAtt = "xsi:schemaLocation";
            StringBuilder locationDef = new StringBuilder();
            locationDef.append(CSW.NAMESPACE).append(" ");
            locationDef.append(cswSchemaLocation("record.xsd"));
            addAttribute(attributes, locationAtt, locationDef.toString());

            addAttribute(attributes, "timeStamp", Converters.convert(new Date(), String.class));

            start("csw:Acknowledgement", attributes);
            start("csw:EchoedRequest");

            Request request = Dispatcher.REQUEST.get();
            if (request.isGet()) {
                encodeGetEcho(request);
            } else {
                encodePostEcho();
            }

            end("csw:EchoedRequest");
            end("csw:Acknowledgement");
        }

        private void encodeGetEcho(Request request) {
            // grab the full request url
            HttpServletRequest httpRequest = request.getHttpRequest();
            String fullRequest = httpRequest.getRequestURL().toString();
            String queryString = httpRequest.getQueryString();
            // odd check, used because the mock http request used for testing
            // actually includes the query string in the request url
            if (queryString != null && !fullRequest.contains("?")) {
                fullRequest += "?" + queryString;
            }

            // build the ows:Get element
            AttributesImpl attributes = new AttributesImpl();
            addAttribute(attributes, "xlink:type", "simple");
            addAttribute(attributes, "xlink:href", fullRequest);
            element("ows:Get", null, attributes);
        }

        public void encodePostEcho() {
            String request = CSWRecordingXmlReader.RECORDED_REQUEST.get();
            if (request != null) {
                Document dom = parseAsXML(request);
                dumpAsXML(dom);
            }
        }

        private void dumpAsXML(Document document) {
            try {
                TreeWalker tw = new TreeWalker(contentHandler);
                tw.traverse(document);
            } catch (Exception e) {
                throw new ServiceException(
                        "Failed to re-encode the original request in the Acknowledgement response");
            }
        }

        private Document parseAsXML(String data) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                factory.setValidating(false);

                DocumentBuilder builder = factory.newDocumentBuilder();
                if (!data.startsWith("<?xml")) {
                    data = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + data;
                }
                return builder.parse(new InputSource(new StringReader(data)));
            } catch (Throwable t) {
                throw new ServiceException(
                        "Failed to parse the original request into XML, "
                                + "this should never happen??",
                        t);
            }
        }
    }
}
