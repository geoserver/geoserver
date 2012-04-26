/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.impl.discovery;

import static org.geoserver.gss.internal.atompub.APP.collection;
import static org.geoserver.gss.internal.atompub.APP.service;
import static org.geoserver.gss.internal.atompub.APP.workspace;

import org.geoserver.gss.config.GSSInfo;
import org.geoserver.gss.internal.atom.Atom;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.xml.transform.Translator;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Transformer for the {@code app:service} section.
 * 
 * @author Gabriel Roldan
 * 
 */
class APPServiceSectionTransformer extends AbstractTransformer {

    private final NamespaceSupport namespaceSupport;

    private final String baseURL;

    public APPServiceSectionTransformer(final NamespaceSupport namespaceSupport,
            final String baseURL) {
        this.namespaceSupport = namespaceSupport;
        this.baseURL = baseURL;
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new APPServiceSectionTranslator(handler, namespaceSupport);
    }

    private class APPServiceSectionTranslator extends AbstractTranslator {

        public APPServiceSectionTranslator(ContentHandler handler, NamespaceSupport namespaceSupport) {
            super(handler, null, null);
            super.nsSupport = namespaceSupport;
        }

        /**
         * TODO: revisit spec section 9.3.1.3 and check app service document contains all needed
         * elements
         * 
         * @param o
         *            a {@link GSSInfo}
         * @see org.geotools.xml.transform.Translator#encode(java.lang.Object)
         */
        public void encode(Object o) throws IllegalArgumentException {
            final GSSInfo serviceInfo = (GSSInfo) o;
            start(service);
            start(workspace);

            element(Atom.title, null, serviceInfo.getTitle());
            String changeFeed = ResponseUtils.buildURL(baseURL, "/gss/feed/CHANGEFEED", null,
                    URLType.SERVICE);
            collection(changeFeed, "GSS Change Feed");

            String replicationFeed = ResponseUtils.buildURL(baseURL, "/gss/feed/REPLICATIONFEED",
                    null, URLType.SERVICE);
            collection(replicationFeed, "GSS Replication Feed");

            String resolutionFeed = ResponseUtils.buildURL(baseURL, "/gss/feed/RESOLUTIONFEED",
                    null, URLType.SERVICE);
            collection(resolutionFeed, "GSS Resolution Feed");

            end(workspace);
            end(service);
        }

        private void collection(final String feedURL, final String title) {
            AttributesImpl url = attributes("href", feedURL);
            start(collection, url);
            element(Atom.title, null, title);
            end(collection);
        }
    }
}
