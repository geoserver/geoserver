/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire;

import static org.geoserver.inspire.InspireMetadata.LANGUAGE;
import static org.geoserver.inspire.InspireMetadata.OTHER_LANGUAGES;
import static org.geoserver.inspire.InspireSchema.COMMON_NAMESPACE;
import static org.geoserver.inspire.InspireSchema.VS_NAMESPACE;
import static org.geoserver.inspire.LanguagesDispatcherCallback.LANGUAGE_PARAM;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.geoserver.ExtendedCapabilitiesProvider;
import org.geoserver.ExtendedCapabilitiesProvider.Translator;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.ows.Dispatcher;
import org.xml.sax.helpers.NamespaceSupport;

/** Utility class that provides methods to encode Inspire elements in GetCapabilities Response */
public final class ServicesUtils {

    private ServicesUtils() {}

    /**
     * Register vs and inspire common namspaces to the passed NamespaceSupport.
     *
     * @param namespaces the NamespaceSupport to which register the namespaces.
     */
    public static void registerNameSpaces(NamespaceSupport namespaces) {
        namespaces.declarePrefix("inspire_vs", VS_NAMESPACE);
        namespaces.declarePrefix("inspire_common", COMMON_NAMESPACE);
    }

    /**
     * Encode ExtendedCapabilities elements for View services (WMS and WMTS).
     *
     * @param translator the translator to use to encode elements.
     * @param metadataUrl the metadataUrl.
     * @param mediaType the mediaType.
     * @param metadataMap the service metadata map.
     */
    public static void addScenario1Elements(
            Translator translator, String metadataUrl, String mediaType, MetadataMap metadataMap) {

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
        encodeSupportedLanguages(metadataMap, translator);
        translator.end("inspire_vs:ExtendedCapabilities");
    }

    /**
     * Encode the SupportedLanguage content of Extended Capabilities.
     *
     * @param metadataMap the service metadata map.
     * @param translator the Translator to use for element encoding.
     */
    public static void encodeSupportedLanguages(
            MetadataMap metadataMap, ExtendedCapabilitiesProvider.Translator translator) {
        String defaultLanguage = null;
        if (metadataMap.get(LANGUAGE.key) != null)
            defaultLanguage = metadataMap.get(LANGUAGE.key).toString();
        String otherLanguages = null;
        if (metadataMap.get(OTHER_LANGUAGES.key) != null)
            otherLanguages = metadataMap.get(OTHER_LANGUAGES.key).toString();
        List<String> langList =
                otherLanguages == null ? null : Arrays.asList(otherLanguages.split(","));
        // encode the default language
        translator.start("inspire_common:SupportedLanguages");
        translator.start("inspire_common:DefaultLanguage");
        translator.start("inspire_common:Language");
        translator.chars(defaultLanguage);
        translator.end("inspire_common:Language");
        translator.end("inspire_common:DefaultLanguage");
        if (langList != null) {
            for (String lang : langList) {
                if (!lang.equals(defaultLanguage) && !lang.equals("")) {
                    translator.start("inspire_common:SupportedLanguage");
                    translator.start("inspire_common:Language");
                    translator.chars(lang);
                    translator.end("inspire_common:Language");
                    translator.end("inspire_common:SupportedLanguage");
                }
            }
        }
        translator.end("inspire_common:SupportedLanguages");
        // encode supported languages

        translator.start("inspire_common:ResponseLanguage");
        translator.start("inspire_common:Language");
        translator.chars(retrieveLanguageParameter(defaultLanguage, langList));
        translator.end("inspire_common:Language");
        translator.end("inspire_common:ResponseLanguage");
    }

    private static String retrieveLanguageParameter(
            String defaultLanguage, List<String> languages) {
        Map<String, Object> kvpDispatcher = Dispatcher.REQUEST.get().getRawKvp();
        String value = null;
        if (kvpDispatcher != null) {
            if (kvpDispatcher.containsKey(LANGUAGE_PARAM)) {
                Object reqLang = kvpDispatcher.get(LANGUAGE_PARAM);
                value = getIfSupported(reqLang, languages);
            }
        }
        if (value == null || value.isEmpty()) value = defaultLanguage;
        return value;
    }

    // Check if the requested languages is among the supported ones.
    private static String getIfSupported(Object reqLang, List<String> inspireLangs) {
        String value = null;
        if (reqLang != null) {
            String inspireLang = mapToInspireLanguage(reqLang.toString());
            if (inspireLangs.contains(inspireLang)) value = inspireLang;
        }
        return value;
    }

    // Map the provided ISO lang to the corresponding INSPIRE one
    // If none is matched return the lang parameter.
    private static String mapToInspireLanguage(String lang) {
        try {
            Properties props = InspireDirectoryManager.get().getLanguagesMappings();
            String result = lang;
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                String key = entry.getKey().toString();
                if (entry.getValue().equals(lang)) {
                    result = key;
                    break;
                }
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
