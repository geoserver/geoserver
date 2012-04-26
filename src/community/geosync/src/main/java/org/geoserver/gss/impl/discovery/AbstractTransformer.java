/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.impl.discovery;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.geoserver.gss.service.GetCapabilities;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.xml.transform.TransformerBase;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

public abstract class AbstractTransformer extends TransformerBase {

    public abstract static class AbstractTranslator extends TranslatorSupport {

        public AbstractTranslator(ContentHandler handler, String prefix, String namespace) {
            super(handler, prefix, namespace);
        }

        protected String schemaLocation(GetCapabilities request, String... kvp) {
            final String baseURL = request.getBaseUrl();
            StringBuilder result = new StringBuilder();
            String[] locations = kvp;
            for (int i = 0; i < locations.length; i += 2) {
                String namespace = locations[i];
                String relativeLocation = locations[i + 1];
                String location;
                if (relativeLocation.startsWith("http")) {
                    location = relativeLocation;
                } else {
                    location = ResponseUtils.buildSchemaURL(baseURL, relativeLocation);
                }
                if (i > 0) {
                    result.append(' ');
                }
                result.append(namespace);
                result.append(' ');
                result.append(location);
            }
            return result.toString();
        }

        protected AttributesImpl attributes(String... kvp) {
            String[] atts = kvp;
            AttributesImpl attributes = new AttributesImpl();
            for (int i = 0; i < atts.length; i += 2) {
                String name = atts[i];
                String value = atts[i + 1];
                attributes.addAttribute("", name, name, "", value);
            }
            return attributes;
        }

        public void start(QName name) {
            start(name, null);
        }

        public void start(QName name, Attributes attributes) {
            String qName = qname(name);
            start(qName, attributes);
        }

        public void end(QName name) {
            String qName = qname(name);
            end(qName);
        }

        public void element(QName name, Attributes attributes, String content) {
            String qName = qname(name);
            start(qName, attributes);
            if (content != null) {
                chars(content);
            }
            end(qName);
        }

        public String qname(QName name) {
            String prefix = name.getPrefix();
            String localPart = name.getLocalPart();
            if (prefix == null || XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
                prefix = getNamespaceSupport().getPrefix(name.getNamespaceURI());
                if (prefix == null) {
                    prefix = XMLConstants.DEFAULT_NS_PREFIX;
                }
            }
            String qName = new StringBuilder(prefix).append(':').append(localPart).toString();
            return qName;
        }
    }
}
