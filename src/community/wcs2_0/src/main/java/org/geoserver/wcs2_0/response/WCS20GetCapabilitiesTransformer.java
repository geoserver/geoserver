/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

import static org.apache.commons.lang.StringUtils.*;
import static org.geoserver.ows.util.ResponseUtils.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.opengis.wcs20.GetCapabilitiesType;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ResourceErrorHandling;
import org.geoserver.config.SettingsInfo;
import org.geoserver.ows.URLMangler;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs.responses.CoverageResponseDelegate;
import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.geoserver.wcs2_0.WCS20Const;
import org.geoserver.wcs2_0.util.NCNameResourceCodec;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.geotools.wcs.v2_0.WCS;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.vfny.geoserver.global.CoverageInfoLabelComparator;
import org.vfny.geoserver.wcs.WcsException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 *
 * @author Emanuele Tajariol (etj) - GeoSolutions
 */
public class WCS20GetCapabilitiesTransformer extends TransformerBase {

    private static final Logger LOGGER = Logging.getLogger(WCS20GetCapabilitiesTransformer.class.getPackage()
            .getName());

    protected static final String CUR_VERSION = WCS20Const.V20x;

    private WCSInfo wcs;

    private Catalog catalog;

    private final boolean skipMisconfigured;

    private CoverageResponseDelegateFinder responseFactory;

    enum SECTIONS {ServiceIdentification, ServiceProvider, OperationsMetadata, ServiceMetadata, Contents, Languages, All;

        public static final Set<String> names;
        static  {
            Set<String> tmp = new HashSet<String>();
            for (SECTIONS section : SECTIONS.values()) {
                tmp.add(section.name());
            }
            names = Collections.unmodifiableSet(tmp);
        }

    };


    public WCS20GetCapabilitiesTransformer(GeoServer gs, CoverageResponseDelegateFinder responseFactory) {
        this.wcs = gs.getService(WCSInfo.class);
        this.catalog = gs.getCatalog();
        this.skipMisconfigured = ResourceErrorHandling.SKIP_MISCONFIGURED_LAYERS.equals(
                gs.getGlobal().getResourceErrorHandling());
        this.responseFactory = responseFactory;
        setNamespaceDeclarationEnabled(false);
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new WCS20GetCapabilitiesTranslator(handler);
    }


    private class WCS20GetCapabilitiesTranslator extends TranslatorSupport {
        /**
         * DOCUMENT ME!
         *
         * @uml.property name="request"
         * @uml.associationEnd multiplicity="(0 1)"
         */
        private GetCapabilitiesType request;

        /**
         * Creates a new WFSCapsTranslator object.
         *
         * @param handler
         *            DOCUMENT ME!
         */
        public WCS20GetCapabilitiesTranslator(ContentHandler handler) {
            super(handler, null, null);
        }

