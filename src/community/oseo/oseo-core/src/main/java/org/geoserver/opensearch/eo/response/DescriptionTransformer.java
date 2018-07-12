/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.response;

import static org.geoserver.ows.util.ResponseUtils.appendQueryString;
import static org.geoserver.ows.util.ResponseUtils.buildURL;
import static org.geoserver.ows.util.ResponseUtils.params;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.opensearch.eo.OSEODescription;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.OpenSearchParameters;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.data.Parameter;
import org.geotools.xml.transform.Translator;
import org.xml.sax.ContentHandler;

/**
 * Encodes a {@link DescriptionResponse} into a OSDD document
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DescriptionTransformer extends LambdaTransformerBase {

    OSEOInfo oseo;

    public DescriptionTransformer(OSEOInfo oseo) {
        this.oseo = oseo;
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new OSEODescriptionTranslator(handler, oseo);
    }

    private class OSEODescriptionTranslator extends LambdaTranslatorSupport {

        private static final String CEOS_SUPPORTED_VERSION = "CEOS-OS-BP-V1.2/L1";
        private final OSEOInfo oseo;

        public OSEODescriptionTranslator(ContentHandler contentHandler, OSEOInfo oseo) {
            super(contentHandler);
            this.oseo = oseo;
        }

        @Override
        public void encode(Object o) throws IllegalArgumentException {
            OSEODescription description = (OSEODescription) o;
            Map<String, String> namespaces = new LinkedHashMap<>();
            namespaces.put("xmlns", "http://a9.com/-/spec/opensearch/1.1/");
            namespaces.put(
                    "xmlns:param", "http://a9.com/-/spec/opensearch/extensions/parameters/1.0/");
            namespaces.put("xmlns:geo", "http://a9.com/-/opensearch/extensions/geo/1.0/");
            namespaces.put("xmlns:time", "http://a9.com/-/opensearch/extensions/time/1.0/");
            namespaces.put("xmlns:eo", "http://a9.com/-/opensearch/extensions/eo/1.0/");
            namespaces.put("xmlns:atom  ", "http://www.w3.org/2005/Atom");
            element(
                    "OpenSearchDescription",
                    () -> describeOpenSearch(description),
                    attributes(namespaces));
        }

        private void describeOpenSearch(OSEODescription description) {
            OSEOInfo oseo = description.getServiceInfo();
            // while the OpenSearch specification does not seem to mandate a specific order for
            // tags,
            // the one of the spec examples has been followed in order to ensure maximum
            // compatibility with clients
            element("ShortName", oseo.getName());
            element("Description", oseo.getAbstract());
            GeoServerInfo gs = description.getGeoserverInfo();
            element("Contact", gs.getSettings().getContact().getContactEmail());
            String tags =
                    oseo.getKeywords()
                            .stream()
                            .map(k -> k.getValue())
                            .collect(Collectors.joining(" "));
            tags = tags + " " + CEOS_SUPPORTED_VERSION;
            element("Tags", tags);
            element(
                    "Url",
                    NO_CONTENTS,
                    attributes(
                            "rel",
                            "self", //
                            "template",
                            buildSelfUrl(description), //
                            "type",
                            "application/opensearchdescription+xml"));
            String relValue = description.getParentId() == null ? "collection" : "results";
            element(
                    "Url",
                    () -> describeParameters(description),
                    attributes( //
                            "rel",
                            relValue, //
                            "template",
                            buildResultsUrl(description, "atom"), //
                            "type",
                            "application/atom+xml",
                            "indexOffset",
                            "1"));
            element("LongName", oseo.getTitle());
            element("Developer", oseo.getMaintainer());
            element("SyndicationRight", "open"); // make configurable?
            element("AdultContent", "false");
            element("Language", "en-us");
            element("OutputEncoding", "UTF-8");
            element("InputEncoding", "UTF-8");
        }

        private String buildSelfUrl(OSEODescription description) {
            String baseURL = description.getBaseURL();
            Map<String, String> params = buildParentIdParams(description);
            return buildURL(baseURL, "oseo/description", params, URLType.SERVICE);
        }

        private Map<String, String> buildParentIdParams(OSEODescription description) {
            Map<String, String> params;
            if (description.getParentId() == null) {
                params = Collections.emptyMap();
            } else {
                params = params("parentId", description.getParentId());
            }
            return params;
        }

        public String buildResultsUrl(OSEODescription description, String format) {
            String baseURL = description.getBaseURL();
            Map<String, String> params = buildParentIdParams(description);
            String base = buildURL(baseURL, "oseo/search", params, URLType.SERVICE);
            // the template must not be url encoded instead
            String paramSpec =
                    description
                            .getSearchParameters()
                            .stream()
                            .map(
                                    p -> {
                                        String spec = p.key + "={";
                                        spec +=
                                                OpenSearchParameters.getQualifiedParamName(
                                                        oseo, p, false);
                                        if (!p.required) {
                                            spec += "?";
                                        }
                                        spec += "}";
                                        return spec;
                                    })
                            .collect(Collectors.joining("&"));

            return appendQueryString(
                    base, paramSpec + "&httpAccept=" + ResponseUtils.urlEncode(format));
        }

        public String buildSearchTermsDocLink(OSEODescription description) {
            String baseURL = description.getBaseURL();
            String url = buildURL(baseURL, "docs/searchTerms.html", null, URLType.RESOURCE);
            return url;
        }

        private void describeParameters(OSEODescription description) {
            for (Parameter param : description.getSearchParameters()) {
                Runnable contentsEncoder = null;

                // TODO: make this generic by adding lambdas into the parameter metadata?
                // difficulty is passing the methods to build elements (we could make them
                // visible or pass a lexical handler
                if ("searchTerms".equals(param.getName())) {
                    String searchTermsDocLink = buildSearchTermsDocLink(description);
                    contentsEncoder =
                            () -> {
                                element(
                                        "atom:link",
                                        (Runnable) null,
                                        attributes( //
                                                "rel", "profile", //
                                                "href", searchTermsDocLink, //
                                                "title",
                                                        "Simple search term parameter specification"));
                            };
                } else if ("geometry".equals(param.getName())) {
                    contentsEncoder =
                            () -> {
                                for (String type :
                                        new String[] {
                                            "LINESTRING",
                                            "POINT",
                                            "POLYGON",
                                            "MULTILINESTRING",
                                            "MULTIPOINT",
                                            "MULTIPOLYGON"
                                        }) {
                                    element(
                                            "atom:link",
                                            (Runnable) null,
                                            attributes(
                                                    "rel", "profile", //
                                                    "href",
                                                            "http://www.opengis.net/wkt/"
                                                                    + type, //
                                                    "title", "This service accepts WKT " + type));
                                }
                            };
                }

                final Map<String, String> map = new LinkedHashMap<>();
                map.put("name", param.key);
                map.put(
                        "value",
                        "{" + OpenSearchParameters.getQualifiedParamName(oseo, param, false) + "}");
                if (!param.isRequired()) {
                    map.put("minimum", "0");
                }
                if (param.metadata != null) {
                    String[] keys =
                            new String[] {
                                OpenSearchParameters.MIN_INCLUSIVE,
                                OpenSearchParameters.MAX_INCLUSIVE
                            };
                    for (String key : keys) {
                        Object value = param.metadata.get(key);
                        if (value != null) {
                            map.put(key, String.valueOf(value));
                        }
                    }
                }
                if (!map.containsKey("pattern")) {
                    Class type = param.getType();
                    if (Integer.class == type) {
                        map.put("pattern", "[+-][0-9]+");
                    } else if (Float.class == type || Double.class == type) {
                        map.put("pattern", "[-+]?[0-9]*\\.?[0-9]+");
                    } else if (Date.class.isAssignableFrom(type)) {
                        map.put(
                                "pattern",
                                "^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(\\.[0-9]+)?(Z|[\\+\\-][0-9]{2}:[0-9]{2})$");
                    }
                }
                element("param:Parameter", contentsEncoder, attributes(map));
            }
        }
    }
}
