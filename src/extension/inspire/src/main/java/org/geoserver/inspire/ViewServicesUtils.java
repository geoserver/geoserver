/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire;

import static org.geoserver.inspire.InspireSchema.COMMON_NAMESPACE;
import static org.geoserver.inspire.InspireSchema.VS_NAMESPACE;

import org.geoserver.ExtendedCapabilitiesProvider.Translator;
import org.xml.sax.helpers.NamespaceSupport;

public final class ViewServicesUtils {

    private ViewServicesUtils() {}

    public static void registerNameSpaces(NamespaceSupport namespaces) {
        namespaces.declarePrefix("inspire_vs", VS_NAMESPACE);
        namespaces.declarePrefix("inspire_common", COMMON_NAMESPACE);
    }

    public static void addScenario1Elements(
            Translator translator, String metadataUrl, String mediaType, String language) {
        translator.start("inspire_vs:ExtendedCapabilities");
        translator.start("inspire_common:MetadataUrl");
        translator.start("inspire_common:URL");
        translator.chars(metadataUrl);
        translator.end("inspire_common:URL");
        if (mediaType != null) {
            translator.start("inspire_common:MediaType");
            translator.chars(mediaType);
            translator.end("inspire_common:MediaType");
        }
        translator.end("inspire_common:MetadataUrl");
        translator.start("inspire_common:SupportedLanguages");
        language = language != null ? language : "eng";
        translator.start("inspire_common:DefaultLanguage");
        translator.start("inspire_common:Language");
        translator.chars(language);
        translator.end("inspire_common:Language");
        translator.end("inspire_common:DefaultLanguage");
        translator.end("inspire_common:SupportedLanguages");
        translator.start("inspire_common:ResponseLanguage");
        translator.start("inspire_common:Language");
        translator.chars(language);
        translator.end("inspire_common:Language");
        translator.end("inspire_common:ResponseLanguage");
        translator.end("inspire_vs:ExtendedCapabilities");
    }
}