        /**
         * Encode the object.
         *
         * @param o
         *            The Object to encode.
         *
         * @throws IllegalArgumentException
         *             if the Object is not encodeable.
         */
        public void encode(Object o) throws IllegalArgumentException {
            if (!(o instanceof GetCapabilitiesType)) {
                throw new IllegalArgumentException(new StringBuffer("Not a GetCapabilitiesType: ")
                        .append(o).toString());
            }

            this.request = (GetCapabilitiesType) o;

            // check the update sequence
            final long updateSequence = wcs.getGeoServer().getGlobal().getUpdateSequence();
            long requestedUpdateSequence = -1;
            if (request.getUpdateSequence() != null) {
                try {
                    requestedUpdateSequence = Long.parseLong(request.getUpdateSequence());
                } catch (NumberFormatException e) {
                    throw new WcsException("Invalid update sequence number format, "
                            + "should be an integer", WcsException.WcsExceptionCode.InvalidUpdateSequence,
                            "updateSequence");
                }
                if (requestedUpdateSequence > updateSequence) {
                    throw new WcsException("Invalid update sequence value, it's higher "
                            + "than the current value, " + updateSequence,
                            WcsException.WcsExceptionCode.InvalidUpdateSequence, "updateSequence");
                }
            }

            // check and init sections
                        // handle the sections directive
            boolean allSections;
            List<String> sections;
            if (request.getSections() == null) {
                sections = Collections.emptyList();
                allSections = true;
            } else {
                sections = request.getSections().getSection(); 
                allSections = sections.contains(SECTIONS.All.name());

                for (String section : sections) {
                    if(! SECTIONS.names.contains(section))
                        throw new WcsException("Unknown section " + section,
                                WcsException.WcsExceptionCode.InvalidParameterValue, "Sections");
                }
            }

            // Build the document

            final AttributesImpl attributes = WCS20Const.getDefaultNamespaces();
            attributes.addAttribute("", "version", "version", "", CUR_VERSION);
            attributes.addAttribute("", "xmlns:wcscrs", "xmlns:wcscrs", "", "http://www.opengis.net/wcs/service-extension/crs/1.0");
            attributes.addAttribute("", "xmlns:int", "xmlns:int", "", "http://www.opengis.net/WCS_service-extension_interpolation/1.0");
            attributes.addAttribute("", "updateSequence", "updateSequence", "", String.valueOf(updateSequence));
            
            // TODO: add a config to choose the canonical or local schema 
            final String locationDef = WCS.NAMESPACE + " http://schemas.opengis.net/wcs/2.0/wcsGetCapabilities.xsd";  
            // final String locationDef = WCS.NAMESPACE + " " + buildSchemaURL(request.getBaseUrl(), "wcs/2.0/wcsGetCapabilities.xsd");//
            attributes.addAttribute("", "xsi:schemaLocation", "xsi:schemaLocation", "", locationDef);

            start("wcs:Capabilities", attributes);

            // encode the actual capabilities contents taking into consideration
            // the sections
            if (requestedUpdateSequence < updateSequence) {
                if (allSections || sections.contains(SECTIONS.ServiceIdentification.name()))
                    handleServiceIdentification();
                if (allSections || sections.contains(SECTIONS.ServiceProvider.name()))
                    handleServiceProvider();
                if (allSections || sections.contains(SECTIONS.OperationsMetadata.name()))
                    handleOperationsMetadata();
                if (allSections || sections.contains(SECTIONS.ServiceMetadata.name()))
                    handleServiceMetadata(request);
                if (allSections || sections.contains(SECTIONS.Contents.name()))
                    handleContents();
                if (allSections || sections.contains(SECTIONS.Languages.name()))
                    handleLanguages(); 
            }

            end("wcs:Capabilities");
        }

        private void handleServiceMetadata(GetCapabilitiesType ct) {
            start("wcs:ServiceMetadata");
            
            // formats are part of the document only starting version 2.0.1
            if(ct.getAcceptVersions() == null || ct.getAcceptVersions().getVersion() == null 
                    || ct.getAcceptVersions().getVersion().isEmpty() || ct.getAcceptVersions().getVersion().contains("2.0.1")) {
                Set<String> formats = new TreeSet<String>();
                for (String format : responseFactory.getOutputFormats()) {
                    CoverageResponseDelegate delegate = responseFactory.encoderFor(format);
                    String mime = delegate.getMimeType(format);
                    try {
                        new URI(mime);
                        formats.add(mime);
                    } catch(URISyntaxException e) {
                        // skip it
                    }
                }
                for (String format : formats) {
                    element("wcs:formatSupported", format);
                }
            }
            
            // the CRS extension requires us to declare the full list of supported CRS
            start("wcs:Extension");
            // add the supported CRS
            for (String code : CRS.getSupportedCodes("EPSG")) {
                if(!code.equals("WGS84(DD)")) {
                     element("wcscrs:crsSupported", "http://www.opengis.net/def/crs/EPSG/0/" + code);
                }
            }
            // add the supported interpolation methods
            element("int:interpolationSupported", "http://www.opengis.net/def/interpolation/OGC/1/nearest-neighbor");
            element("int:interpolationSupported", "http://www.opengis.net/def/interpolation/OGC/1/linear");
            element("int:interpolationSupported", "http://www.opengis.net/def/interpolation/OGC/1/cubic");
            end("wcs:Extension");
            
            end("wcs:ServiceMetadata");
        }

