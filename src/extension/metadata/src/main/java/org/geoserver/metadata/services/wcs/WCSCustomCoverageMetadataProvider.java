/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.services.wcs;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.metadata.data.service.impl.MetadataConstants;
import org.geoserver.ows.Dispatcher;
import org.geoserver.wcs2_0.response.WCS20CoverageMetadataProvider;
import org.geotools.util.logging.Logging;
import org.springframework.stereotype.Service;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.NamespaceSupport;

@Service
public class WCSCustomCoverageMetadataProvider implements WCS20CoverageMetadataProvider {

    static final Logger LOGGER = Logging.getLogger(WCSCustomCoverageMetadataProvider.class);

    private GeoServer gs;

    public WCSCustomCoverageMetadataProvider(GeoServer gs) {
        this.gs = gs;
    }

    @Override
    public String[] getSchemaLocations(String schemaBaseURL) {
        return new String[] {};
    }

    @Override
    public void registerNamespaces(NamespaceSupport namespaces) {}

    @Override
    public void encode(Translator tx, Object context) throws IOException {
        CoverageInfo ci;
        if (context == null) {
            // hack hack, get it straight from the request
            String coverageId = (String) Dispatcher.REQUEST.get().getKvp().get("CoverageId");
            ci = gs.getCatalog().getCoverageByName(coverageId);
            if (ci == null) {
                return;
            }
        } else if (!(context instanceof CoverageInfo)) {
            return;
        } else {
            ci = (CoverageInfo) context;
        }

        Serializable custom = ci.getMetadata().get(MetadataConstants.CUSTOM_METADATA_KEY);
        if (custom == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        String xml =
                (String)
                        ((HashMap<String, Serializable>) custom)
                                .get(MetadataConstants.WCS_FIELD_ATTRIBUTE);
        if (xml == null) {
            return;
        }

        try {
            SAXParserFactory.newInstance()
                    .newSAXParser()
                    .parse(
                            new InputSource(new StringReader(xml)),
                            new DefaultHandler() {
                                @Override
                                public void startElement(
                                        String uri,
                                        String localName,
                                        String qName,
                                        Attributes attributes) {
                                    tx.start(qName, attributes);
                                }

                                @Override
                                public void endElement(String uri, String localName, String qName)
                                        throws SAXException {
                                    tx.end(qName);
                                }

                                @Override
                                public void characters(char[] ch, int start, int length)
                                        throws SAXException {
                                    tx.chars(new String(ch, start, length));
                                }
                            });
        } catch (SAXException | ParserConfigurationException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }
    }
}
