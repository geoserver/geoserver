/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.impl.discovery;

import static org.geoserver.gss.xml.GSSSchema.GSS_Capabilities;

import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.gss.config.GSSInfo;
import org.geoserver.gss.impl.GSS;
import org.geoserver.gss.impl.discovery.FilterCapabilitiesTransformer.FilterCapabilitiesTranslator;
import org.geoserver.gss.internal.atom.Atom;
import org.geoserver.gss.internal.atompub.APP;
import org.geoserver.gss.internal.opensearch.OS;
import org.geoserver.gss.service.GetCapabilities;
import org.geoserver.gss.xml.GSSSchema;
import org.geotools.filter.v1_1.OGC;
import org.geotools.gml3.GML;
import org.geotools.ows.v1_1.OWS;
import org.geotools.xml.transform.Translator;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;

public class GetCapabilitiesTransformer extends AbstractTransformer {

    private static final String XML_SCHEMA_INSTANCE = "http://www.w3.org/2001/XMLSchema-instance";

    private static final String XLINK_NS = "http://www.w3.org/1999/xlink";

    private static final String OWS_PREFIX = "ows";

    private static final String FES_PREFIX = "fes";

    private static final String GML_PREFIX = "gml";

    private final GSS gss;

    public GetCapabilitiesTransformer(GSS gss) {
        this.gss = gss;
    }

    @Override
    public Translator createTranslator(final ContentHandler handler) {
        return new GSSCapabilitiesTranslator(handler, gss);
    }

    private static class GSSCapabilitiesTranslator extends AbstractTranslator {

        private final GSS gss;

        public GSSCapabilitiesTranslator(ContentHandler handler, GSS gss) {
            super(handler, null, GSSSchema.NAMESPACE);
            this.gss = gss;
            NamespaceSupport namespaceSupport = getNamespaceSupport();
            namespaceSupport.declarePrefix(GSSSchema.DEFAULT_PREFIX, GSSSchema.NAMESPACE);
            namespaceSupport.declarePrefix(OWS_PREFIX, OWS.NAMESPACE);
            namespaceSupport.declarePrefix(FES_PREFIX, OGC.NAMESPACE);
            namespaceSupport.declarePrefix(OS.DEFAULT_PREFIX, OS.NAMESPACE);
            namespaceSupport.declarePrefix(APP.DEFAULT_PREFIX, APP.NAMESPACE);
            namespaceSupport.declarePrefix(GML_PREFIX, GML.NAMESPACE);
            namespaceSupport.declarePrefix(Atom.DEFAULT_PREFIX, Atom.NAMESPACE);
        }

        /**
         * @param o
         *            a {@link GetCapabilities} request
         * @see org.geotools.xml.transform.Translator#encode(java.lang.Object)
         */
        public void encode(Object o) throws IllegalArgumentException {

            final GetCapabilities request = (GetCapabilities) o;

            String schemaLocation = schemaLocation(request, GSSSchema.NAMESPACE,
                    "gss/1.0.0/gss.xsd", XLINK_NS, "/xlink/1.0.0/xlinks.xsd", OWS.NAMESPACE,
                    "ows/1.1.0/owsAll.xsd", OGC.NAMESPACE, "filter/1.1.0/filter.xsd");

            AttributesImpl rootAtts = attributes("version", "1.0.0", "xmlns", GSSSchema.NAMESPACE,
                    "xmlns:xlink", XLINK_NS, "xmlns:xsi", XML_SCHEMA_INSTANCE,
                    "xsi:schemaLocation", schemaLocation);

            start(GSS_Capabilities, rootAtts);
            encodeServiceIndentification();
            encodeServiceProvider();

            final String baseURL = request.getBaseUrl();
            encodeOperationsMetadata(baseURL);
            encodeAtomPublishingProtocol(baseURL);
            encodeOpenSearchDescription(baseURL);

            encodeConformanceDeclaration();
            encodeFilterCapabilities();
            end(GSS_Capabilities);
        }

        private void encodeServiceIndentification() {
            ServiceIdentificationTransformer tr;
            tr = new ServiceIdentificationTransformer(getNamespaceSupport());
            tr.setOmitXMLDeclaration(true);
            Translator translator = tr.createTranslator(contentHandler);
            translator.encode(gss.getGssInfo());
        }

        private void encodeServiceProvider() {
            GSSInfo serviceInfo = gss.getGssInfo();
            GeoServer geoServer = serviceInfo.getGeoServer();
            ContactInfo contact = geoServer.getGlobal().getContact();

            ServiceProviderTransformer tr;
            tr = new ServiceProviderTransformer(getNamespaceSupport());
            tr.setOmitXMLDeclaration(true);
            Translator translator = tr.createTranslator(contentHandler);
            translator.encode(contact);
        }

        private void encodeOperationsMetadata(final String baseURL) {
            OperationsMetadataTransformer tr;
            tr = new OperationsMetadataTransformer(getNamespaceSupport());
            tr.setGetEntriesOutputFormats(gss.getGetEntriesOutputFormats());
            tr.setOmitXMLDeclaration(true);
            Translator translator = tr.createTranslator(contentHandler);
            translator.encode(baseURL);
        }

        private void encodeAtomPublishingProtocol(final String baseURL) {
            APPServiceSectionTransformer tr;
            tr = new APPServiceSectionTransformer(getNamespaceSupport(), baseURL);
            tr.setOmitXMLDeclaration(true);
            Translator translator = tr.createTranslator(contentHandler);
            translator.encode(gss.getGssInfo());
        }

        private void encodeOpenSearchDescription(String baseURL) {
            OpenSearchServiceSectionTransformer tr;
            tr = new OpenSearchServiceSectionTransformer(getNamespaceSupport(), baseURL);
            tr.setOmitXMLDeclaration(true);
            Translator translator = tr.createTranslator(contentHandler);
            translator.encode(gss.getGssInfo());
        }

        private void encodeConformanceDeclaration() {
            ConformanceDeclarationTransformer tr;
            tr = new ConformanceDeclarationTransformer(getNamespaceSupport());
            tr.setOmitXMLDeclaration(true);
            Translator translator = tr.createTranslator(contentHandler);
            translator.encode(gss.getGssInfo());
        }

        private void encodeFilterCapabilities() {
            FilterCapabilitiesTransformer tr;
            tr = new FilterCapabilitiesTransformer(getNamespaceSupport());
            tr.setOmitXMLDeclaration(true);
            FilterCapabilitiesTranslator translator = tr.createTranslator(contentHandler);
            translator.encode();
        }

    }
}