        /**
         * Handles the service identification of the capabilities document.
         *
         * @param config
         *            The OGC service to transform.
         *
         * @throws SAXException
         *             For any errors.
         */
        private void handleServiceIdentification() {
            start("ows:ServiceIdentification");

            element("ows:Title", wcs.getTitle());
            element("ows:Abstract", wcs.getAbstract());
            
            handleKeywords(wcs.getKeywords());

            element("ows:ServiceType", "urn:ogc:service:wcs"); // TODO: check this: some docs specify a "OGC WCS" string
            element("ows:ServiceTypeVersion", WCS20Const.V20x);
            element("ows:ServiceTypeVersion", WCS20Const.V111);
            element("ows:ServiceTypeVersion", WCS20Const.V110);

            element("ows:Profile", "http://www.opengis.net/spec/WCS/2.0/conf/core");
            element("ows:Profile", "http://www.opengis.net/spec/WCS_protocol-binding_get-kvp/1.0"); // requirement #1 in OGC 09-147r1
            // element("ows:Profile", "http://www.opengis.net/spec/WCS_protocol-binding_get-kvp/1.0/conf/get-kvp"); // sample getCapa in OGC 09-110r4
            element("ows:Profile", "http://www.opengis.net/spec/WCS_protocol-binding_post-xml/1.0");

            element("ows:Profile","http://www.opengis.net/spec/WCS_service-extension_scaling/1.0/conf/scaling");
            element("ows:Profile","http://www.opengis.net/spec/WCS_service-extension_range-subsetting/1.0/conf/record-subsetting");
            // don't believe we support these
            // element("ows:Profile","http://www.opengis.net/spec/WCS_service-extension_array-subsetting/1.0/conf/array-subsetting");
            // element("ows:Profile","http://www.opengis.net/spec/WCS_service-extension_range-subsetting/1.0/conf/nested-subsetting");
            element("ows:Profile", "http://www.opengis.net/spec/WCS_service-extension_crs/1.0/conf/crs");
            // don't believe we support this one
            // element("ows:Profile", "http://www.opengis.net/spec/WCS_service-extension_crs/1.0/conf/crs-discrete-coverage");
            element("ows:Profile", "http://www.opengis.net/spec/WCS_service-extension_crs/1.0/conf/crs-gridded-coverage");
            
            // element("ows:Profile","http://www.opengis.net/spec/WCS_coverage-encoding/1.0/conf/coverage-encoding"); // TODO: check specs and URL
            element("ows:Profile","http://www.opengis.net/spec/GMLCOV_geotiff-coverages/1.0/conf/geotiff-coverage");
            element("ows:Profile","http://www.opengis.net/spec/WCS_coverage-encoding_geotiff/1.0/"); // TODO: check specs and URL
            element("ows:Profile","http://www.opengis.net/spec/WCS_coverage-encoding_gml/1.0/"); // TODO: check specs and URL
            
            
            element("ows:Profile", "http://www.opengis.net/spec/WCS_service-extension_interpolation/1.0/conf/interpolation");
            element("ows:Profile","http://www.opengis.net/spec/WCS_service-extension_interpolation/1.0/conf/nearest-neighbor");
            element("ows:Profile","http://www.opengis.net/spec/WCS_service-extension_interpolation/1.0/conf/linear");
            element("ows:Profile","http://www.opengis.net/spec/WCS_service-extension_interpolation/1.0/conf/cubic");

            String fees = wcs.getFees();
            if ( isBlank(fees)) {
                fees = "NONE";
            }
            element("ows:Fees", fees);

            String accessConstraints = wcs.getAccessConstraints();
            if ( isBlank(accessConstraints)) {
                accessConstraints = "NONE";
            }
            element("ows:AccessConstraints", accessConstraints);
            end("ows:ServiceIdentification");
        }

        /**
         * Handles the service provider of the capabilities document.
         *
         * @param config
         *            The OGC service to transform.
         *
         * @throws SAXException
         *             For any errors.
         */
        private void handleServiceProvider() {
            start("ows:ServiceProvider");
            SettingsInfo settings = wcs.getGeoServer().getSettings();
            element("ows:ProviderName", settings.getContact().getContactOrganization());
            AttributesImpl attributes = new AttributesImpl();
            attributes.addAttribute("", "xlink:href", "xlink:href", "",
                settings.getOnlineResource() != null ? settings.getOnlineResource() : "");
            element("ows:ProviderSite", null, attributes);

            handleContact();

            end("ows:ServiceProvider");
        }

