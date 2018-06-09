/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire.wmts;

import static org.geoserver.inspire.InspireMetadata.CREATE_EXTENDED_CAPABILITIES;
import static org.geoserver.inspire.InspireMetadata.LANGUAGE;
import static org.geoserver.inspire.InspireMetadata.SERVICE_METADATA_TYPE;
import static org.geoserver.inspire.InspireMetadata.SERVICE_METADATA_URL;
import static org.geoserver.inspire.InspireSchema.COMMON_NAMESPACE;

import java.io.IOException;
import org.geoserver.ExtendedCapabilitiesProvider;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.gwc.wmts.WMTSInfo;
import org.geoserver.inspire.ViewServicesUtils;
import org.geowebcache.config.meta.ServiceInformation;
import org.geowebcache.io.XMLBuilder;
import org.geowebcache.service.wmts.WMTSExtensionImpl;
import org.xml.sax.Attributes;

public class WMTSExtendedCapabilitiesProvider extends WMTSExtensionImpl {

    public static final String VS_VS_OWS_NAMESPACE =
            "http://inspire.ec.europa.eu/schemas/inspire_vs_ows11/1.0";
    public static final String VS_VS_OWS_SCHEMA =
            "http://inspire.ec.europa.eu/schemas/inspire_vs_ows11/1.0/inspire_vs_ows_11.xsd";

    private final GeoServer geoserver;

    public WMTSExtendedCapabilitiesProvider(GeoServer geoserver) {
        this.geoserver = geoserver;
    }

    @Override
    public String[] getSchemaLocations() {
        return new String[] {VS_VS_OWS_NAMESPACE, VS_VS_OWS_SCHEMA};
    }

    @Override
    public void registerNamespaces(XMLBuilder xml) throws IOException {
        xml.attribute("xmlns:inspire_vs", VS_VS_OWS_NAMESPACE);
        xml.attribute("xmlns:inspire_common", COMMON_NAMESPACE);
    }

    @Override
    public void encodedOperationsMetadata(XMLBuilder xml) throws IOException {

        // create custom translator
        ExtendedCapabilitiesProvider.Translator translator =
                new org.geoserver.ExtendedCapabilitiesProvider.Translator() {

                    @Override
                    public void start(String element) {
                        try {
                            xml.indentElement(element);
                        } catch (IOException exception) {
                            throw new RuntimeException(exception);
                        }
                    }

                    @Override
                    public void start(String element, Attributes attributes) {
                        try {
                            xml.indentElement(element);
                            for (int i = 0; i < attributes.getLength(); i++) {
                                xml.attribute(attributes.getQName(i), attributes.getValue(i));
                            }
                        } catch (IOException exception) {
                            throw new RuntimeException(exception);
                        }
                    }

                    @Override
                    public void chars(String text) {
                        try {
                            xml.text(text);
                        } catch (IOException exception) {
                            throw new RuntimeException(exception);
                        }
                    }

                    @Override
                    public void end(String element) {
                        try {
                            xml.endElement(element);
                        } catch (IOException exception) {
                            throw new RuntimeException(exception);
                        }
                    }
                };

        // write inspire scenario 1 metadata
        MetadataMap serviceMetadata = geoserver.getService(WMTSInfo.class).getMetadata();
        Boolean createExtendedCapabilities =
                serviceMetadata.get(CREATE_EXTENDED_CAPABILITIES.key, Boolean.class);
        String metadataURL = (String) serviceMetadata.get(SERVICE_METADATA_URL.key);
        if (metadataURL == null
                || createExtendedCapabilities != null && !createExtendedCapabilities) {
            return;
        }
        String mediaType = (String) serviceMetadata.get(SERVICE_METADATA_TYPE.key);
        String language = (String) serviceMetadata.get(LANGUAGE.key);
        ViewServicesUtils.addScenario1Elements(translator, metadataURL, mediaType, language);
    }

    @Override
    public ServiceInformation getServiceInformation() {
        return new ServiceInformation();
    }
}
