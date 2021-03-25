/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.response;

import java.util.Map;
import org.geotools.xml.transform.TransformerBase;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Extends {@link TransformerBase} to provide some extra Java 8 based utilities methods for
 * encoding. Will eventually be merged with {@link TransformerBase}
 *
 * @author Andrea Aime - GeoSolutions
 */
abstract class LambdaTransformerBase extends TransformerBase {

    /** Delegate encoder encoding no contents */
    protected static final Runnable NO_CONTENTS = () -> {};

    protected abstract static class LambdaTranslatorSupport extends TranslatorSupport {

        public LambdaTranslatorSupport(ContentHandler contentHandler) {
            super(contentHandler, null, null);
        }

        public LambdaTranslatorSupport(
                ContentHandler contentHandler,
                String prefix,
                String nsURI,
                SchemaLocationSupport schemaLocation) {
            super(contentHandler, prefix, nsURI, schemaLocation);
        }

        public LambdaTranslatorSupport(ContentHandler contentHandler, String prefix, String nsURI) {
            super(contentHandler, prefix, nsURI);
        }

        /**
         * Encodes an element, delegating encoding its sub-elements to the content encoder, with no
         * attributes
         */
        protected void element(String elementName, Runnable contentsEncoder) {
            element(elementName, contentsEncoder, null);
        }

        /** Encodes an element, delegating encoding its sub-elements to the content encoder */
        protected void element(
                String elementName, Runnable contentsEncoder, Attributes attributes) {
            if (attributes != null) {
                start(elementName, attributes);
            } else {
                start(elementName);
            }
            if (contentsEncoder != null) {
                contentsEncoder.run();
            }
            end(elementName);
        }

        /** Builds {@link Attributes} from a map */
        protected Attributes attributes(Map<String, String> map) {
            AttributesImpl attributes = new AttributesImpl();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String name = entry.getKey();
                String value = entry.getValue();
                attributes.addAttribute("", name, name, "", value);
            }
            return attributes;
        }

        /**
         * Builds {@link Attributes} from an array of string pairs, key1, value1, key2, value2, ...
         */
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
    }
}
