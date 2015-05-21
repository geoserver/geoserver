/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire.wms;

import static org.geoserver.inspire.InspireMetadata.LANGUAGE;
import static org.geoserver.inspire.InspireMetadata.SERVICE_METADATA_TYPE;
import static org.geoserver.inspire.InspireMetadata.SERVICE_METADATA_URL;
import static org.geoserver.inspire.InspireSchema.COMMON_NAMESPACE;
import static org.geoserver.inspire.InspireSchema.VS_NAMESPACE;
import static org.geoserver.inspire.InspireSchema.VS_SCHEMA;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.wms.ExtendedCapabilitiesProvider;
import org.geoserver.wms.GetCapabilitiesRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geotools.util.NumberRange;
import org.geotools.util.Version;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;

public class WMSExtendedCapabilitiesProvider implements ExtendedCapabilitiesProvider {

    @Override
    public String[] getSchemaLocations(String schemaBaseURL) {
        return new String[]{VS_NAMESPACE, VS_SCHEMA};
    }

    /**
     * @return empty list, INSPIRE profile for WMS 1.1.1 not supported.
     * @see
     * org.geoserver.wms.ExtendedCapabilitiesProvider#getVendorSpecificCapabilitiesRoots()
     */
    @Override
    public List<String> getVendorSpecificCapabilitiesRoots(GetCapabilitiesRequest request) {
        return Collections.emptyList();
    }

    /**
     * @return empty list, INSPIRE profile for WMS 1.1.1 not supported.
     * @see
     * org.geoserver.wms.ExtendedCapabilitiesProvider#getVendorSpecificCapabilitiesChildDecls()
     */
    @Override
    public List<String> getVendorSpecificCapabilitiesChildDecls(GetCapabilitiesRequest request) {
        return Collections.emptyList();
    }

    @Override
    public void registerNamespaces(NamespaceSupport namespaces) {
        namespaces.declarePrefix("inspire_vs", VS_NAMESPACE);
        namespaces
                .declarePrefix("inspire_common", COMMON_NAMESPACE);
        namespaces.declarePrefix("gml", "http://schemas.opengis.net/gml");
        namespaces
                .declarePrefix("gmd", "http://schemas.opengis.net/iso/19139/20060504/gmd/gmd.xsd");
        namespaces
                .declarePrefix("gco", "http://schemas.opengis.net/iso/19139/20060504/gco/gco.xsd");
        namespaces
                .declarePrefix("srv", "http://schemas.opengis.net/iso/19139/20060504/srv/srv.xsd");
    }

    @Override
    public void encode(Translator tx, WMSInfo wms, GetCapabilitiesRequest request)
            throws IOException {
        Version requestVersion = WMS.version(request.getVersion());

        // if this is not a wms 1.3.0 request
        if (!WMS.VERSION_1_3_0.equals(requestVersion)) {
            return;
        }

        // IGN : INSPIRE SCENARIO 1
        tx.start("inspire_vs:ExtendedCapabilities");

        // Metadata URL
        tx.start("inspire_common:MetadataUrl",
                atts("xsi:type", "inspire_common:resourceLocatorType"));
        String metadataURL = (String) wms.getMetadata().get(SERVICE_METADATA_URL.key);
        tx.start("inspire_common:URL");
        if (metadataURL != null) {
            tx.chars(metadataURL);
        }
        tx.end("inspire_common:URL");

        String metadataMediaType = (String) wms.getMetadata().get(SERVICE_METADATA_TYPE.key);
        if (metadataMediaType == null) {
            //default
            metadataMediaType = "application/vnd.iso.19139+xml";
        }
        tx.start("inspire_common:MediaType");
        tx.chars(metadataMediaType);
        tx.end("inspire_common:MediaType");
        tx.end("inspire_common:MetadataUrl");

        // SupportedLanguages
        tx.start("inspire_common:SupportedLanguages",
                atts("xsi:type", "inspire_common:supportedLanguagesType"));
        String language = (String) wms.getMetadata().get(LANGUAGE.key);
        language = language != null ? language : "eng";
        tx.start("inspire_common:DefaultLanguage");
        tx.start("inspire_common:Language");
        tx.chars(language);
        tx.end("inspire_common:Language");
        tx.end("inspire_common:DefaultLanguage");
        tx.start("inspire_common:SupportedLanguage");
        tx.start("inspire_common:Language");
        tx.chars(language);
        tx.end("inspire_common:Language");
        tx.end("inspire_common:SupportedLanguage");
        tx.end("inspire_common:SupportedLanguages");

        // ResponseLanguage
        tx.start("inspire_common:ResponseLanguage");
        tx.start("inspire_common:Language");
        tx.chars(language);
        tx.end("inspire_common:Language");
        tx.end("inspire_common:ResponseLanguage");

        tx.end("inspire_vs:ExtendedCapabilities");

    }

    Attributes atts(String... atts) {
        AttributesImpl attributes = new AttributesImpl();
        for (int i = 0; i < atts.length; i += 2) {
            attributes.addAttribute(null, atts[i], atts[i], null, atts[i + 1]);
        }
        return attributes;
    }

    @Override
    public void customizeRootCrsList(Set<String> srs) {
        // nothing to do
    }

    @Override
    public NumberRange<Double> overrideScaleDenominators(LayerInfo layer,
            NumberRange<Double> scaleDenominators) {
        return scaleDenominators;
    }

}
