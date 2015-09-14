/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire.wms;

import static org.geoserver.inspire.InspireMetadata.CREATE_EXTENDED_CAPABILITIES;
import static org.geoserver.inspire.InspireMetadata.SERVICE_METADATA_TYPE;
import static org.geoserver.inspire.InspireMetadata.SERVICE_METADATA_URL;
import static org.geoserver.inspire.InspireMetadata.LANGUAGE;
import static org.geoserver.inspire.InspireSchema.COMMON_NAMESPACE;
import static org.geoserver.inspire.InspireSchema.VS_NAMESPACE;
import static org.geoserver.inspire.InspireSchema.VS_SCHEMA;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
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
    }

    @Override
    public void encode(Translator tx, WMSInfo wms, GetCapabilitiesRequest request)
            throws IOException {
        Version requestVersion = WMS.version(request.getVersion());
        // if this is not a wms 1.3.0 request
        if (!WMS.VERSION_1_3_0.equals(requestVersion)) {
            return;
        }
        MetadataMap serviceMetadata = wms.getMetadata();
        Boolean createExtendedCapabilities = serviceMetadata.get(CREATE_EXTENDED_CAPABILITIES.key, Boolean.class);
        String metadataURL = (String) serviceMetadata.get(SERVICE_METADATA_URL.key);
        //Don't create extended capabilities element if mandatory content not present
        //or turned off
        if (metadataURL == null
                || createExtendedCapabilities != null
                && !createExtendedCapabilities) {
            return;
        }
        String mediaType = (String) serviceMetadata.get(SERVICE_METADATA_TYPE.key);
        String language = (String) serviceMetadata.get(LANGUAGE.key);

        // IGN : INSPIRE SCENARIO 1
        tx.start("inspire_vs:ExtendedCapabilities");
        tx.start("inspire_common:MetadataUrl");
        tx.start("inspire_common:URL");
        tx.chars(metadataURL);
        tx.end("inspire_common:URL");
        if (mediaType != null) {
            tx.start("inspire_common:MediaType");
            tx.chars(mediaType);
            tx.end("inspire_common:MediaType");
        }
        tx.end("inspire_common:MetadataUrl");
        tx.start("inspire_common:SupportedLanguages");
        language = language != null ? language : "eng";
        tx.start("inspire_common:DefaultLanguage");
        tx.start("inspire_common:Language");
        tx.chars(language);
        tx.end("inspire_common:Language");
        tx.end("inspire_common:DefaultLanguage");
        tx.end("inspire_common:SupportedLanguages");
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
