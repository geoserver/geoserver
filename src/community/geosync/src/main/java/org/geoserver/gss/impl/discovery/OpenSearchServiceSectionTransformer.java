/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.impl.discovery;

import static org.geoserver.gss.internal.opensearch.OS.AdultContent;
import static org.geoserver.gss.internal.opensearch.OS.Attribution;
import static org.geoserver.gss.internal.opensearch.OS.Contact;
import static org.geoserver.gss.internal.opensearch.OS.Description;
import static org.geoserver.gss.internal.opensearch.OS.Developer;
import static org.geoserver.gss.internal.opensearch.OS.Image;
import static org.geoserver.gss.internal.opensearch.OS.InputEncoding;
import static org.geoserver.gss.internal.opensearch.OS.Language;
import static org.geoserver.gss.internal.opensearch.OS.LongName;
import static org.geoserver.gss.internal.opensearch.OS.OpenSearchDescription;
import static org.geoserver.gss.internal.opensearch.OS.OutputEncoding;
import static org.geoserver.gss.internal.opensearch.OS.Query;
import static org.geoserver.gss.internal.opensearch.OS.ShortName;
import static org.geoserver.gss.internal.opensearch.OS.SyndicationRight;
import static org.geoserver.gss.internal.opensearch.OS.Tags;
import static org.geoserver.gss.internal.opensearch.OS.Url;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.geoserver.catalog.KeywordInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.gss.config.GSSInfo;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.xml.transform.Translator;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Transformer for the {@code os:OpenSearchDescription} section.
 * 
 * @author Gabriel Roldan
 * 
 */
class OpenSearchServiceSectionTransformer extends AbstractTransformer {

    private NamespaceSupport namespaceSupport;

    private String baseURL;

    public OpenSearchServiceSectionTransformer(final NamespaceSupport namespaceSupport,
            final String baseURL) {
        this.namespaceSupport = namespaceSupport;
        this.baseURL = baseURL;
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new OpenSearchServiceSectionTranslator(handler, namespaceSupport);
    }

    private class OpenSearchServiceSectionTranslator extends AbstractTranslator {

        public OpenSearchServiceSectionTranslator(ContentHandler handler,
                NamespaceSupport namespaceSupport) {
            super(handler, null, null);
            super.nsSupport = namespaceSupport;
        }

        /**
         * @param o
         *            a {@link GSSInfo}
         * @see org.geotools.xml.transform.Translator#encode(java.lang.Object)
         */
        public void encode(Object o) throws IllegalArgumentException {
            final GSSInfo serviceInfo = (GSSInfo) o;
            final GeoServerInfo geoServerInfo = serviceInfo.getGeoServer().getGlobal();
            start(OpenSearchDescription);

            element(ShortName, null, serviceInfo.getTitle());
            element(Description, null, serviceInfo.getAbstract());
            element(Tags, null, tags(serviceInfo));
            element(Contact, null, geoServerInfo.getContact().getContactEmail());

            url("application/atom+xml", "collection", template(baseURL, "CHANGEFEED"));
            url("application/atom+xml", "collection", template(baseURL, "REPLICATIONFEED"));
            url("application/atom+xml", "collection", template(baseURL, "RESOLUTIONFEED"));

            element(LongName, null, serviceInfo.getTitle());

            Attributes imgAtts = attributes("height", "64", "width", "64", "type", "image/png");
            String imgURL = ResponseUtils.buildURL(baseURL, "www/gss/websearch.png", null,
                    URLType.RESOURCE);
            element(Image, imgAtts, imgURL);

            element(Query, attributes("role", "example", "count", "25"), null);
            element(Developer, null, geoServerInfo.getContact().getContactPerson());
            element(Attribution, null, geoServerInfo.getContact().getContactOrganization());
            element(SyndicationRight, null, "open");
            element(AdultContent, null, "false");
            element(Language, null, "en");
            element(OutputEncoding, null, "UTF-8");
            element(InputEncoding, null, "UTF-8");
            end(OpenSearchDescription);
        }

        private void url(final String type, final String rel, final String template) {
            element(Url, attributes("type", type, "rel", rel, "template", template), null);
        }

        private String template(final String baseURL, final String feedName) {
            Map<String, String> kvp = new HashMap<String, String>();
            kvp.put("service", "GSS");
            kvp.put("version", "1.0.0");
            kvp.put("request", "GetEntries");
            kvp.put("feed", feedName);
            kvp.put("outputFormat", "application/atom+xml");
            kvp.put("startPosition", "{startIndex?}");
            kvp.put("maxEntries", "{count?}");
            kvp.put("searchTerms", "{searchTerms?}");
            kvp.put("bbox", "{geo:box?}");
            kvp.put("starttime", "{time:start?}");
            kvp.put("temporalOp", "After");

            String openSearchTemplateURL;
            openSearchTemplateURL = ResponseUtils.buildURL(baseURL, "/ows", kvp, URLType.SERVICE);
            return openSearchTemplateURL;
        }

        private String tags(final GSSInfo serviceInfo) {
            List<KeywordInfo> keywords = serviceInfo.getKeywords();
            if (keywords == null || keywords.size() == 0) {
                return "GeoServer GeoSynchronization feed collaboration GSS search";
            }
            StringBuilder sb = new StringBuilder();
            for (Iterator<KeywordInfo> i = keywords.iterator(); i.hasNext();) {
                KeywordInfo kw = i.next();
                if (kw != null && kw.getValue() != null) {
                    sb.append(kw.getValue());
                    if (i.hasNext()) {
                        sb.append(' ');
                    }
                }
            }
            return sb.toString();
        }
    }
}
