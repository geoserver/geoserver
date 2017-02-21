/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.response;

import java.util.stream.Collectors;

import org.geoserver.catalog.KeywordInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.opensearch.eo.OSEODescription;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Encodes a {@link OSEODescriptionResponse} into a OSDD document
 *
 * @author Andrea Aime - GeoSolutions
 */
public class OSEODescriptionTransformer extends TransformerBase {

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new OSEODescriptionTranslator(handler);
    }

    /**
     * @author Gabriel Roldan
     * @version $Id
     */
    private static class OSEODescriptionTranslator extends TranslatorSupport {

        public OSEODescriptionTranslator(ContentHandler contentHandler) {
            super(contentHandler, null, null);
        }

        void element(String elementName, Runnable contentsEncoder, String... attributes) {
            if(attributes != null && attributes.length > 0) {
                start(elementName, attributes(attributes));
            } else {
                start(elementName);
            }
            if(contentsEncoder != null) {
                contentsEncoder.run();
            }
            end(elementName);
        }
        
        private AttributesImpl attributes(String... kvp) {
            String[] atts = kvp;
            AttributesImpl attributes = new AttributesImpl();
            for (int i = 0; i < atts.length; i += 2) {
                String name = atts[i];
                String value = atts[i + 1];
                attributes.addAttribute("", name, name, "", value);
            }
            return attributes;
        }

        
        @Override
        public void encode(Object o) throws IllegalArgumentException {
            OSEODescription description = (OSEODescription) o;
            // <OpenSearchDescription xmlns="http://a9.com/-/spec/opensearch/1.1/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:eo="http://a9.com/-/opensearch/extensions/eo/1.0/" xmlns:geo="http://a9.com/-/opensearch/extensions/geo/1.0/" xmlns:param="http://a9.com/-/spec/opensearch/extensions/parameters/1.0/" xmlns:semantic="http://a9.com/-/opensearch/extensions/semantic/1.0/" xmlns:sru="http://a9.com/-/opensearch/extensions/sru/2.0/" xmlns:time="http://a9.com/-/opensearch/extensions/time/1.0/" xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
            element("OpenSearchDescription", () -> describeOpenSearch(description), "xmlns", "http://a9.com/-/spec/opensearch/1.1/");
        }

        private void describeOpenSearch(OSEODescription description) {
            OSEOInfo oseo = description.getServiceInfo();
            // while the OpenSearch specification does not seem to mandate a specific order for tags,
            // the one of the spec examples has been followed in order to ensure maximum compatibility with clients
            element("ShortName", oseo.getName());
            element("Description", oseo.getAbstract());
            GeoServerInfo gs = description.getGeoserverInfo();
            element("Contact", gs.getSettings().getContact().getContactEmail());
            String tags = oseo.getKeywords().stream().map(k -> k.getValue()).collect(Collectors.joining(" "));
            element("Tags", tags);
            element("LongName", oseo.getTitle());
            element("Developer", oseo.getMaintainer());
            element("SyndicationRight", "open"); // make configurable?
            element("AdultContent", "false");
            element("Language", "en-us");
            element("OutputEncoding", "UTF-8");
            element("InputEncoding", "UTF-8");
        }
        
    }



}