        /**
         * Handles the OperationMetadata portion of the document, printing out
         * the operations and where to bind to them.
         *
         * @param config
         *            The global wms.
         *
         * @throws SAXException
         *             For any problems.
         */
        private void handleOperationsMetadata() {
            start("ows:OperationsMetadata");
            handleOperation("GetCapabilities", null);
            handleOperation("DescribeCoverage", null);
            handleOperation("GetCoverage", null);
//            new HashMap<String, List<String>>() {
//                {
//                    put("store", Arrays.asList("True", "False"));
//                }
//            });

            // specify that we do support xml post encoding, clause 8.3.2.2 of
            // the WCS 1.1.1 spec
            AttributesImpl attributes = new AttributesImpl();
            attributes.addAttribute(null, "name", "name", null, "PostEncoding");
            start("ows:Constraint", attributes);
            start("ows:AllowedValues");
            element("ows:Value", "XML");
//            element("ows:Value", "text/xml");
//            element("ows:Value", "application/xml");
            end("ows:AllowedValues");
            end("ows:Constraint");

            end("ows:OperationsMetadata");
        }

        private void handleOperation(String capabilityName, Map<String, List<String>> parameters) {
            AttributesImpl attributes = new AttributesImpl();
            attributes.addAttribute(null, "name", "name", null, capabilityName);
            start("ows:Operation", attributes);

            final String url = appendQueryString(buildURL(request.getBaseUrl(), "wcs", null, URLMangler.URLType.SERVICE), "");

            start("ows:DCP");
            start("ows:HTTP");
            attributes = new AttributesImpl();
            attributes.addAttribute("", "xlink:href", "xlink:href", "", url);
            element("ows:Get", null, attributes);
            end("ows:HTTP");
            end("ows:DCP");

            attributes = new AttributesImpl();
            attributes.addAttribute("", "xlink:href", "xlink:href", "", url);
            start("ows:DCP");
            start("ows:HTTP");
            element("ows:Post", null, attributes);
            end("ows:HTTP");
            end("ows:DCP");

            if (parameters != null && !parameters.isEmpty()) {
                for (Map.Entry<String, List<String>> param : parameters.entrySet()) {
                    attributes = new AttributesImpl();
                    attributes.addAttribute("", "name", "name", "", param.getKey());
                    start("ows:Parameter", attributes);
                    start("ows:AllowedValues");
                    for (String value : param.getValue()) {
                        element("ows:Value", value);
                    }
                    end("ows:AllowedValues");
                    end("ows:Parameter");
                }
            }

            end("ows:Operation");
        }

        /**
         * DOCUMENT ME!
         *
         * @param kwords
         *            DOCUMENT ME!
         *
         * @throws SAXException
         *             DOCUMENT ME!
         */
        private void handleKeywords(List<KeywordInfo> kwords) {
            if( kwords != null && ! kwords.isEmpty()) {
                start("ows:Keywords");
                for (KeywordInfo kword : kwords) {
                    element("ows:Keyword", kword.getValue());
                }
                end("ows:Keywords");
            }
        }

        /**
         * Handles contacts.
         *
         * @param wcs
         *            the service.
         */
        private void handleContact() {
            final GeoServer gs = wcs.getGeoServer();
            start("ows:ServiceContact");

            ContactInfo contact = gs.getSettings().getContact();
            elementIfNotEmpty("ows:IndividualName", contact.getContactPerson());
            elementIfNotEmpty("ows:PositionName", contact.getContactPosition());

            start("ows:ContactInfo");
            start("ows:Phone");
            elementIfNotEmpty("ows:Voice", contact.getContactVoice());
            elementIfNotEmpty("ows:Facsimile", contact.getContactFacsimile());
            end("ows:Phone");
            start("ows:Address");
            elementIfNotEmpty("ows:DeliveryPoint", contact.getAddress());
            elementIfNotEmpty("ows:City", contact.getAddressCity());
            elementIfNotEmpty("ows:AdministrativeArea", contact.getAddressState());
            elementIfNotEmpty("ows:PostalCode", contact.getAddressPostalCode());
            elementIfNotEmpty("ows:Country", contact.getAddressCountry());
            elementIfNotEmpty("ows:ElectronicMailAddress", contact.getContactEmail());
            end("ows:Address");

            String or = gs.getSettings().getOnlineResource();
            if ( isNotBlank(or)) {
                AttributesImpl attributes = new AttributesImpl();
                attributes.addAttribute("", "xlink:href", "xlink:href", "", or);
                start("ows:OnlineResource", attributes);
                end("OnlineResource");
            }

            end("ows:ContactInfo");
            end("ows:ServiceContact");
        }

        private void handleEnvelope(ReferencedEnvelope envelope) {
            start("ows:WGS84BoundingBox");
            element("ows:LowerCorner", new StringBuffer(Double.toString(envelope.getLowerCorner()
                    .getOrdinate(0))).append(" ").append(envelope.getLowerCorner().getOrdinate(1))
                    .toString());
            element("ows:UpperCorner", new StringBuffer(Double.toString(envelope.getUpperCorner()
                    .getOrdinate(0))).append(" ").append(envelope.getUpperCorner().getOrdinate(1))
                    .toString());
            end("ows:WGS84BoundingBox");
        }

        private void handleContents() {
            start("wcs:Contents");

            List<CoverageInfo> coverages =
                    new ArrayList<CoverageInfo>(wcs.getGeoServer().getCatalog().getCoverages());

            // filter out disabled coverages
            for (Iterator<CoverageInfo> it = coverages.iterator(); it.hasNext();) {
                CoverageInfo cv = (CoverageInfo) it.next();
                if (!cv.enabled()) {
                    it.remove();
                }
            }

            // filter out coverages that are not in the requested namespace
// namespaces not in 2.0
//            if(request.getNamespace() != null) {
//                String namespace = request.getNamespace();
//                for (Iterator it = coverages.iterator(); it.hasNext();) {
//                    CoverageInfo cv = (CoverageInfo) it.next();
//                    if(!namespace.equals(cv.getStore().getWorkspace().getName()))
//                        it.remove();
//                }
//            }

            Collections.sort(coverages, new CoverageInfoLabelComparator());
            for (CoverageInfo cv : coverages) {
                try {
                    mark();
                    handleCoverageSummary(cv);
                    commit();
                } catch (Exception e) {
                    if (skipMisconfigured) {
                        reset();
                        LOGGER.log(Level.SEVERE, "Skipping coverage " + cv.prefixedName()
                                + " as its capabilities generation failed", e);
                    } else {
                        throw new RuntimeException("Capabilities document generation failed on coverage "
                                + cv.prefixedName(), e);
                    }
                }
            }

            end("wcs:Contents");
        }

        private void handleCoverageSummary(CoverageInfo cv) {
            start("wcs:CoverageSummary");
            String covId = NCNameResourceCodec.encode(cv);
            element("wcs:CoverageId", covId);
            element("wcs:CoverageSubtype", "GridCoverage");

            handleEnvelope(cv.getLatLonBoundingBox());

            end("wcs:CoverageSummary");
        }

        /**
         * Converts each metadata URL to XML.
         *
         * @param links
         *              a collection of links
         * @param linkType
         *              the type of links
         */
        private void handleMetadataLinks(List<MetadataLinkInfo> links, String linkType) {
            for (MetadataLinkInfo  mdl : links) {
                if (mdl != null) {
                    handleMetadataLink(mdl, linkType);
                }
            }
        }

        private void handleMetadataLink(MetadataLinkInfo mdl, String linkType) {
            AttributesImpl attributes = new AttributesImpl();

            if ( isNotBlank(mdl.getAbout())) {
                attributes.addAttribute("", "about", "about", "", mdl.getAbout());
            }

            if ( isNotBlank(mdl.getMetadataType()) ) {
                attributes.addAttribute("", "metadataType", "metadataType", "", mdl
                        .getMetadataType());
            }

            if ( isNotBlank(linkType) ) {
                attributes.addAttribute("", "xlink:type", "xlink:type", "", linkType);
            }

            if ( isNotBlank(mdl.getContent()) ) {
                attributes.addAttribute("", "xlink:href", "xlink:href",
                        "", mdl.getContent());
                element("ows:Metadata", null, attributes);
            }
        }

        private void handleLanguages() {
//            start("ows:Languages");
//            // TODO
//            end("ows:Languages");
        }

        /**
         * Writes the element if and only if the content is not null and not
         * empty
         *
         * @param elementName
         * @param content
         */
        private void elementIfNotEmpty(String elementName, String content) {
            if ( isNotBlank(content) )
                element(elementName, content);
        }
    }

}
